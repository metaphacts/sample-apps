package com.metaphacts.sail.ld;

import java.io.File;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.metaphacts.sail.ld.LinkedDataDocumentServiceSailConnection.LinkedDataDocumentStatementCollector;

public class LinkedDataDocumentServiceSailTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void test() throws Exception {

        Repository repo = new SailRepository(new LinkedDataDocumentServiceSail());
        repo.setDataDir(tempFolder.newFolder("dataDir"));
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "SELECT ?subj ?publication WHERE { "
                    + "?subj <http://www.metaphacts.com/ontologies/ld#document> <https://dblp.uni-trier.de/pers/tr/h/Haase_0001:Peter.nt> ."
                    + "?subj <https://dblp.uni-trier.de/rdf/schema-2017-04-18#authorOf> ?publication ." 
                    + "}";
            TupleQuery tq = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }

        }
        repo.shutDown();
    }

    @Test
    public void testWithContentNegotiation2() throws Exception {

        /*
         * Note: rdf/xml document uses different namespace
         * 
         * https://dblp.org/rdf/schema-2017-04-18#authorOf
         * 
         * vs
         * 
         * https://dblp.uni-trier.de/rdf/schema-2017-04-18#authorOf
         */
        Repository repo = new SailRepository(new LinkedDataDocumentServiceSail());
        repo.setDataDir(tempFolder.newFolder("dataDir"));
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "SELECT ?publication WHERE { "
                    + "<https://dblp.org/pers/h/Haase_0001:Peter> <https://dblp.org/rdf/schema-2017-04-18#authorOf> ?publication ."
                    + "?subj <http://www.metaphacts.com/ontologies/ld#followSilent> true ." 
                    + "}";
            TupleQuery tq = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }

        }
        repo.shutDown();
    }

    @Test
    public void testFollowPredicate() throws Exception {

        Repository repo = new SailRepository(new LinkedDataDocumentServiceSail());
        repo.setDataDir(tempFolder.newFolder("dataDir"));
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "SELECT ?subj ?publication ?title WHERE { "
                    + "?subj <http://www.metaphacts.com/ontologies/ld#document> <https://dblp.uni-trier.de/pers/tr/h/Haase_0001:Peter.nt> ."
                    + "?subj <http://www.metaphacts.com/ontologies/ld#follow> <https://dblp.uni-trier.de/rdf/schema-2017-04-18#authorOf> . "
                    + "?subj <http://www.metaphacts.com/ontologies/ld#followSilent> true ."
                    + "?subj <https://dblp.uni-trier.de/rdf/schema-2017-04-18#authorOf> ?publication . "
                    + "?publication <https://dblp.org/rdf/schema-2017-04-18#title> ?title" 
                    + "}";
            TupleQuery tq = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }

        }
        repo.shutDown();
    }

    @Test
    public void testFollowPredicate2() throws Exception {

        Repository repo = new SailRepository(new LinkedDataDocumentServiceSail());
//		repo.setDataDir(tempFolder.newFolder("dataDir"));
        repo.setDataDir(new File("tmp/cache"));
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "SELECT ?subj ?coAuthor ?name ?affiliation WHERE { "
                    + "?subj <http://www.metaphacts.com/ontologies/ld#document> <https://dblp.org/pid/h/PeterHaase1.rdf> ."
                    + "?subj <http://www.metaphacts.com/ontologies/ld#follow> <https://dblp.org/rdf/schema-2017-04-18#coCreatorWith> . "
                    + "?subj <http://www.metaphacts.com/ontologies/ld#followSilent> true . "
                    + "?subj <https://dblp.org/rdf/schema-2017-04-18#coCreatorWith> ?coAuthor . "
                    + "?coAuthor <https://dblp.org/rdf/schema-2017-04-18#primaryFullPersonName> ?name ."
                    + "OPTIONAL { ?coAuthor <https://dblp.org/rdf/schema-2017-04-18#primaryAffiliation> ?affiliation } "
                    + "}";
            TupleQuery tq = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }

        }
        repo.shutDown();
    }

    @Test
    public void testQueryExpression() throws Exception {

        String queryString = "SELECT * WHERE { "
                + "?subj <http://www.metaphacts.com/ontologies/ld#document> <https://dblp.uni-trier.de/pers/tr/h/Haase_0001:Peter.nt> ."
                + "?subj <https://dblp.uni-trier.de/rdf/schema-2017-04-18#authorOf> ?publication ."
                + "?subj <urn:p> <urn:o> ." 
                + "?subj <urn:p2> <urn:o2> ." 
                + "}";

        ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, queryString,
                "http://example.org");

        TupleExpr expr = query.getTupleExpr();

        System.out.println(expr.toString());

        LinkedDataDocumentStatementCollector info = new LinkedDataDocumentStatementCollector();
        expr.visit(info);

        System.out.println(info.getQueryExpression());

    }

}
