package org.xbib.cql;

/**
 * Qery option.
 * @param <V> parameter type
 */
public interface QueryOption<V> {

    void setName(String name);

    String getName();

    void setValue(V value);

    V getValue();
}
