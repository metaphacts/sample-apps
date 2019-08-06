package com.metaphacts.sail.ld;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public class LinkedDataDocumentSailConfig extends AbstractSailImplConfig {

    @Override
    public void validate() throws SailConfigException {
        super.validate();
    }

    @Override
    public Resource export(Model m) {
        return super.export(m);
    }

    @Override
    public void parse(Model m, Resource implNode) throws SailConfigException {
        super.parse(m, implNode);
    }
}
