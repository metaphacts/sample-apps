package com.metaphacts.sail.ld;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.common.collect.Sets;

public class LinkedDataDocumentServiceSailConnection extends AbstractSailConnection {

    private static final String NAMESPACE = "http://www.metaphacts.com/ontologies/ld#";
    public static final IRI LD_RESOURCE = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "document");
    public static final IRI LD_FOLLOW = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "follow");
    public static final IRI LD_FOLLOW_SILENT = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "followSilent");

    protected final LinkedDataDocumentServiceSail ldSail;

    public LinkedDataDocumentServiceSailConnection(LinkedDataDocumentServiceSail sailBase) {
        super(sailBase);
        this.ldSail = sailBase;
    }

    @Override
    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tuple,
            Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {

        TupleExpr cloned = tuple.clone();
        // Important! You need to assign the values to the variables which were already
        // bound in other clauses of the query
        new BindingAssigner().optimize(cloned, dataset, bindings);

        LinkedDataDocumentStatementCollector info = new LinkedDataDocumentStatementCollector();
        cloned.visit(info);

        Model model;
        try {
            model = readDocument(info.getDocumentURL());
        } catch (IOException e) {
            throw new SailException(e);
        }

        MemoryStore store = new MemoryStore();
        try {
            store.initialize();
            try (SailConnection conn = store.getConnection()) {
                conn.begin();
                model.stream().forEach(st -> conn.addStatement(st.getSubject(), st.getPredicate(), st.getObject()));

                // retrieve documents of follow predicates
                if (!info.followPredicates.isEmpty()) {

                    for (IRI followPredicate : info.followPredicates) {
                        Model followPredicateStmts = model.filter(null, followPredicate, null);
                        followPredicateStmts.parallelStream().forEach(followSt -> {
                            try {
                                Model newDoc = readDocument(followSt.getObject().stringValue());
                                newDoc.stream().forEach(
                                        st -> conn.addStatement(st.getSubject(), st.getPredicate(), st.getObject()));
                            } catch (IOException e) {
                                if (info.followSilent) {
                                    logger.debug("failed to read document for '" + followSt.getObject() + "': "
                                            + e.getMessage());
                                } else {
                                    throw new SailException("failed to read document for '" + followSt.getObject()
                                            + "': " + e.getMessage(), e);
                                }
                            }
                        });
                    }

                }

                if (logger.isTraceEnabled()) {
                    for (Statement st : Iterations.asList(conn.getStatements(null, null, null, false))) {
                        logger.trace(st.toString());
                    }
                }

                conn.commit();

                // if document subject var is given, try to find a binding
                if (info.documentSubjectVar != null) {

                    MapBindingSet newBindings = new MapBindingSet();
                    Value documentSuject = identifySubject(model);
                    newBindings.addBinding(info.documentSubjectVar, documentSuject);
                    bindings.forEach(binding -> newBindings.addBinding(binding));
                    bindings = newBindings;
                }

                // Note: we need to materialize the result as the MemoryStore is only temporary
                return new CollectionIteration<BindingSet, QueryEvaluationException>(Iterations
                        .asList(conn.evaluate(info.getQueryExpression(), dataset, bindings, includeInferred)));
            }
        } finally {
            store.shutDown();
        }
    }

    protected Model readDocument(String documentUrl) throws IOException {

        // check if the document is already cached
        if (ldSail.useCache) {
            File cacheFile = cacheFile(documentUrl);
            if (cacheFile.isFile()) {
                try (InputStream in = new FileInputStream(cacheFile)) {
                    String baseURI = "http://example.org/";
                    return Rio.parse(in, baseURI, RDFFormat.BINARY);
                }
            }
        }

        HttpClient client = getHttpClient();

        HttpGet request = new HttpGet(documentUrl);

        request.addHeader("Accept", "application/n-triples;q=0.9,application/rdf+xml,text/turtle;q=0.8");

        HttpResponse response = client.execute(request);
        try {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                throw new IOException("Unpexted response: " + responseCode);
            }

            HttpEntity entity = response.getEntity();
            ContentType contentType = ContentType.getOrDefault(entity);

            Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
            RDFFormat rdfFormat = RDFFormat.matchMIMEType(contentType.getMimeType(), rdfFormats)
                    .orElse(RDFFormat.RDFXML);

            Model result;
            try (InputStream in = response.getEntity().getContent()) {
                String baseURI = "http://example.org/";

                result = Rio.parse(in, baseURI, rdfFormat);
            }

            if (ldSail.useCache) {
                // create a copy in the cache folder for next consumption
                try (OutputStream output = new FileOutputStream(cacheFile(documentUrl))) {
                    Rio.write(result, output, RDFFormat.BINARY);
                }
            }

            return result;
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     * Tries to identify the most likely subject in the model which represents the
     * document.
     * 
     * @param model
     * @return
     */
    protected Value identifySubject(Model model) {

        Set<Resource> subjects = model.subjects();
        if (subjects.size() == 1) {
            return subjects.iterator().next();
        }

        // find the subject with the highest number of outgoing stmts
        Value res = null;
        int nStmts = -1;
        for (Resource subj : subjects) {
            Model subModel = model.filter(subj, null, null);
            if (subModel.size() > nStmts) {
                res = subj;
                nStmts = subModel.size();
            }
        }

        return res;
    }

    private File cacheFile(String documentUrl) {
        return new File(ldSail.cacheDir, "doc_" + documentUrl.hashCode());
    }

    protected HttpClient getHttpClient() {
        return ldSail.getHttpClient();
    }

    static class LinkedDataDocumentStatementCollector extends AbstractQueryModelVisitor<SailException> {

        protected String documentSubjectVar;

        protected String ldDocumentUrl;

        protected List<StatementPattern> stmts = new ArrayList<>();

        protected Projection proj;

        protected Set<IRI> followPredicates = Sets.newHashSet();

        protected boolean followSilent = false;

        @Override
        public void meet(StatementPattern node) throws SailException {

            if (node.getPredicateVar().hasValue() && node.getPredicateVar().getValue().equals(LD_RESOURCE)) {

                if (ldDocumentUrl != null) {
                    throw new SailException("LD document URL is already defined, cannot redefine.");
                }
                Value v = node.getObjectVar().getValue();
                ldDocumentUrl = v.stringValue();
                if (!node.getSubjectVar().hasValue()) {
                    documentSubjectVar = node.getSubjectVar().getName();
                }
                return;
            }
            if (node.getPredicateVar().hasValue() && node.getPredicateVar().getValue().equals(LD_FOLLOW)) {
                Value v = node.getObjectVar().getValue();
                if (v == null || !(v instanceof IRI)) {
                    throw new SailException("LD Follow predicate must be a valid IRI");
                }
                followPredicates.add((IRI) v);
                return;
            }
            if (node.getPredicateVar().hasValue() && node.getPredicateVar().getValue().equals(LD_FOLLOW_SILENT)) {
                Value v = node.getObjectVar().getValue();
                if (v == null || !(v instanceof Literal)) {
                    throw new SailException("LD Follow Silent predicate must point to boolean literal");
                }
                followSilent = ((Literal) v).booleanValue();
                return;
            }
            stmts.add(node);
            super.meet(node);
        }

        @Override
        public void meet(Projection node) throws SailException {
            proj = node.clone();
            super.meet(node);
        }

        public String getDocumentURL() {
            // explicit URL
            if (ldDocumentUrl != null) {
                return ldDocumentUrl;
            }

            // inspect the subject of the first statement
            Var subj = stmts.get(0).getSubjectVar();
            if (subj.hasValue()) {
                return subj.getValue().stringValue();
            }

            throw new SailException("Could not identify URL for the document.");
        }

        public TupleExpr getQueryExpression() {

            TupleExpr expr;
            if (stmts.size() == 1) {
                expr = stmts.get(0);
            } else {

                Join join = new Join();
                join.setLeftArg(stmts.get(0));
                for (int i = 1; i < stmts.size() - 1; i++) {
                    join.setRightArg(stmts.get(i));
                    Join prevJoin = join;
                    join = new Join();
                    join.setLeftArg(prevJoin);
                }
                join.setRightArg(stmts.get(stmts.size() - 1));
                expr = join;
            }

            proj.setArg(expr);
            return proj;
        }
    }

    // non-required interface methods
    // TODO remove as soon as metaphactory SDK exposes a suitable base class

    @Override
    protected void closeInternal() throws SailException {
    }

    @Override
    protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
        return new CollectionIteration<>(Collections.emptyList());
    }

    @Override
    protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, IRI pred,
            Value obj, boolean includeInferred, Resource... contexts) throws SailException {
        return new CollectionIteration<>(Collections.emptyList());
    }

    @Override
    protected long sizeInternal(Resource... contexts) throws SailException {
        return 0;
    }

    @Override
    protected void startTransactionInternal() throws SailException {
    }

    @Override
    protected void commitInternal() throws SailException {
    }

    @Override
    protected void rollbackInternal() throws SailException {
    }

    @Override
    protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
    }

    @Override
    protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
            throws SailException {
    }

    @Override
    protected void clearInternal(Resource... contexts) throws SailException {
    }

    @Override
    protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
        return new CollectionIteration<>(Collections.emptyList());
    }

    @Override
    protected String getNamespaceInternal(String prefix) throws SailException {
        return null;
    }

    @Override
    protected void setNamespaceInternal(String prefix, String name) throws SailException {
    }

    @Override
    protected void removeNamespaceInternal(String prefix) throws SailException {
    }

    @Override
    protected void clearNamespacesInternal() throws SailException {
    }

    @Override
    public boolean pendingRemovals() {
        return false;
    }

}
