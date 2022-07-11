package com.metaphacts.sail.ld;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.metaphacts.federation.service.ServiceDescriptor;
import com.metaphacts.federation.service.ServiceDescriptor.Parameter;
import com.metaphacts.federation.service.ServiceDescriptorAware;
import com.metaphacts.federation.service.ServiceDescriptorImpl;
import com.metaphacts.federation.service.ServiceDescriptorImpl.ParameterImpl;

public class LinkedDataDocumentServiceSail extends AbstractSail implements ServiceDescriptorAware {

    protected static final Logger logger = LoggerFactory.getLogger(LinkedDataDocumentServiceSail.class);

    protected HttpClient httpClient;

    protected boolean useCache = true;

    protected File cacheDir;

    protected final ServiceDescriptor serviceDescriptor;

    public LinkedDataDocumentServiceSail() {
        serviceDescriptor = createServiceDescriptor();
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

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    protected ServiceDescriptor createServiceDescriptor() {
        ServiceDescriptorImpl serviceDescriptor = new ServiceDescriptorImpl();
        serviceDescriptor.setInputParameters(getInputParameters());
        serviceDescriptor.setOutputParameters(getOutputParameters());
        return serviceDescriptor;
    }

    protected Map<String, Parameter> getInputParameters() {

        Map<String, Parameter> parameters = new HashMap<>();
        ParameterImpl param;

        // linked data document
        param = new ParameterImpl();
        param.setParameterId(LinkedDataDocumentServiceSailConnection.LD_RESOURCE);
        param.setParameterName("document");
        param.setValueType(XSD.ANYURI);
        param.setObjectPatterns(Lists.newArrayList(new StatementPattern(new Var("resource"),
                TupleExprs.createConstVar(LinkedDataDocumentServiceSailConnection.LD_RESOURCE), new Var("document"))));
        parameters.put(param.getParameterName(), param);

        return parameters;
    }

    protected Map<String, Parameter> getOutputParameters() {

        Map<String, Parameter> parameters = new HashMap<>();

        
        return parameters;
    }

}
