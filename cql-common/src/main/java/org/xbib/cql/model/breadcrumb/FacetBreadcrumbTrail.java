package org.xbib.cql.model.breadcrumb;

import org.xbib.cql.model.Facet;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Facet breadcrumb trail.
 * @param <V> the facet value parameter
 */
public class FacetBreadcrumbTrail<V> extends TreeSet<Facet<V>> {

    private static final long serialVersionUID = 6268000598739081048L;

    @Override
    public String toString() {
        return toCQL();
    }

    public String toCQL() {
        StringBuilder sb = new StringBuilder();
        if (isEmpty()) {
            return sb.toString();
        }
        Iterator<Facet<V>> it = iterator();
        if (it.hasNext()) {
            sb.append(it.next().toCQL());
        }
        while (it.hasNext()) {
            sb.append(" and ").append(it.next().toCQL());
        }
        return sb.toString();
    }
}
