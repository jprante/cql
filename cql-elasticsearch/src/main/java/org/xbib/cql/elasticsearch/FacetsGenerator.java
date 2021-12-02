package org.xbib.cql.elasticsearch;

import org.xbib.cql.SyntaxException;
import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;
import org.xbib.datastructures.json.tiny.JsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Build facet from abstract syntax tree.
 */
public class FacetsGenerator implements Visitor {

    private int facetlength = 10;

    private final JsonBuilder builder;

    public FacetsGenerator() throws IOException {
        this.builder = JsonBuilder.builder();
    }

    public void start() throws IOException {
        builder.beginMap();
    }

    public void end() throws IOException {
        builder.endMap();
    }

    public void startFacets() throws IOException {
        builder.beginMap("aggregations");
    }

    public void endFacets() throws IOException {
        builder.endMap();
    }

    public JsonBuilder getResult() {
        return builder;
    }

    @Override
    public void visit(Token node) {
        try {
            builder.buildValue(node.toString());
        } catch (IOException e) {
            throw new SyntaxException(e.getMessage(), e);
        }
    }

    @Override
    public void visit(Name node) {
        try {
            builder.buildValue(node.toString());
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
        try {
            Operator op = node.getOperator();
            switch (op) {
                case TERMS_FACET: {
                    builder.beginMap()
                            .field("myfacet", "myvalue")
                            .endMap();
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "unable to translate operator while building elasticsearch facet: " + op);
            }
        } catch (IOException e) {
            throw new SyntaxException("internal error while building elasticsearch query", e);
        }
    }

    public FacetsGenerator facet(String facetLimit, String facetSort) throws IOException {
        if (facetLimit == null) {
            return this;
        }
        Map<String, Integer> facetMap = parseFacet(facetLimit);
        String[] sortSpec = facetSort != null ? facetSort.split(",") : new String[]{"recordCount", "descending"};
        String order = "_count";
        String dir = "desc";
        for (String s : sortSpec) {
            switch (s) {
                case "recordCount":
                    order = "_count";
                    break;
                case "alphanumeric":
                    order = "_term";
                    break;
                case "ascending":
                    dir = "asc";
                    break;
            }
        }
        builder.beginMap();
        for (String index : facetMap.keySet()) {
            if ("*".equals(index)) {
                continue;
            }
            // TODO(jprante) range aggregations etc.
            String facetType = "terms";
            Integer size = facetMap.get(index);
            builder.buildKey(index)
                    .beginMap()
                    .buildKey(facetType)
                    .beginMap()
                    .field("field", index)
                    .field("size", size > 0 ? size : 10)
                    .beginMap("order")
                    .field(order, dir)
                    .endMap()
                    .endMap();
            builder.endMap();
        }
        builder.endMap();
        return this;
    }

    private Map<String, Integer> parseFacet(String spec) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("*", facetlength);
        if (spec == null || spec.length() == 0) {
            return m;
        }
        String[] params = spec.split(",");
        for (String param : params) {
            int pos = param.indexOf(':');
            if (pos > 0) {
                int n = parseInt(param.substring(0, pos), facetlength);
                m.put(param.substring(pos + 1), n);
            } else if (param.length() > 0) {
                int n = parseInt(param, facetlength);
                m.put("*", n);
            }
        }
        return m;
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
