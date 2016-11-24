package org.xbib.cql.elasticsearch;

import org.xbib.cql.elasticsearch.ast.Expression;
import org.xbib.cql.elasticsearch.ast.Modifier;
import org.xbib.cql.elasticsearch.ast.Name;
import org.xbib.cql.elasticsearch.ast.Operator;
import org.xbib.cql.elasticsearch.ast.Token;

/**
 *
 */
public interface Visitor {

    void visit(Token node);

    void visit(Name node);

    void visit(Modifier node);

    void visit(Operator node);

    void visit(Expression node);

}
