package org.xbib.cql.elasticsearch;

import org.xbib.datastructures.json.tiny.JsonBuilder;

import java.io.IOException;

public class SourceGenerator {

    private final JsonBuilder builder;

    public SourceGenerator() throws IOException {
        this.builder = JsonBuilder.builder();
    }

    public void build(QueryGenerator query) throws IOException {
        build(query, null, null, null, null);
    }

    public void build(QueryGenerator query, Integer from, Integer size) throws IOException {
        build(query, from, size, null, null);
    }

    public void build(QueryGenerator query, Integer from, Integer size,
                      JsonBuilder sort, JsonBuilder facets) throws IOException {
        builder.beginMap();
        if (query != null) {
            if (from != null) {
                builder.field("from", from);
            }
            if (size != null) {
                builder.field("size", size);
            }
            if (query.getResult() != null) {
                builder.buildKey("query");
                builder.copy(query.getResult());
            }
            if (sort != null && sort.build().length() > 0) {
                builder.buildKey("sort");
                builder.copy(sort);
            }
            if (facets != null && facets.build().length() > 0) {
                builder.buildKey("aggregations");
                builder.copy(facets);
            }
        }
        builder.endMap();
    }

    public JsonBuilder getResult() {
        return builder;
    }
}
