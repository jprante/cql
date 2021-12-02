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
import java.util.Stack;

/**
 * Build sort in Elasticsearch JSON syntax from abstract syntax tree.
 */
public class SortGenerator implements Visitor {

    private final JsonBuilder builder;

    private final Stack<Modifier> modifiers;

    public SortGenerator() {
        this.builder = JsonBuilder.builder();
        this.modifiers = new Stack<>();
    }

    public void start() throws IOException {
        builder.beginCollection();
    }

    public void end() throws IOException {
        builder.endCollection();
    }

    public JsonBuilder getResult() {
        return builder;
    }

    @Override
    public void visit(Token node) {
    }

    @Override
    public void visit(Name node) {
        try {
            if (modifiers.isEmpty()) {
                builder.beginMap()
                        .buildKey(node.getName())
                        .beginMap()
                        .field("ignore_unmapped", "true")
                        .field("missing", "_last")
                        .endMap()
                        .endMap();
            } else {
                builder.beginMap().buildKey(node.getName()).beginMap();
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
                            builder.field(s, mod.getTerm().toString());
                            break;
                        }
                    }
                }
                builder.field("ignore_unmapped", "true");
                builder.field("missing", "_last");
                builder.endMap();
                builder.endMap();
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
