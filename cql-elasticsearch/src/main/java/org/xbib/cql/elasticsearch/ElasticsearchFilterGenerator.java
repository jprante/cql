package org.xbib.cql.elasticsearch;

import org.xbib.cql.BooleanGroup;
import org.xbib.cql.BooleanOperator;
import org.xbib.cql.Comparitor;
import org.xbib.cql.Identifier;
import org.xbib.cql.Index;
import org.xbib.cql.ModifierList;
import org.xbib.cql.PrefixAssignment;
import org.xbib.cql.Query;
import org.xbib.cql.Relation;
import org.xbib.cql.ScopedClause;
import org.xbib.cql.SearchClause;
import org.xbib.cql.SimpleName;
import org.xbib.cql.SingleSpec;
import org.xbib.cql.SortSpec;
import org.xbib.cql.SortedQuery;
import org.xbib.cql.SyntaxException;
import org.xbib.cql.Term;
import org.xbib.cql.Visitor;
import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Node;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;
import org.xbib.cql.elasticsearch.ast.TokenType;
import org.xbib.cql.elasticsearch.model.ElasticsearchQueryModel;
import org.xbib.datastructures.json.tiny.JsonBuilder;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Stack;

/**
 * Generate Elasticsearch filter query from CQL abstract syntax tree.
 */
public class ElasticsearchFilterGenerator implements Visitor {

    private final ElasticsearchQueryModel model;

    private final Stack<Node> stack;

    private final String globalField;

    private final FilterGenerator filterGen;

    public ElasticsearchFilterGenerator(String globalField) {
        this(globalField, new ElasticsearchQueryModel());
    }

    public ElasticsearchFilterGenerator(String globalField, ElasticsearchQueryModel model) {
        this.globalField = globalField;
        this.model = model;
        this.stack = new Stack<>();
        this.filterGen = new FilterGenerator();
    }

    public void addOrFilter(String filterKey, Collection<String> filterValues) {
        for (String value : filterValues) {
            model.addDisjunctiveFilter(filterKey, new Expression(Operator.OR_FILTER, new Name(filterKey),
                    new Token(value)), Operator.OR);
        }
    }

    public void addAndFilter(String filterKey, Collection<String> filterValues) {
        for (String value : filterValues) {
            model.addConjunctiveFilter(filterKey, new Expression(Operator.AND_FILTER, new Name(filterKey),
                    new Token(value)), Operator.AND);
        }
    }

    public JsonBuilder getResult() {
        return filterGen.getResult();
    }

    @Override
    public void visit(SortedQuery node) {
        try {
            filterGen.start();
            node.getQuery().accept(this);
            Node querynode = stack.pop();
            if (querynode instanceof Token) {
                filterGen.visit(new Expression(Operator.TERM_FILTER, new Name(globalField), querynode));
            } else if (querynode instanceof Expression) {
                filterGen.visit(new Expression(Operator.QUERY_FILTER, (Expression) querynode));
            }
            if (model.hasFilter()) {
                filterGen.visit(model.getFilterExpression());
            }
            filterGen.end();
        } catch (IOException e) {
            throw new SyntaxException("unable to build a valid query from " + node + ", reason: " + e.getMessage(), e);
        }
    }

    @Override
    public void visit(SortSpec node) {
        if (node.getSingleSpec() != null) {
            node.getSingleSpec().accept(this);
        }
        if (node.getSortSpec() != null) {
            node.getSortSpec().accept(this);
        }
    }

    @Override
    public void visit(SingleSpec node) {
        if (node.getIndex() != null) {
            node.getIndex().accept(this);
        }
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
        if (!stack.isEmpty()) {
            model.setSort(stack);
        }
    }

    @Override
    public void visit(Query node) {
        for (PrefixAssignment assignment : node.getPrefixAssignments()) {
            assignment.accept(this);
        }
        if (node.getScopedClause() != null) {
            node.getScopedClause().accept(this);
        }
    }

    @Override
    public void visit(PrefixAssignment node) {
        node.getPrefix().accept(this);
        node.getURI().accept(this);
    }

    @Override
    public void visit(ScopedClause node) {
        if (node.getScopedClause() != null) {
            node.getScopedClause().accept(this);
        }
        node.getSearchClause().accept(this);
        if (node.getBooleanGroup() != null) {
            node.getBooleanGroup().accept(this);
        }
        // evaluate expression
        if (!stack.isEmpty() && stack.peek() instanceof Operator) {
            Operator op = (Operator) stack.pop();
            if (!stack.isEmpty()) {
                Node esnode = stack.pop();
                // add default context if node is a literal without a context
                if (esnode instanceof Token && TokenType.STRING.equals(esnode.getType())) {
                    esnode = new Expression(Operator.ALL, new Name(globalField), esnode);
                }
                if (stack.isEmpty()) {
                    // unary expression
                    throw new IllegalArgumentException("unary expression not allowed, op=" + op + " node=" + esnode);
                } else {
                    // binary expression
                    Node esnode2 = stack.pop();
                    // add default context if node is a literal without context
                    if (esnode2 instanceof Token && TokenType.STRING.equals(esnode2.getType())) {
                        esnode2 = new Expression(Operator.ALL, new Name(globalField), esnode2);
                    }
                    esnode = new Expression(op, esnode2, esnode);
                }
                stack.push(esnode);
            }
        }
    }

