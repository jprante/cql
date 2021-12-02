package org.xbib.cql.elasticsearch;

import org.xbib.cql.SyntaxException;
import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Node;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;
import org.xbib.datastructures.json.tiny.JsonBuilder;

import java.io.IOException;
import java.util.Collections;

/**
 * Build Elasticsearch query from abstract syntax tree.
 */
public class QueryGenerator implements Visitor {

    private final JsonBuilder builder;

    public QueryGenerator() {
        this.builder = JsonBuilder.builder();
    }

    public void start() throws IOException {
        builder.beginMap();
    }

    public void end() throws IOException {
        builder.endMap();
    }

    public void startFiltered() throws IOException {
        builder.beginMap("filtered").beginMap("query");
    }

    public void endFiltered() throws IOException {
        builder.endMap();
    }

    public void startBoost(String boostField, String modifier, Float factor, String boostMode) throws IOException {
        builder.beginMap("function_score")
                .beginMap("field_value_factor")
                .field("field", boostField)
                .field("modifier", modifier != null ? modifier : "log1p")
                .field("factor", factor != null ? factor : 1.0f)
                .endMap()
                .field("boost_mode", boostMode != null ? boostMode : "multiply")
                .beginMap("query");
    }

    public void endBoost() throws IOException {
        builder.endMap().endMap();
    }

    public JsonBuilder getResult() {
        return builder;
    }

