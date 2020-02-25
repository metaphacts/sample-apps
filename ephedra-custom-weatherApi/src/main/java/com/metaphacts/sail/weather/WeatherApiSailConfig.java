package com.metaphacts.sail.weather;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public class WeatherApiSailConfig extends AbstractSailImplConfig {

	private static final String NAMESPACE = "http://www.metaphacts.com/ontologies/weather-cfg#";

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	public static final IRI API_KEY = vf.createIRI(NAMESPACE, "apiKey");

	private String apiKey;

	@Override
	public void validate() throws SailConfigException {
		super.validate();
		
		if (getApiKey() == null) {
			throw new SailConfigException("API key is required to interact with the Weather API.");
		}
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		if (apiKey != null) {
			m.add(implNode, API_KEY, vf.createLiteral(apiKey));
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		Models.objectLiteral(m.filter(implNode, API_KEY, null)).ifPresent(value -> setApiKey(value.stringValue()));
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}
