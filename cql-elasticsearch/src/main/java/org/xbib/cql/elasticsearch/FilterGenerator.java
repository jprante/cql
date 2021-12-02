package org.xbib.cql.elasticsearch;

import org.xbib.cql.SyntaxException;
import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Node;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;
import org.xbib.cql.util.QuotedStringTokenizer;
import org.xbib.datastructures.json.tiny.JsonBuilder;

import java.io.IOException;

/**
 * Build query filter in Elasticsearch JSON syntax from abstract syntax tree.
 */
public class FilterGenerator implements Visitor {

    private final JsonBuilder builder;

    public FilterGenerator() {
        this.builder = JsonBuilder.builder();
    }

    public FilterGenerator(QueryGenerator queryGenerator) throws IOException {
        this.builder = queryGenerator.getResult();
    }

    public FilterGenerator start() throws IOException {
        builder.beginMap();
        return this;
    }

    public FilterGenerator end() throws IOException {
        builder.endMap();
        return this;
    }

    public FilterGenerator startFilter() throws IOException {
        builder.beginMap("filter");
        return this;
    }

    public FilterGenerator endFilter() throws IOException {
        builder.endMap();
        return this;
    }

    public JsonBuilder getResult() {
        return builder;
    }

    @Override
    public void visit(Token node) {
        try {
            builder.buildValue(node.getString());
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
                case 2: {
                    Node arg1 = node.getArg1();
                    Node arg2 = node.getArgs().length > 1 ? node.getArg2() : null;
                    boolean visible = false;
                    for (Node arg : node.getArgs()) {
                        visible = visible || arg.isVisible();
                    }
                    if (!visible) {
                        return;
                    }
                    Token tok2 = arg2 instanceof Token ? (Token) arg2 : null;
                    switch (op) {
                        case EQUALS: {
                            String field = arg1.toString();
                            String value = tok2 != null ? tok2.getString() : "";
                            builder.beginMap(tok2 != null && tok2.isBoundary() ? "prefix" : "term");
                            builder.field(field, value)
                                    .endMap();
                            break;
                        }
                        case NOT_EQUALS: {
                            String field = arg1.toString();
                            String value = tok2 != null ? tok2.getString() : "";
                            builder.beginMap("not")
                                    .beginMap(tok2 != null && tok2.isBoundary() ? "prefix" : "term")
                                    .field(field, value)
                                    .endMap().endMap();
                            break;
                        }
                        case ALL: {
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            boolean phrase = arg2 instanceof Token && ((Token) arg2).isQuoted();
                            if (phrase) {
                                builder.beginCollection("and");
                                QuotedStringTokenizer qst = new QuotedStringTokenizer(value);
                                while (qst.hasMoreTokens()) {
                                    builder.beginMap().beginMap("term")
                                            .field(field, qst.nextToken())
                                            .endMap().endMap();
                                }
                                builder.endCollection();
                            } else {
                                builder.beginMap(tok2 != null && tok2.isBoundary() ? "prefix" : "term")
                                        .field(field, value)
                                        .endMap();
                            }
                            break;
                        }
                        case ANY: {
                            boolean phrase = arg2 instanceof Token && ((Token) arg2).isQuoted();
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            if (phrase) {
                                builder.beginCollection("or");
                                QuotedStringTokenizer qst = new QuotedStringTokenizer(value);
                                while (qst.hasMoreTokens()) {
                                    builder.beginMap().beginMap("term")
                                            .field(field, qst.nextToken()).endMap().endMap();
                                }
                                builder.endCollection();
                            } else {
                                builder.beginMap(tok2 != null && tok2.isBoundary() ? "prefix" : "term")
                                        .field(field, value)
                                        .endMap();
                            }
                            break;
                        }
                        case RANGE_GREATER_THAN: {
                            String field = arg1.toString();
                            String value = tok2 != null ? tok2.getString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("from", value)
                                    .field("include_lower", false)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_GREATER_OR_EQUAL: {
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("from", value)
                                    .field("include_lower", true)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_LESS_THAN: {
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("to", value)
                                    .field("include_upper", false)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_LESS_OR_EQUALS: {
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("range").beginMap(field)
                                    .field("to", value)
                                    .field("include_upper", true)
                                    .endMap().endMap();
                            break;
                        }
                        case RANGE_WITHIN: {
                            String field = arg1.toString();
                            String value = tok2 != null ? tok2.getString() : "";
                            String[] s = value.split(" ");
                            builder.beginMap("range").beginMap(field).
                                    field("from", s[0])
                                    .field("to", s[1])
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
                                builder.beginCollection("must");
                                Node[] args = node.getArgs();
                                for (int i = 0; i < node.getArgs().length; i++) {
                                    if (args[i].isVisible()) {
                                        builder.beginMap();
                                        args[i].accept(this);
                                        builder.endMap();
                                    }
                                }
                                builder.endCollection();
                                builder.endMap();
                            }
                            break;
                        }
                        case OR: {
                            if (arg2 == null) {
                                if (arg1.isVisible()) {
                                    arg1.accept(this);
                                }
                            } else {
                                builder.beginMap("bool");
                                builder.beginCollection("should");
                                Node[] args = node.getArgs();
                                for (int i = 0; i < node.getArgs().length; i++) {
                                    if (args[i].isVisible()) {
                                        builder.beginMap();
                                        args[i].accept(this);
                                        builder.endMap();
                                    }
                                }
                                builder.endCollection();
                                builder.endMap();
                            }
                            break;
                        }
                        case OR_FILTER: {
                            builder.beginMap("bool");
                            builder.beginCollection("should");
                            Node[] args = node.getArgs();
                            for (int i = 0; i < args.length; i += 2) {
                                if (args[i].isVisible()) {
                                    builder.beginMap().beginMap("term");
                                    args[i].accept(this);
                                    args[i + 1].accept(this);
                                    builder.endMap().endMap();
                                }
                            }
                            builder.endCollection();
                            builder.endMap();
                            break;
                        }
                        case AND_FILTER: {
                            builder.beginMap("bool");
                            builder.beginCollection("must");
                            Node[] args = node.getArgs();
                            for (int i = 0; i < args.length; i += 2) {
                                if (args[i].isVisible()) {
                                    builder.beginMap().beginMap("term");
                                    args[i].accept(this);
                                    args[i + 1].accept(this);
                                    builder.endMap().endMap();
                                }
                            }
                            builder.endCollection();
                            builder.endMap();
                            break;
                        }
                        case ANDNOT: {
                            if (arg2 == null) {
                                if (arg1.isVisible()) {
                                    arg1.accept(this);
                                }
                            } else {
                                builder.beginMap("bool");
                                builder.beginCollection("must_not");
                                Node[] args = node.getArgs();
                                for (int i = 0; i < node.getArgs().length; i++) {
                                    if (args[i].isVisible()) {
                                        builder.beginMap();
                                        args[i].accept(this);
                                        builder.endMap();
                                    }
                                }
                                builder.endCollection();
                                builder.endMap();
                            }
                            break;
                        }
                        case PROX: {
                            String field = arg1.toString();
                            // we assume a  default of 10 words is enough for proximity
                            String value = arg2 != null ? arg2.toString() + "~10" : "";
                            builder.beginMap("field").field(field, value).endMap();
                            break;
                        }
                        case TERM_FILTER: {
                            String field = arg1.toString();
                            String value = arg2 != null ? arg2.toString() : "";
                            builder.beginMap("term").field(field, value).endMap();
                            break;
                        }
                        case QUERY_FILTER: {
                            builder.beginMap("query");
                            arg1.accept(this);
                            builder.endMap();
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("unable to translate operator "
                                    + "while building elasticsearch query filter: " + op);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new SyntaxException("internal error while building elasticsearch query filter", e);
        }
    }
}
