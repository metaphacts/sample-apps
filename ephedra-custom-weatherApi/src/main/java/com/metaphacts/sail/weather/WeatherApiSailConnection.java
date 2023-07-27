package com.metaphacts.sail.weather;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingAssignerOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

/**
 * 
 * Parameters
 * 
 * - weather:longitude 
 * - weather:latitude 
 * - weather:dataPointObject -> list of data point objects to retrieve 
 * - weather:block -> daily or hourly, default daily
 * 
 * @author as
 *
 */
public class WeatherApiSailConnection extends AbstractSailConnection {

    private static final String NAMESPACE = "http://www.metaphacts.com/ontologies/weather#";
    public static final IRI LONGITUDE = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "longitude");
    public static final IRI LATITUDE = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "latitude");

    public static final IRI BLOCK = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "block");
    public static final IRI DATA_POINT_OBJECT = SimpleValueFactory.getInstance().createIRI(NAMESPACE,
            "dataPointObject");

    private final WeatherApiSail sail;

    public WeatherApiSailConnection(WeatherApiSail sailBase) {
        super(sailBase);
        this.sail = sailBase;
    }

    @Override
    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr,
            Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
        TupleExpr cloned = tupleExpr.clone();
        new BindingAssignerOptimizer().optimize(cloned, dataset, bindings);
        StatementPatternCollector collector = new StatementPatternCollector();
        cloned.visit(collector);
        List<StatementPattern> stmtPatterns = collector.getStatementPatterns();
        WeatherApiConfig weatherConfig = extractConfig(stmtPatterns);
        return executeAndConvertResultsToBindingSet(weatherConfig);
    }

    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> executeAndConvertResultsToBindingSet(
            WeatherApiConfig weatherApiConfig) {


        HttpClient client = getHttpClient();
        HttpUriRequest request = createRequest(weatherApiConfig);

        try {
            HttpResponse response = client.execute(request);
            try {

                StatusLine statusLine = response.getStatusLine();
                int responseCode = statusLine.getStatusCode();
                validateResponse(response, responseCode, statusLine);

                try (InputStream in = response.getEntity().getContent()) {
                    return consumeResult(response, in, weatherApiConfig);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } catch (IOException e) {
            throw new SailException("Failed to submit weather API request: " + e.getMessage(), e);
        }
    }

    protected HttpUriRequest createRequest(WeatherApiConfig cfg) {

        HttpGet request = new HttpGet(serviceUrl(cfg));

        request.addHeader("Accept-Encoding", "gzip");

        return request;
    }

    /**
     * 
     * @param cfg
     * @return the service URL including query parameters
     */
    protected URI serviceUrl(WeatherApiConfig cfg) {
        String apiKey = apiKey();
        if (apiKey == null) {
            throw new IllegalStateException("No api key defined");
        }

        try {
            // https://api.darksky.net/forecast/[key]/[latitude],[longitude]
            String url = "https://api.darksky.net/forecast/" + apiKey + "/" + cfg.latitude + "," + cfg.longitude;
            URIBuilder builder = new URIBuilder(url);
            builder.addParameter("exclude", cfg.getBlockExcludeString());
            builder.addParameter("units", "si");
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unexpected error while constructing service URI: " + e.getMessage());
        }
    }

    protected void validateResponse(HttpResponse response, int responseCode, StatusLine statusLine) throws IOException {
        if (responseCode != 200) {
            throw new IOException("Unpexted response: " + responseCode);
        }
    }

    @SuppressWarnings("unchecked")
    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> consumeResult(HttpResponse response,
            InputStream in, WeatherApiConfig cfg) throws IOException {

        List<BindingSet> res = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> map = (HashMap<String, Object>) mapper.readValue(in, HashMap.class);

            // e.g. "daily"
            Map<String, Object> block = (Map<String, Object>) map.get(cfg.block);

            // block data
            List<Object> data = (List<Object>) block.get("data");

            // iterate the data (e.g. the daily forecast)
            for (Object dataItem : data) {
                Map<String, Object> values = (Map<String, Object>) dataItem;

                MapBindingSet bindings = new MapBindingSet();
                for (Entry<String, String> outputMapping : cfg.dataPointObjectToOutput.entrySet()) {
                    String dataPointObjectKey = outputMapping.getKey();
                    String outputBinding = outputMapping.getValue();

                    Object value = values.get(dataPointObjectKey);

                    // convert unix timestamp to date, value is unix time in seconds
                    if ("time".contentEquals(dataPointObjectKey)) {
                        value = new Date((int) value * 1000L);
                    }
                    if (value == null) {
                        throw new SailException("No value available for data point object " + dataPointObjectKey);
                    }
                    bindings.addBinding(outputBinding, toLiteral(value));
                }

                res.add(bindings);
            }

        } catch (Exception e) {
            throw new SailException("Failed to read document structure: " + e.getMessage(), e);
        }

        return new CollectionIteration<BindingSet, QueryEvaluationException>(res);
    }

    protected Literal toLiteral(Object object) {
        if (object == null) {
            return null;
        }
        ValueFactory vf = SimpleValueFactory.getInstance();
        if (object.getClass().equals(String.class)) {
            return vf.createLiteral((String) object);
        } else if (object.getClass().equals(Integer.class) || object.getClass().equals(int.class)) {
            return vf.createLiteral((int) object);
        } else if (object.getClass().equals(Long.class) || object.getClass().equals(long.class)) {
            return vf.createLiteral((long) object);
        } else if (object.getClass().equals(Double.class) || object.getClass().equals(double.class)) {
            return vf.createLiteral((double) object);
        } else if (object.getClass().equals(Boolean.class) || object.getClass().equals(boolean.class)) {
            return vf.createLiteral((boolean) object);
        } else if (object.getClass().equals(Float.class) || object.getClass().equals(float.class)) {
            return vf.createLiteral((float) object);
        } else if (object instanceof Date) {
            return vf.createLiteral((Date) object);
        } else {
            return vf.createLiteral(object.toString());
        }
    }


    protected WeatherApiConfig extractConfig(List<StatementPattern> stmts) throws SailException {

        WeatherApiConfig res = new WeatherApiConfig();

        for (StatementPattern stmt : stmts) {

            if (!stmt.getPredicateVar().hasValue()) {
                continue;
            }
            if (!(stmt.getObjectVar().hasValue() && stmt.getObjectVar().getValue() instanceof Literal)) {
                continue;
            }

            Value pred = stmt.getPredicateVar().getValue();
            if (pred.equals(LONGITUDE)) {
                res.longitude = stmt.getObjectVar().getValue().stringValue();
            }

            else if (pred.equals(LATITUDE)) {
                res.latitude = stmt.getObjectVar().getValue().stringValue();
            }

            else if (pred.equals(BLOCK)) {
                res.block = stmt.getObjectVar().getValue().stringValue();
            }

            else if (pred.equals(DATA_POINT_OBJECT)) {
                String dataPointObject = stmt.getObjectVar().getValue().stringValue();
                // Note: for now use the dataPointObject name also as output binding
                res.dataPointObjectToOutput.put(dataPointObject, dataPointObject);
            }
        }

        if (res.latitude == null || res.longitude == null) {
            throw new SailException("Required input is missing: latitude and longitude need to be defined.");
        }

        return res;
    }

    protected HttpClient getHttpClient() {
        return sail.getHttpClient();
    }

    protected String apiKey() {
        return sail.getApiKey();
    }

    static class WeatherApiConfig {

        static final Set<String> AVAILABLE_BLOCKS = Sets.newHashSet("currently", "minutely", "hourly", "daily",
                "alerts", "flags");

        public String longitude;

        public String latitude;

        public String block = "daily";

        // map "data point object" to output binding name
        public Map<String, String> dataPointObjectToOutput = new LinkedHashMap<>();

        /**
         * @return comma separated list of "block"s for the "exclude" request parameter
         */
        public String getBlockExcludeString() {
            return AVAILABLE_BLOCKS.stream().filter(s -> !(s.equals(block))).collect(Collectors.joining(","));
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
