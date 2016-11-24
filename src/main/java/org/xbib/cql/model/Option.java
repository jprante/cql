package org.xbib.cql.model;

import org.xbib.cql.QueryOption;

/**
 * Option.
 * @param <V> parameter type
 */
public class Option<V> implements QueryOption<V>, Comparable<Option<V>> {

    private String name;
    private V value;

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public V getValue() {
        return value;
    }

    public String toCQL() {
        return CQLQueryModel.OPTION_INDEX_NAME + "." + name + " = " + value;
    }

    @Override
    public int compareTo(Option<V> o) {
        return name.compareTo((o).getName());
    }

    @Override
    public String toString() {
        return toCQL();
    }

}
