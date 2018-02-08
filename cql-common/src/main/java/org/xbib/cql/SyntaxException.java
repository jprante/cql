package org.xbib.cql;

/**
 * CQL Syntax exception.
 */
public class SyntaxException extends RuntimeException {

    private static final long serialVersionUID = -9028694755857782309L;

    /**
     * Creates a new SyntaxException object.
     *
     * @param msg the message for this syntax exception
     */
    public SyntaxException(String msg) {
        super(msg);
    }

    /**
     * Creates a new SyntaxException object.
     *
     * @param msg the message for this syntax exception
     * @param t   the throwable for this syntax exception
     */
    public SyntaxException(String msg, Throwable t) {
        super(msg, t);
    }
}
