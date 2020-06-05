module org.xbib.cql.elasticsearch {
    exports org.xbib.cql.elasticsearch;
    exports org.xbib.cql.elasticsearch.ast;
    exports org.xbib.cql.elasticsearch.model;
    requires transitive org.xbib.cql;
    requires transitive org.xbib.content.core;
}