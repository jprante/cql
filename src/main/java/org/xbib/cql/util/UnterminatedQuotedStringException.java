package org.xbib.cql.util;

/**
 * Exception for string tokenizing.
 */
public class UnterminatedQuotedStringException extends RuntimeException {

    public UnterminatedQuotedStringException(String msg) {
        super(msg);
    }
}
