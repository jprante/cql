package org.xbib.cql.model.breadcrumb;

import org.xbib.cql.model.Option;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * An Option breadcrumb trail is a trail of attributes (key/value pairs).
 * There is no interdependency between attributes; all values are allowed,
 * even if they interfere with each other, the trail does not resolve it.
 * @param <V> the option value parameter
 */
public class OptionBreadcrumbTrail<V> extends TreeSet<Option<V>> {

    private static final long serialVersionUID = -3426025113354994279L;

    public OptionBreadcrumbTrail() {
    }

    @Override
    public String toString() {
        return toCQL();
    }

    /**
     * Conjunct all CQL options to form a valid CQL string.
     *
     * @return the CQL string
     */
    public String toCQL() {
        StringBuilder sb = new StringBuilder();
        if (isEmpty()) {
            return sb.toString();
        }
        Iterator<Option<V>> it = iterator();
        if (it.hasNext()) {
            sb.append(it.next().toCQL());
        }
        while (it.hasNext()) {
            sb.append(" and ").append(it.next().toCQL());
        }
        return sb.toString();
    }
}
