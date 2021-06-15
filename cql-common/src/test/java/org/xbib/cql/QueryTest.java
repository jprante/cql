package org.xbib.cql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class QueryTest {

    private static final Logger logger = Logger.getLogger(QueryTest.class.getName());

    @Test
    void testValidQueries() throws IOException {
        test("queries.txt");
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
                    } else {
                        validate(line);
                    }
                    ok++;
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage());
                    errors++;
                }
                count++;
            }
        }
        lr.close();
        assertEquals(0, errors);
        assertEquals(count, ok);
    }

    private void validate(String line) throws Exception {
        CQLParser parser = new CQLParser(line);
        parser.parse();
        assertEquals(line, parser.getCQLQuery().toString());
    }

    private void validate(String line, String expected) throws Exception {
        CQLParser parser = new CQLParser(line);
        parser.parse();
        assertEquals(expected, parser.getCQLQuery().toString());
    }
}
