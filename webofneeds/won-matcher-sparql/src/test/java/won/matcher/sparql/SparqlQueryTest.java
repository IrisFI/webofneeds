package won.matcher.sparql;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.tdb.TDB;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import won.matcher.sparql.actor.SparqlMatcherActor;
import won.matcher.sparql.actor.SparqlMatcherUtils;

/**
 * Test for experimenting with in-memory datasets and queries.
 * @author fkleedorfer
 *
 */
public class SparqlQueryTest  {
    
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
    
    private String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            while ((bytesRead = in.read(buffer)) > -1) {
                baos.write(buffer,0,bytesRead);
            }
            return new String(baos.toByteArray(),Charset.defaultCharset());
        }
        
    }
    
    @Test
    //@Ignore // useful for trying things out, does not make so much sense as a unit test
    public void testQuery() throws Exception {
        Dataset dataset = DatasetFactory.create();
        RDFDataMgr.read(dataset, getResourceAsStream("sparqlquerytest/need2.trig"), Lang.TRIG);
        String queryString = getResourceAsString("sparqlquerytest/query2.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        
        Op queryWithGraphClause = SparqlMatcherUtils.addGraphOp(queryOp, Optional.of("urn:x-arq:UnionGraph"));
        
        System.out.println("query algebra: " + queryOp);
        System.out.println("transformed query algebra: " + queryWithGraphClause);
        System.out.println("optimized query algebra:" + Algebra.optimize(queryWithGraphClause));
        System.out.println("\nDataset:");
        RDFDataMgr.write(System.out, dataset, Lang.TRIG);
        System.out.println("\nQuery:");

        query = OpAsQuery.asQuery(queryWithGraphClause);
        System.out.println(query);
        System.out.println("\nResult:");
        
        
        try (QueryExecution execution = QueryExecutionFactory.create(query, dataset)) {
            execution.getContext().set(TDB.symUnionDefaultGraph, true);
            
            ResultSet result = execution.execSelect();
            Set<String> resultNeeds = new HashSet<>();
            while (result.hasNext()) {
                QuerySolution solution = result.next();
                System.out.println("solution:" + solution);
                String foundNeedURI = solution.get("result").toString();
                resultNeeds.add(foundNeedURI);
            }
            System.out.println(resultNeeds);
        }
    }
    
    
    @Test
    public void testAddGraphOp() throws Exception {
        Dataset dataset = DatasetFactory.create();
        RDFDataMgr.read(dataset, getResourceAsStream("sparqlquerytest/need.trig"), Lang.TRIG);
        String queryString = getResourceAsString("sparqlquerytest/query.rq");
        String queryWithGraphClauseString = getResourceAsString("sparqlquerytest/query-with-graph-clause.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        Query expectedQueryWithGraphClauseString = QueryFactory.create(queryWithGraphClauseString);
        Op expectedQueryWithGraphClause = Algebra.compile(expectedQueryWithGraphClauseString);
        Op queryWithGraphClause = SparqlMatcherUtils.addGraphOp(queryOp, Optional.of("g"));
        
        Assert.assertEquals("Adding graph op to query did not yield expected result", expectedQueryWithGraphClause , queryWithGraphClause);
    }

    
    
    
    
}
