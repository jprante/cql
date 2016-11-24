package org.xbib.cql.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class QuotedStringTokenizerTest {

    @Test
    public void testTokenizer() throws Exception {
        String s = "Linux is \"pinguin's best friend\", not Windows";
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(s);
        assertEquals("Linux", tokenizer.nextToken());
        assertEquals("is", tokenizer.nextToken());
        assertEquals("pinguin's best friend,", tokenizer.nextToken());
        assertEquals("not", tokenizer.nextToken());
        assertEquals("Windows", tokenizer.nextToken());
    }
}
