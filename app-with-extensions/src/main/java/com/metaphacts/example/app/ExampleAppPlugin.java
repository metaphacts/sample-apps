/*
 * Copyright (C) 2015-2020, metaphacts GmbH
 */
package com.metaphacts.example.app;



import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.HelperRegistry;

import jakarta.inject.Inject;
import jakarta.inject.Provider;


public class ExampleAppPlugin extends Plugin {
    private static final Logger logger = LoggerFactory.getLogger(ExampleAppPlugin.class);
    
    protected Provider<HelperRegistry> helperRegistry;

    public ExampleAppPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Inject
    public void setHelperRegistry(Provider<HelperRegistry> helperRegistry) {
        this.helperRegistry = helperRegistry;
    }

    @Override
    public void start() {
        logger.debug("Starting plugin");
        
        helperRegistry.get().registerHelpers(AppHelpers.class);
        super.start();
    }
    
    @Override
    public void stop() {
        logger.debug("Stopping plugin");
        helperRegistry.get().registerHelpers(AppHelpers.class);
        super.stop();
    }
}
