/*
 * Copyright (C) 2015-2020, metaphacts GmbH
 */
package com.metaphacts.example.app;

import java.util.Date;

/**
 * Handlebars Helpers contributed by the {@link ExampleAppPlugin}.
 * 
 * <p>
 * See <a href="https://jknack.github.io/handlebars.java/helpers.html">Registering custom helpers</a> 
 * in the <a href="https://jknack.github.io/handlebars.java/">Handlebars</a> documentation for details.
 * </p>
 * 
 * @author Wolfgang Schell <ws@metaphacts.com>
 */
public class AppHelpers {
    /**
     * This method renders the current timestamp in the default locale.
     * 
     * <p>
     * Use like this:
     * <pre>
     * [[exampleDateTime]]
     * </pre>
     * </p>
     * @return timestamp string
     */
    public static String exampleDateTime() {
        return "date and time: " + new Date().toString();
    }
}