    @Override
    public void visit(SearchClause node) {
        if (node.getQuery() != null) {
            // CQL query in parenthesis
            node.getQuery().accept(this);
        }
        if (node.getTerm() != null) {
            node.getTerm().accept(this);
        }
        if (node.getIndex() != null) {
            node.getIndex().accept(this);
        }
        if (node.getRelation() != null) {
            node.getRelation().accept(this);
            if (node.getRelation().getModifierList() != null && node.getIndex() != null) {
                // stack layout: op, list of modifiers, modifiable index
                Node op = stack.pop();
                StringBuilder sb = new StringBuilder();
                Node modifier = stack.pop();
                while (modifier instanceof Modifier) {
                    if (sb.length() > 0) {
                        sb.append('.');
                    }
                    sb.append(modifier.toString());
                    modifier = stack.pop();
                }
                String modifiable = sb.toString();
                stack.push(new Name(modifiable));
                stack.push(op);
            }
        }
        // evaluate expression
        if (!stack.isEmpty() && stack.peek() instanceof Operator) {
            Operator op = (Operator) stack.pop();
            Node arg1 = stack.pop();
            Node arg2 = stack.pop();
            // fold two expressions if they have the same operator
            boolean fold = arg1.isVisible() && arg2.isVisible()
                    && arg2 instanceof Expression
                    && ((Expression) arg2).getOperator().equals(op);
            Expression expression = fold ? new Expression((Expression) arg2, arg1) : new Expression(op, arg1, arg2);
            stack.push(expression);
        }
    }

    @Override
    public void visit(BooleanGroup node) {
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
        stack.push(booleanToES(node.getOperator()));
    }

    @Override
    public void visit(Relation node) {
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
        stack.push(comparitorToES(node.getComparitor()));
    }

    @Override
    public void visit(ModifierList node) {
        for (org.xbib.cql.Modifier modifier : node.getModifierList()) {
            modifier.accept(this);
        }
    }

    @Override
    public void visit(org.xbib.cql.Modifier node) {
        Node term = null;
        if (node.getTerm() != null) {
            node.getTerm().accept(this);
            term = stack.pop();
        }
        node.getName().accept(this);
        Node name = stack.pop();
        stack.push(new Modifier(name, term));
    }

    @Override
    public void visit(Term node) {
        stack.push(termToES(node));
    }

    @Override
    public void visit(Identifier node) {
        stack.push(new Name(node.getValue()));
    }

    @Override
    public void visit(Index node) {
        String context = node.getContext();
        String name = context != null ? context + "." + node.getName() : node.getName();
        Name esname = new Name(name, model.getVisibility(context));
        esname.setType(model.getElasticsearchType(name));
        stack.push(esname);
    }

    @Override
    public void visit(SimpleName node) {
        stack.push(new Name(node.getName()));
    }

    private Node termToES(Term node) {
        if (node.isLong()) {
            return new Token(Long.parseLong(node.getValue()));
        } else if (node.isFloat()) {
            return new Token(Double.parseDouble(node.getValue()));
        } else if (node.isIdentifier()) {
            return new Token(node.getValue());
        } else if (node.isDate()) {
            return new Token(ZonedDateTime.from(DateTimeFormatter.ISO_INSTANT.parse(node.getValue())));
        } else if (node.isString()) {
            return new Token(node.getValue());
        }
        return null;
    }

    private Operator booleanToES(BooleanOperator bop) {
        Operator op;
        switch (bop) {
            case AND:
                op = Operator.AND;
                break;
            case OR:
                op = Operator.OR;
                break;
            case NOT:
                op = Operator.ANDNOT;
                break;
            case PROX:
                op = Operator.PROX;
                break;
            default:
                throw new IllegalArgumentException("unknown CQL operator: " + bop);
        }
        return op;
    }

    private Operator comparitorToES(Comparitor op) {
        Operator esop;
        switch (op) {
            case EQUALS:
                esop = Operator.EQUALS;
                break;
            case GREATER:
                esop = Operator.RANGE_GREATER_THAN;
                break;
            case GREATER_EQUALS:
                esop = Operator.RANGE_GREATER_OR_EQUAL;
                break;
            case LESS:
                esop = Operator.RANGE_LESS_THAN;
                break;
            case LESS_EQUALS:
                esop = Operator.RANGE_LESS_OR_EQUALS;
                break;
            case NOT_EQUALS:
                esop = Operator.NOT_EQUALS;
                break;
            case WITHIN:
                esop = Operator.RANGE_WITHIN;
                break;
            case ADJ:
                esop = Operator.PHRASE;
                break;
            case ALL:
                esop = Operator.ALL;
                break;
            case ANY:
                esop = Operator.ANY;
                break;
            default:
                throw new IllegalArgumentException("unknown CQL comparitor: " + op);
        }
        return esop;
    }
}