    @Override
    public void visit(Token token) {
        try {
            switch (token.getType()) {
                case BOOL:
                    builder.buildValue(token.getBoolean());
                    break;
                case INT:
                    builder.buildValue(token.getInteger());
                    break;
                case FLOAT:
                    builder.buildValue(token.getFloat());
                    break;
                case DATETIME:
                    builder.buildValue(token.getDate());
                    break;
                case STRING:
                    builder.buildValue(token.getString());
                    break;
                default:
                    throw new IOException("unknown token type: " + token);
            }
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Name node) {
        try {
            builder.buildKey(node.toString());
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Modifier node) {
        try {
            builder.buildValue(node.toString());
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Operator node) {
        try {
            builder.buildValue(node.toString());
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Expression node) {
        if (!node.isVisible()) {
            return;
        }
        try {
            Operator op = node.getOperator();
            switch (op.getArity()) {
                case 0: {
                    if (op == Operator.MATCH_ALL) {
                        builder.beginMap("match_all").endMap();
                    }
                    break;
                }
                case 1: {
                    // unary operators, anyone?
                    break;
                }
                case 2: {
                    // binary operators
                    Node arg1 = node.getArg1();
                    Node arg2 = node.getArgs().length > 1 ? node.getArg2() : null;
                    Token tok2 = arg2 instanceof Token ? (Token) arg2 : null;
                    boolean visible = false;
                    for (Node arg : node.getArgs()) {
                        visible = visible || arg.isVisible();
                    }
                    if (!visible) {
                        return;
                    }
                    String field = arg1.toString();
                    switch (op) {
                        case EQUALS: {
                            String value = arg2 != null ? arg2.toString() : ""; // with quote
                            if (field.endsWith("Keyword")) {
                                // exact search
                                builder.beginMap()
                                        .beginMap("term")
                                        .field(field, value)
                                        .endMap()
                                        .endMap();
                            } else {
                                // with phrase boost
                                builder.beginMap("bool")
                                        .beginCollection("should")
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", value)
                                        .field("fields", Collections.singletonList(field))
                                        .field("analyze_wildcard", true)
                                        .field("default_operator", "and")
                                        .endMap()
                                        .endMap()
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", "\"" + value + "\"")
                                        .field("fields", Collections.singletonList(field + "^2"))
                                        .field("default_operator", "and")
                                        .endMap()
                                        .endMap()
                                        .endCollection()
                                        .field("minimum_should_match", "1")
                                        .endMap();
                            }
                            break;
                        }
                        case NOT_EQUALS: {
                            String value = arg2 != null ? arg2.toString() : ""; // with quote
                            if (field.endsWith("Keyword")) {
                                // exact search
                                builder.beginMap("bool")
                                        .beginMap("must_not")
                                        .beginMap("term")
                                        .field(field, value)
                                        .endMap()
                                        .endMap()
                                        .endMap();
                            } else {
                                builder.beginMap("bool")
                                        .beginMap("must_not")
                                        .beginMap("simple_query_string")
                                        .field("query", value)
                                        .field("fields", Collections.singletonList(field))
                                        .field("analyze_wildcard", true)
                                        .field("default_operator", "and")
                                        .endMap()
                                        .endMap()
                                        .endMap();
                            }
                            break;
                        }
                        case ALL: {
                            String value = tok2 != null ? tok2.getString() : ""; // always unquoted
                            if (field.endsWith("Keyword")) {
                                // exact search
                                builder.beginMap("term")
                                        .field(field, value)
                                        .endMap();
                            } else {
                                // with phrase boost
                                builder.beginMap("bool")
                                        .beginCollection("should")
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", value)
                                        .field("fields", Collections.singletonList(field))
                                        .field("analyze_wildcard", true)
                                        .field("default_operator", "and")
                                        .endMap()
                                        .endMap()
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", "\"" + value + "\"")
                                        .field("fields", Collections.singletonList(field + "^2"))
                                        .field("default_operator", "and")
                                        .endMap()
                                        .endMap()
                                        .endCollection()
                                        .field("minimum_should_match", "1")
                                        .endMap();
                            }
                            break;
                        }
                        case ANY: {
                            String value = tok2 != null ? tok2.getString() : ""; // always unquoted
                            if (field.endsWith("Keyword")) {
                                // exact search
                                builder.beginMap("term")
                                        .field(field, value)
                                        .endMap();
                            } else {
                                // with phrase boost
                                builder.beginMap("bool")
                                        .beginCollection("should")
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", value)
                                        .field("fields", Collections.singletonList(field))
                                        .field("analyze_wildcard", true)
                                        .endMap()
                                        .endMap()
                                        .beginMap()
                                        .beginMap("simple_query_string")
                                        .field("query", "\"" + value + "\"")
                                        .field("fields", Collections.singletonList(field + "^2"))
                                        .endMap()
                                        .endMap()
                                        .endCollection()
                                        .field("minimum_should_match", "1")
                                        .endMap();
                            }
                            break;
                        }
                        case PHRASE: {
                            if (tok2 != null) {
                                String value = tok2.isQuoted() ? tok2.getString() : arg2.toString();
                                if (tok2.isAll()) {
                                    builder.beginMap("match_all").endMap();
                                } else if (tok2.isWildcard()) {
                                    builder.beginMap("wildcard").field(field, value).endMap();
                                } else if (tok2.isBoundary()) {
                                    builder.beginMap("prefix").field(field, value).endMap();
                                } else {
                                    if (field.endsWith("Keyword")) {
                                        // exact search
                                        builder.beginMap("term")
                                                .field(field, value)
                                                .endMap();
                                    } else {
                                        builder.beginMap("simple_query_string")
                                                .field("query", value)
                                                .field("fields", Collections.singletonList(field))
                                                .field("analyze_wildcard", true)
                                                .field("default_operator", "and")
                                                .endMap();
                                    }
                                }
                            }
                            break;
                        }
                        case RANGE_GREATER_THAN: {
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("from", value)
                                    .field("include_lower", false)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_GREATER_OR_EQUAL: {
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("from", value)
                                    .field("include_lower", true)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_LESS_THAN: {
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("to", value)
                                    .field("include_upper", false)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_LESS_OR_EQUALS: {
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("to", value)
                                    .field("include_upper", true)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_WITHIN: {
                            // borders are inclusive
                            String value = arg2 != null ? arg2.toString() : "";
                            String from = null;
                            String to = null;
                            if (tok2 != null) {
                                if (!tok2.isQuoted()) {
                                    throw new IllegalArgumentException("range within: unable to derive range "
                                            + "from a non-phrase: " + value);
                                }
                                if (tok2.getStringList().size() != 2) {
                                    throw new IllegalArgumentException("range within: unable to derive range "
                                            + "from a phrase of length not equals to 2: " + tok2.getStringList());
                                }
                                from = tok2.getStringList().get(0);
                                to = tok2.getStringList().get(1);
                            }
                            builder.beginMap("range").beginMap(field)
                                    .field("from", from)
                                    .field("to", to)
                                    .field("include_lower", true)
                                    .field("include_upper", true)
                                    .endMap().endMap();
                            break;
                        }
                        case AND: {
                            if (arg2 == null) {
                                if (arg1.isVisible()) {
                                    arg1.accept(this);
                                }
                            } else {
                                builder.beginMap("bool");
                                if (arg1.isVisible() && arg2.isVisible()) {
                                    builder.beginCollection("must").beginMap();
                                    arg1.accept(this);
                                    builder.endMap().beginMap();
                                    arg2.accept(this);
                                    builder.endMap().endCollection();
                                } else if (arg1.isVisible()) {
                                    builder.beginMap("must");
                                    arg1.accept(this);
                                    builder.endMap();
                                } else if (arg2.isVisible()) {
                                    builder.beginMap("must");
                                    arg2.accept(this);
                                    builder.endMap();
                                }
                                builder.endMap();
                            }
                            break;
                        }
                        case OR: {
                            // short expression
                            if (arg2 == null) {
                                if (arg1.isVisible()) {
                                    arg1.accept(this);
                                }
                            } else {
                                builder.beginMap("bool");
                                if (arg1.isVisible() && arg2.isVisible()) {
                                    builder.beginCollection("should").beginMap();
                                    arg1.accept(this);
                                    builder.endMap().beginMap();
                                    arg2.accept(this);
                                    builder.endMap().endCollection();
                                } else if (arg1.isVisible()) {
                                    builder.beginMap("should");
                                    arg1.accept(this);
                                    builder.endMap();
                                } else if (arg2.isVisible()) {
                                    builder.beginMap("should");
                                    arg2.accept(this);
                                    builder.endMap();
                                }
                                builder.endMap();
                            }
                            break;
                        }
                        case ANDNOT: {
                            if (arg2 == null) {
                                if (arg1.isVisible()) {
                                    arg1.accept(this);
                                }
                            } else {
                                builder.beginMap("bool");
                                if (arg1.isVisible() && arg2.isVisible()) {
                                    builder.beginCollection("must_not").beginMap();
                                    arg1.accept(this);
                                    builder.endMap().beginMap();
                                    arg2.accept(this);
                                    builder.endMap().endCollection();
                                } else if (arg1.isVisible()) {
                                    builder.beginMap("must_not");
                                    arg1.accept(this);
                                    builder.endMap();
                                } else if (arg2.isVisible()) {
                                    builder.beginMap("must_not");
                                    arg2.accept(this);
                                    builder.endMap();
                                }
                                builder.endMap();
                            }
                            break;
                        }
                        case PROX: {
                            // we assume a default of 10 words is enough for proximity
                            String value = arg2 != null ? arg2 + "~10" : "";
                            builder.beginMap("field").field(field, value).endMap();
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("unable to translate operator while "
                                    + "building elasticsearch query: " + op);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new SyntaxException("internal error while building elasticsearch query", e);
        }
    }

}
