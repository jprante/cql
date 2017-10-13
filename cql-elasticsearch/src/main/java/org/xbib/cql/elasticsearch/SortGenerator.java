package org.xbib.cql.elasticsearch;

import org.xbib.content.XContentBuilder;
import org.xbib.content.json.JsonXContent;
import org.xbib.cql.SyntaxException;
import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Node;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;

import java.io.IOException;
import java.util.Stack;

/**
 * Build sort in Elasticsearch JSON syntax from abstract syntax tree.
 */
public class SortGenerator implements Visitor {

    private final XContentBuilder builder;

    private final Stack<Modifier> modifiers;

    public SortGenerator() throws IOException {
        this.builder = JsonXContent.contentBuilder();
        this.modifiers = new Stack<>();
    }

    public void start() throws IOException {
        builder.startArray();
    }

    public void end() throws IOException {
        builder.endArray();
    }

    public XContentBuilder getResult() {
        return builder;
    }

    @Override
    public void visit(Token node) {
    }

    @Override
    public void visit(Name node) {
        try {
            if (modifiers.isEmpty()) {
                builder.startObject()
                        .field(node.getName())
                        .startObject()
                        .field("unmapped_type", "string")
                        .field("missing", "_last")
                        .endObject()
                        .endObject();
            } else {
                builder.startObject().field(node.getName()).startObject();
                while (!modifiers.isEmpty()) {
                    Modifier mod = modifiers.pop();
                    String s = mod.getName().toString();
                    switch (s) {
                        case "ascending":
                        case "sort.ascending": {
                            builder.field("order", "asc");
                            break;
                        }
                        case "descending":
                        case "sort.descending": {
                            builder.field("order", "desc");
                            break;
                        }
                        default: {
                            builder.field(s, mod.getTerm());
                            break;
                        }
                    }
                }
                builder.field("unmapped_type", "string");
                builder.field("missing", "_last");
                builder.endObject();
                builder.endObject();
            }
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Modifier node) {
        modifiers.push(node);
    }

    @Override
    public void visit(Operator node) {
    }

    @Override
    public void visit(Expression node) {
        Operator op = node.getOperator();
        if (op == Operator.SORT) {
            for (Node arg : node.getArgs()) {
                arg.accept(this);
            }
        }
    }

}
