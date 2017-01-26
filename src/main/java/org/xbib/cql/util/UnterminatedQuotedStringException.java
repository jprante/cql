package org.xbib.cql.util;

/**
 * Exception for string tokenizing.
 */
public class UnterminatedQuotedStringException extends RuntimeException {

    private static final long serialVersionUID = 3114942659171051019L;

    public UnterminatedQuotedStringException(String msg) {
        super(msg);
    }
}
