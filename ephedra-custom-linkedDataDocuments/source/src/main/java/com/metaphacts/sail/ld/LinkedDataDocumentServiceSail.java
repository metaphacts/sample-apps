package com.metaphacts.sail.ld;

import java.io.File;
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

public class LinkedDataDocumentServiceSail extends AbstractSail {

    protected static final Logger logger = LoggerFactory.getLogger(LinkedDataDocumentServiceSail.class);

    protected HttpClient httpClient;

    protected boolean useCache = true;

    protected File cacheDir;

    public LinkedDataDocumentServiceSail() {
    }

    @Override
    protected SailConnection getConnectionInternal() throws SailException {
        return new LinkedDataDocumentServiceSailConnection(this);
    }

    @Override
    protected void initializeInternal() throws SailException {
        httpClient = createHttpClient();
        if (useCache) {
            if (getDataDir() == null) {
                throw new SailException("Data dir is not set, cannot use caching");
            }
            cacheDir = new File(getDataDir(), "cache");
            if (!cacheDir.isDirectory()) {
                if (!cacheDir.mkdirs()) {
                    throw new SailException("Failed to create cache directory at " + cacheDir);
                }
            }
        }
        super.initializeInternal();
    }

    protected HttpClient getHttpClient() {
        return httpClient;
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
