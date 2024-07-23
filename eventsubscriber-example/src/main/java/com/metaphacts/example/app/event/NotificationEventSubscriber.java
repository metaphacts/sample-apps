/*
 * Copyright (C) 2015-2020, metaphacts GmbH
 */
package com.metaphacts.example.app.event;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.metaphacts.repository.RepositoryManager;
import com.metaphacts.services.notification.NotificationService;
import com.metaphacts.services.notification.SendNotificationRequest;
import com.metaphacts.services.ontologies.OntologyEvent;
import com.metaphacts.services.ontologies.OntologyEvent.OntologyCreatedEvent;
import com.metaphacts.services.ontologies.OntologyEvent.OntologyDeletedEvent;
import com.metaphacts.services.vocabularies.VocabularyEvent;
import com.metaphacts.services.vocabularies.VocabularyEvent.VocabularyCreatedEvent;
import com.metaphacts.services.vocabularies.VocabularyEvent.VocabularyDeletedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

/**
 * Example subscriber to {@link OntologyEvent}s and {@link VocabularyEvent}s which use the event's information to send
 * notifications via {@link SnsNotificationService}.
 */
@ApplicationScoped
public class NotificationEventSubscriber {

    private static final Logger logger = LogManager.getLogger(NotificationEventSubscriber.class);
    private static final String targetArn = null; // arn:aws:sns:<region>:<IAM-id>:<topic-id>

    @Inject
    private NotificationService notificationService;

    @Inject
    private RepositoryManager repositoryManager;

    /**
     * Handles the creation of an ontology (CDI event subscription).
     * 
     * @param event Event of type {@link OntologyCreatedEvent} injected by the CDI event mechanism
     */
    public void handleOntologyCreated(@ObservesAsync OntologyCreatedEvent event) {
        logger.debug("Received {}", event.getEventType());

        String subject = "Ontology has been created";
        String msg = "Ontology '%s' has been created by user '%s'".formatted(getAssetLabel(event.getOntologyIRI()),
                event.getUserId());
        SendNotificationRequest request = new SendNotificationRequest(subject, msg, targetArn);

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            logger.warn("Failed to send notification: {}", e.getMessage());
            logger.debug("Details: ", e);
        }
    }

    /**
     * Handles the deletion of an ontology (CDI event subscription).
     * 
     * @param event Event of type {@link OntologyDeletedEvent} injected by the CDI event mechanism
     */
    public void handleOntologyDeleted(@ObservesAsync OntologyDeletedEvent event) {
        logger.debug("Received {}", event.getEventType());

        String subject = "Ontology has been deleted";
        String msg = "Ontology '%s' has been deleted by user '%s'".formatted(getAssetLabel(event.getOntologyIRI()),
                event.getUserId());
        SendNotificationRequest request = new SendNotificationRequest(subject, msg, targetArn);
        
        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            logger.warn("Failed to send notification: {}", e.getMessage());
            logger.debug("Details: ", e);
        }
    }

    /**
     * Handles the creation of a vocabulary (CDI event subscription).
     * 
     * @param event Event of type {@link VocabularyCreatedEvent} injected by the CDI event mechanism
     */
    public void handleVocabularyCreated(@ObservesAsync VocabularyCreatedEvent event) {
        logger.debug("Received {}", event.getEventType());

        String subject = "Vocabulary has been created";
        String msg = "Vocabulary '%s' has been created by user '%s'".formatted(getAssetLabel(event.getVocabularyIRI()),
                event.getUserId());
        SendNotificationRequest request = new SendNotificationRequest(subject, msg, targetArn);

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            logger.warn("Failed to send notification: {}", e.getMessage());
            logger.debug("Details: ", e);
        }
    }

    /**
     * Handles the deletion of a vocabulary (CDI event subscription).
     * 
     * @param event Event of type {@link VocabularyDeletedEvent} injected by the CDI event mechanism
     */
    public void handleVocabularyDeleted(@ObservesAsync VocabularyDeletedEvent event) {
        logger.debug("Received {}", event.getEventType());

        String subject = "Vocabulary has been deleted";
        String msg = "Vocabulary '%s' has been deleted by user '%s'".formatted(getAssetLabel(event.getVocabularyIRI()),
                event.getUserId());
        SendNotificationRequest request = new SendNotificationRequest(subject, msg, targetArn);

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            logger.warn("Failed to send notification: {}", e.getMessage());
            logger.debug("Details: ", e);
        }
    }

    /**
     * Get the label of an asset from the default repository.
     * 
     * @param assetIri {@link IRI} of the asset
     * @return the label (dcterms:title or rdfs:label) of the given asset or the {@link IRI} of the asset as
     *         {@link String} if no label exists
     */
    private String getAssetLabel(IRI assetIri) {
        Optional<String> label;
        try (RepositoryConnection con = repositoryManager.getDefault().getConnection()) {
            // Try to get dcterms:title from the asset
            Model model = QueryResults.asModel(con.getStatements(assetIri, DC.TITLE, null));
            if (model.isEmpty()) { // Use rdfs:label as fallback
                model = QueryResults.asModel(con.getStatements(assetIri, RDFS.LABEL, null));
            }
            label = Models.objectString(model);
        }
        return label.orElseGet(() -> assetIri.toString());
    }

}
