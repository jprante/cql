package org.xbib.cql.model.breadcrumb;

import org.xbib.cql.BooleanOperator;
import org.xbib.cql.model.Filter;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Filter breadcrumbs.
 * @param <V> the filter value parameter
 */
public class FilterBreadcrumbTrail<V> extends TreeSet<Filter<V>> {

    private static final long serialVersionUID = 3150916290466088839L;
    private BooleanOperator op;

    public FilterBreadcrumbTrail(BooleanOperator op) {
        super();
        this.op = op;
    }

    @Override
    public String toString() {
        return toCQL();
    }

    public String toCQL() {
        StringBuilder sb = new StringBuilder();
        if (isEmpty()) {
            return sb.toString();
        }
        if (op == BooleanOperator.OR && size() > 1) {
            sb.append('(');
        }
        Iterator<Filter<V>> it = this.iterator();
        sb.append(it.next().toCQL());
        while (it.hasNext()) {
            sb.append(' ').append(op).append(' ').append(it.next().toCQL());
        }
        if (op == BooleanOperator.OR && size() > 1) {
            sb.append(')');
        }
        return sb.toString();
    }
}
