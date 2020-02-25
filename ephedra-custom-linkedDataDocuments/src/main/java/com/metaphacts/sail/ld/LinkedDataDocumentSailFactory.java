package com.metaphacts.sail.ld;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public class LinkedDataDocumentSailFactory implements SailFactory {

    public static final String SAIL_TYPE = "metaphacts:LinkedDataDocument";

    @Override
    public String getSailType() {
        return SAIL_TYPE;
    }

    @Override
    public LinkedDataDocumentSailConfig getConfig() {
        return new LinkedDataDocumentSailConfig();
    }

    @Override
    public Sail getSail(SailImplConfig config) throws SailConfigException {
        if (!(config instanceof LinkedDataDocumentSailConfig)) {
            throw new SailConfigException("Wrong config type: " + config.getClass().getCanonicalName() + ". ");
        }
        @SuppressWarnings("unused")
        LinkedDataDocumentSailConfig c = (LinkedDataDocumentSailConfig) config;

        return new LinkedDataDocumentServiceSail();
    }

}
