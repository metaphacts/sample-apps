package com.metaphacts.sail.weather;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;


public class WeatherApiSailFactory implements SailFactory {

	public static final String SAIL_TYPE = "metaphacts:WeatherApi";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public WeatherApiSailConfig getConfig() {
		return new WeatherApiSailConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!(config instanceof WeatherApiSailConfig)) {
			throw new SailConfigException("Wrong config type: " + config.getClass().getCanonicalName() + ". ");
		}

		WeatherApiSailConfig c = (WeatherApiSailConfig) config;

		WeatherApiSail sail = new WeatherApiSail();

		sail.setApiKey(c.getApiKey());

		return sail;
	}

}
