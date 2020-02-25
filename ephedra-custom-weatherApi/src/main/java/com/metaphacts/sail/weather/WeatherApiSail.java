package com.metaphacts.sail.weather;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherApiSail extends AbstractSail {

    private static final Logger logger = LoggerFactory.getLogger(WeatherApiSail.class);

    protected HttpClient httpClient;

    protected String apiKey;

    public WeatherApiSail() {

    }

    @Override
    protected SailConnection getConnectionInternal() throws SailException {
        return new WeatherApiSailConnection(this);
    }

    @Override
    protected void initializeInternal() throws SailException {
        httpClient = createHttpClient();
        super.initializeInternal();
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    protected String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void shutDownInternal() throws SailException {
        if (httpClient instanceof CloseableHttpClient) {
            try {
                ((CloseableHttpClient) httpClient).close();
            } catch (IOException e) {
                logger.warn("Failed to close http client: " + e.getMessage());
                logger.debug("Details:", e);
            }
        }
    }

    protected HttpClient createHttpClient() {
        return HttpClients.custom().useSystemProperties().build();
    }

    @Override
    public boolean isWritable() throws SailException {
        return false;
    }

    @Override
    public ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }
}
