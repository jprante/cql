package org.xbib.cql.elasticsearch;

import static org.xbib.content.json.JsonXContent.contentBuilder;

import org.xbib.content.XContentBuilder;

import java.io.IOException;

/**
 *
 */
public class SourceGenerator {

    private final XContentBuilder builder;

    public SourceGenerator() throws IOException {
        this.builder = contentBuilder();
    }

    public void build(QueryGenerator query,
                      int from, int size) throws IOException {
        build(query, from, size, null, null);
    }

    public void build(QueryGenerator query, int from, int size, XContentBuilder sort, XContentBuilder facets)
            throws IOException {
        builder.startObject();
        builder.field("from", from);
        builder.field("size", size);
        builder.rawField("query", query.getResult().bytes().toBytes());
        if (sort != null && sort.bytes().length() > 0) {
            builder.rawField("sort", sort.bytes().toBytes());
        }
        if (facets != null && facets.bytes().length() > 0) {
            builder.rawField("aggregations", facets.bytes().toBytes());
        }
        builder.endObject();
        builder.close();
    }

    public XContentBuilder getResult() {
        return builder;
    }
}
