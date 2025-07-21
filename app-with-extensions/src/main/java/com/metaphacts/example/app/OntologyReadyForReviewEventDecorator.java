/*
 * Copyright (C) 2015-2025, metaphacts GmbH
 */
package com.metaphacts.example.app;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.metaphacts.event.EventDecorator;
import com.metaphacts.event.EventDecoratorFactory;
import com.metaphacts.repository.RepositoryManager;
import com.metaphacts.services.assets.AssetVocabulary;
import com.metaphacts.services.ontologies.OntologyEvent.OntologyReadyForReviewEvent;

/**
 * An example {@link EventDecorator} for {@link OntologyReadyForReviewEvent}s,
 * which adds the owners and authors as custom data.
 */
public class OntologyReadyForReviewEventDecorator implements EventDecorator<OntologyReadyForReviewEvent> {

    public static final String OWNERS = "owners";
    public static final String AUTHORS = "authors";

    private final RepositoryManager repositoryManager;

    protected OntologyReadyForReviewEventDecorator(RepositoryManager repositoryManager) {
        super();
        this.repositoryManager = repositoryManager;
    }

    @Override
    public Class<? extends OntologyReadyForReviewEvent> applicableTo() {
        return OntologyReadyForReviewEvent.class;
    }

    @Override
    public Map<String, Object> getCustomData(OntologyReadyForReviewEvent event) {

        Set<Value> owners = getOwners(event.getOntologyIRI());
        Set<Value> authors = getAuthors(event.getOntologyIRI());
        return Map.of(OWNERS, owners, AUTHORS, authors);
    }

    protected Set<Value> getOwners(IRI ontology) {

        Repository repo = repositoryManager.getDefault();

        try (RepositoryConnection conn = repo.getConnection()) {
            return QueryResults.asModel(conn.getStatements(ontology, AssetVocabulary.HAS_OWNER, null)).objects();
        }
    }

    protected Set<Value> getAuthors(IRI ontology) {

        Repository repo = repositoryManager.getDefault();

        try (RepositoryConnection conn = repo.getConnection()) {
            return QueryResults.asModel(conn.getStatements(ontology, AssetVocabulary.HAS_AUTHOR, null)).objects();
        }
    }

    public static class Factory implements EventDecoratorFactory<OntologyReadyForReviewEvent> {

        @Inject
        private RepositoryManager repositoryManager;

        @Override
        public EventDecorator<OntologyReadyForReviewEvent> create() {
            return new OntologyReadyForReviewEventDecorator(repositoryManager);
        }

    }

}
