package org.xbib.cql.elasticsearch;

import org.junit.jupiter.api.Test;
import org.xbib.cql.CQLParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class ElasticsearchQueryTest {

    @Test
    void testValidQueries() throws IOException {
        test("queries.txt");
    }

    @Test
    void testSimpleTermFilter() throws Exception {
        String cql = "Jörg";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchFilterGenerator generator = new ElasticsearchFilterGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String json = generator.getResult().string();
        assertEquals(json, "{\"term\":{\"cql.allIndexes\":\"Jörg\"}}");
    }

    @Test
    void testFieldTermFilter() throws Exception {
        String cql = "dc.type = electronic";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchFilterGenerator generator = new ElasticsearchFilterGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String json = generator.getResult().string();
        assertEquals(json, "{\"query\":{\"term\":{\"dc.type\":\"electronic\"}}}");
    }

    @Test
    void testDoubleFieldTermFilter() throws Exception {
        String cql = "dc.type = electronic and dc.date = 2013";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchFilterGenerator generator = new ElasticsearchFilterGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String json = generator.getResult().string();
        assertEquals(
                "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"dc.type\":\"electronic\"}},{\"term\":{\"dc.date\":\"2013\"}}]}}}",
                json
        );
    }

    @Test
    void testTripleFieldTermFilter() throws Exception {
        String cql = "dc.format = online and dc.type = electronic and dc.date = 2013";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchFilterGenerator generator = new ElasticsearchFilterGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String json = generator.getResult().string();
        assertEquals(
                "{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"term\":{\"dc.format\":\"online\"}}," +
                        "{\"term\":{\"dc.type\":\"electronic\"}}]}},{\"term\":{\"dc.date\":\"2013\"}}]}}}",
                json);
    }

    @Test
    void testBoost() throws Exception {
        String cql = "Jörg";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator("cql.allIndexes");
        generator.setBoostParams("boost", "log2p", 2.0f, "sum");
        parser.getCQLQuery().accept(generator);
        String json = generator.getSourceResult();
        assertEquals(
                "{\"from\":0,\"size\":10,\"query\":{\"function_score\":{\"field_value_factor\":{\"field\":\"boost\"," +
                        "\"modifier\":\"log2p\",\"factor\":2.0},\"boost_mode\":\"sum\"," +
                        "\"query\":{\"simple_query_string\":{\"query\":\"Jörg\",\"fields\":[\"cql.allIndexes\"]," +
                        "\"analyze_wildcard\":true,\"default_operator\":\"and\"}}}}}",
                json);
    }

    @Test
    void testWildcardTerm() throws Exception {
        String cql = "dc.format = book*";
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String json = generator.getSourceResult();
        assertEquals("{\"from\":0,\"size\":10,\"query\":{\"simple_query_string\":" +
                "{\"query\":\"book*\",\"fields\":[\"dc.format\"],\"analyze_wildcard\":true," +
                "\"default_operator\":\"and\"}}}",
                json);
    }

    private void test(String path) throws IOException {
        int count = 0;
        int ok = 0;
        int errors = 0;
        LineNumberReader lr = new LineNumberReader(new InputStreamReader(getClass().getResourceAsStream(path),
                StandardCharsets.UTF_8));
        String line;
        while ((line = lr.readLine()) != null) {
            if (line.trim().length() > 0 && !line.startsWith("#")) {
                try {
                    int pos = line.indexOf('|');
                    if (pos > 0) {
                        validate(line.substring(0, pos), line.substring(pos + 1));
                        ok++;
                    }
                } catch (Exception e) {
                    errors++;
                }
                count++;
            }
        }
        lr.close();
        assertEquals(0, errors);
        assertEquals(count, ok);
    }

    private void validate(String cql, String expected) throws Exception {
        CQLParser parser = new CQLParser(cql);
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator("cql.allIndexes");
        parser.getCQLQuery().accept(generator);
        String elasticsearchQuery = generator.getSourceResult();
        assertEquals(expected, elasticsearchQuery);
    }

}
