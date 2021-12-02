module org.xbib.cql.elasticsearch {
    exports org.xbib.cql.elasticsearch;
    exports org.xbib.cql.elasticsearch.ast;
    exports org.xbib.cql.elasticsearch.model;
    requires transitive org.xbib.cql;
    requires org.xbib.datastructures.api;
    requires transitive org.xbib.datastructures.json.tiny;
}
