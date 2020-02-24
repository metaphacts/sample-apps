/*
 * Copyright (C) 2015-2020, metaphacts GmbH
 */
package com.metaphacts.example.app;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaphacts.plugin.extension.RestExtension;
import com.metaphacts.repository.RepositoryManagerInterface;

/**
 * This class serves as a test endpoint extensions to access functionality provided via dependency injection.
 * 
 * @author Wolfgang Schell <ws@metaphacts.com>
 */
@Singleton
@Path("custom")
public class CustomQueryEndpoint implements RestExtension {
    private static final Logger logger = LoggerFactory.getLogger(CustomQueryEndpoint.class);
    
    @Inject
    protected RepositoryManagerInterface repositoryManager;
    
    public CustomQueryEndpoint() {
        logger.debug("Initializing integration endpoint /rest/extension/custom/connection-check");
    }
    
    @GET
    @Path("connection-check")
    @RequiresAuthentication
    @Produces("text/plain")
    public Response checkRepositoryConnection()
            throws Exception {
        if (!checkPermission("qaas:*")) {
            throw new ForbiddenException();
        }
        String repoID = "default";
        try {
            // use the repository
            Repository repository = repositoryManager.getRepository(repoID);
            try (RepositoryConnection connection = repository.getConnection()) {
                String query = "SELECT ?message WHERE { BIND('Hello, world!' as ?message) }";
                try (TupleQueryResult result = connection.prepareTupleQuery(query).evaluate()) {
                    return Response.ok("connection ok: " + result.next().getBinding("message").getValue().stringValue()).build();
                }
            }
            catch (Exception e) {
                return Response.serverError().entity("connection failed").build();
            }
        }
        catch (IllegalArgumentException e) {
            return Response.status(404).entity("respository does not exist").build();
        }

    }
    
    protected boolean checkPermission(String permission) {
        return SecurityUtils.getSubject().isPermitted(new WildcardPermission(permission));
    }
}
