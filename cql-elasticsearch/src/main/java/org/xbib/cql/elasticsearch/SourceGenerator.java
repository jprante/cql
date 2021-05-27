package org.xbib.cql.elasticsearch;

import org.xbib.content.XContentBuilder;
import org.xbib.content.core.DefaultXContentBuilder;
import org.xbib.content.json.JsonXContent;

import java.io.IOException;

public class SourceGenerator {

    private final XContentBuilder builder;

    public SourceGenerator() throws IOException {
        this.builder = JsonXContent.contentBuilder();
    }

    public void build(QueryGenerator query) throws IOException {
        build(query, null, null, null, null);
    }

    public void build(QueryGenerator query, Integer from, Integer size) throws IOException {
        build(query, from, size, null, null);
    }

    public void build(QueryGenerator query, Integer from, Integer size,
                      XContentBuilder sort, XContentBuilder facets) throws IOException {
        builder.startObject();
        if (query != null) {
            if (from != null) {
                builder.field("from", from);
            }
            if (size != null) {
                builder.field("size", size);
            }
            copy(builder, "query", query.getResult());
            copy(builder, "sort", sort);
            copy(builder, "aggregations", facets);
        }
        builder.endObject();
        builder.close();
    }

    public XContentBuilder getResult() {
        return builder;
    }

    private void copy(XContentBuilder builder, String rawFieldName, XContentBuilder anotherBuilder) throws IOException {
        if (anotherBuilder != null) {
            DefaultXContentBuilder contentBuilder = (DefaultXContentBuilder) anotherBuilder;
            if (contentBuilder.bytes().length() > 0) {
                byte[] b = contentBuilder.bytes().toBytes();
                builder.rawField(rawFieldName, b, 0, b.length);
            }
        }
    }
}
