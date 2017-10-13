package org.xbib.cql;

/**
 * Query facet.
 * @param <V> the facet value type parameter
 */
public interface QueryFacet<V> extends QueryOption<V> {
    /**
     * The size of the facet.
     *
     * @return the facet size
     */
    int getSize();

    /**
     * Get the filter name which must be used for filtering facet entries.
     *
     * @return the filter name
     */
    String getFilterName();

}
