package org.xbib.cql;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class QueryTest extends Assert {

    @Test
    public void testValidQueries() throws IOException {
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
                    errors++;
                }
                count++;
            }
        }
        lr.close();
        assertEquals(errors, 0);
        assertEquals(ok, count);
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
