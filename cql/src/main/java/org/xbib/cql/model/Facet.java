package org.xbib.cql.model;

import org.xbib.cql.QueryFacet;

/**
 * Facet.
 *
 * @param <V> parameter type
 */
public final class Facet<V> implements QueryFacet<V>, Comparable<Facet<V>> {

    private int size;
    private String filterName;
    private String name;
    private V value;

    public Facet(String name) {
        this.name = name;
    }

    public Facet(String name, String filterName, int size) {
        this.name = name;
        this.filterName = filterName;
        this.size = size;
    }

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

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    public String toCQL() {
        return CQLQueryModel.FACET_INDEX_NAME + "." + name + " = " + value;
    }

    @Override
    public int compareTo(Facet<V> o) {
        return getName().compareTo((o).getName());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Facet && getName().equals(((Facet) o).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return toCQL();
    }
}
