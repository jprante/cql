module org.xbib.cql.elasticsearch {
    exports org.xbib.cql.elasticsearch;
    exports org.xbib.cql.elasticsearch.ast;
    exports org.xbib.cql.elasticsearch.model;
    requires transitive org.xbib.cql;
    requires org.xbib.content.core;
    requires org.xbib.content.json;
}
