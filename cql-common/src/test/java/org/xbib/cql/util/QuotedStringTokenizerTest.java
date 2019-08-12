package org.xbib.cql.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class QuotedStringTokenizerTest {

    @Test
    void testTokenizer() throws Exception {
        String s = "Linux is \"pinguin's best friend\", not Windows";
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(s);
        assertEquals("Linux", tokenizer.nextToken());
        assertEquals("is", tokenizer.nextToken());
        assertEquals("pinguin's best friend,", tokenizer.nextToken());
        assertEquals("not", tokenizer.nextToken());
        assertEquals("Windows", tokenizer.nextToken());
    }
}
