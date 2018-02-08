package org.xbib.cql;

/**
 * An Identifier is a SimpleName or a String in double quotes.
 */
public class Identifier extends AbstractNode {

    private final String value;

    private final boolean quoted;

    public Identifier(String value) {
        this.value = value;
        this.quoted = true;
    }

    public Identifier(SimpleName name) {
        this.value = name.getName();
        this.quoted = false;
    }

    public String getValue() {
        return value;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return quoted ? "\"" + value.replaceAll("\"", "\\\\\"") + "\"" : value;
    }
}
