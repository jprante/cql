package org.xbib.cql;

import org.xbib.cql.model.CQLQueryModel;
import org.xbib.cql.model.Facet;
import org.xbib.cql.model.Filter;
import org.xbib.cql.model.Option;

/**
 * This is a CQL abstract syntax tree generator useful for normalizing CQL queries.
 */
public final class CQLGenerator implements Visitor {

    /**
     * helper for managing our CQL query model (facet/filter/option contexts, breadcrumb trails etc.).
     */
    private CQLQueryModel model;

    private Visitor substitutor;

    public CQLGenerator() {
        this.model = new CQLQueryModel();
    }

    public CQLGenerator setSubstitutor(Visitor substitutor) {
        this.substitutor = substitutor;
        return this;
    }

    public CQLGenerator setModel(CQLQueryModel model) {
        this.model = model;
        return this;
    }

    public CQLQueryModel getModel() {
        return model;
    }

    public String getResult() {
        return model.getQuery();
    }

    @Override
    public void visit(SortedQuery node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getSortSpec() != null) {
            node.getSortSpec().accept(this);
        }
        if (node.getQuery() != null) {
            node.getQuery().accept(this);
        }
        model.setQuery(node.toString());
    }

    @Override
    public void visit(Query node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getPrefixAssignments() != null) {
            for (PrefixAssignment assignment : node.getPrefixAssignments()) {
                assignment.accept(this);
            }
        }
        if (node.getQuery() != null) {
            node.getQuery().accept(this);
        }
        if (node.getScopedClause() != null) {
            node.getScopedClause().accept(this);
        }
    }

    @Override
    public void visit(SortSpec node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getSingleSpec() != null) {
            node.getSingleSpec().accept(this);
        }
        if (node.getSortSpec() != null) {
            node.getSortSpec().accept(this);
        }
    }

    @Override
    public void visit(SingleSpec node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getIndex() != null) {
            node.getIndex().accept(this);
        }
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
    }

    @Override
    public void visit(PrefixAssignment node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        node.getPrefix().accept(this);
        node.getURI().accept(this);
    }

    @Override
    public void visit(ScopedClause node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getScopedClause() != null) {
            node.getScopedClause().accept(this);
        }
        node.getSearchClause().accept(this);
        if (node.getBooleanGroup() != null) {
            node.getBooleanGroup().accept(this);
            BooleanOperator op = node.getBooleanGroup().getOperator();
            checkFilter(op, node);
            checkFilter(op, node.getScopedClause());
        }
    }

    @Override
    public void visit(BooleanGroup node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
    }

    @Override
    public void visit(SearchClause node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getQuery() != null) {
            node.getQuery().accept(this);
        }
        if (node.getTerm() != null) {
            node.getTerm().accept(this);
        }
        if (node.getIndex() != null) {
            node.getIndex().accept(this);
            String context = node.getIndex().getContext();
            if (CQLQueryModel.FACET_INDEX_NAME.equals(context)) {
                Facet<Term> facet = new Facet<>(node.getIndex().getName());
                facet.setValue(node.getTerm());
                model.addFacet(facet);
            } else if (CQLQueryModel.OPTION_INDEX_NAME.equals(context)) {
                Option<Term> option = new Option<>();
                option.setName(node.getIndex().getName());
                option.setValue(node.getTerm());
                model.addOption(option);
            }
        }
        if (node.getRelation() != null) {
            node.getRelation().accept(this);
        }
    }

    @Override
    public void visit(Relation node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getModifierList() != null) {
            node.getModifierList().accept(this);
        }
    }

    @Override
    public void visit(Modifier node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        if (node.getTerm() != null) {
            node.getTerm().accept(this);
        }
        if (node.getName() != null) {
            node.getName().accept(this);
        }
    }

    @Override
    public void visit(ModifierList node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
        for (Modifier modifier : node.getModifierList()) {
            modifier.accept(this);
        }
    }

    @Override
    public void visit(Term node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
    }

    @Override
    public void visit(Identifier node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
    }

    @Override
    public void visit(SimpleName node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
    }

    @Override
    public void visit(Index node) {
        if (substitutor != null) {
            substitutor.visit(node);
        }
    }

    public String withBreadcrumbs() {
        return model.toCQL();
    }

    private void checkFilter(BooleanOperator op, ScopedClause node) {
        if (node.getSearchClause().getIndex() != null
                && CQLQueryModel.FILTER_INDEX_NAME.equals(node.getSearchClause().getIndex().getContext())) {
            String filtername = node.getSearchClause().getIndex().getName();
            Comparitor filterop = node.getSearchClause().getRelation().getComparitor();
            Term filterterm = node.getSearchClause().getTerm();
            Filter<AbstractNode> filter2 = new Filter<>(filtername, filterterm, filterop);
            model.addFilter(op, filter2);
        }
    }
}
