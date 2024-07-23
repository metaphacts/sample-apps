/*
 * Copyright (C) 2015-2020, metaphacts GmbH
 */
package com.metaphacts.example.app.event;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.metaphacts.repository.RepositoryManager;
import com.metaphacts.services.email.EmailService;
import com.metaphacts.services.email.EmailUtils;
import com.metaphacts.services.email.SendEmailRequest;
import com.metaphacts.services.email.SendEmailRequest.Message;
import com.metaphacts.services.ontologies.OntologyEvent;
import com.metaphacts.services.ontologies.OntologyEvent.OntologyReadyForReviewEvent;
import com.metaphacts.services.ontologies.OntologyEvent.OntologyReviewerUpdatedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

/**
 * Example subscriber for {@link OntologyEvent}s which uses the event's information to send emails via
 * {@link SesEmailService}.
 */
@ApplicationScoped
public class EmailEventSubscriber {

    private static final Logger logger = LogManager.getLogger(EmailEventSubscriber.class);

    @Inject
    private EmailService emailService;

    @Inject
    private RepositoryManager repositoryManager;

    /**
     * Handles the update of reviewers on an ontology (CDI event subscription).
     * 
     * @param event Event of type {@link OntologyReviewerUpdatedEvent} injected by the CDI event mechanism
     */
    public void handleUpdatedReviewers(@ObservesAsync OntologyReviewerUpdatedEvent event) {
        logger.debug("Handling event {}", event.getEventType());
        sendEmailToReviewers(event.getOntologyIRI(), event.getUserId(), event.getAddedReviewers());
    }

    /**
     * Handles the status update of a review request on an ontology (CDI event subscription).
     * 
     * @param event Event of type {@link OntologyReadyForReviewEvent} injected by the CDI event mechanism
     */
    public void handleRequestReview(@ObservesAsync OntologyReadyForReviewEvent event) {
        logger.debug("Handling event {}", event.getEventType());
        sendEmailToReviewers(event.getOntologyIRI(), event.getUserId(), event.getReviewers());
    }

    /**
     * Sends emails to all given reviewers whose username is a valid email address.
     * 
     * @param ontologyIri {@link IRI} of the ontology reviewers have been added to
     * @param userId      Id of the user who triggered the event
     * @param reviewers   {@link IRI}s of users who were added as reviewers of the ontology
     */
    private void sendEmailToReviewers(IRI ontologyIri, String userId, Set<IRI> reviewers) {
        Set<String> reviewersSet = reviewers.stream()
                .map(this::decodeUserIri)
                .filter(EmailUtils::isValidEmailAddress)
                .collect(Collectors.toSet());

        if (!reviewersSet.isEmpty()) {
            logger.debug("Sending email(s) to the following reviewer(s): {}",
                    reviewersSet.stream().collect(Collectors.joining(", ")));

            String msg = "Your review on ontology '%s' has been requested by %s."
                    .formatted(getOntologyLabel(ontologyIri), userId);

            SendEmailRequest request = new SendEmailRequest("Review requested",
                    new Message().withHtml(msg),
                    reviewersSet);

            try {
                emailService.sendEmail(request);
            } catch (Exception e) {
                logger.warn("Failed to send email to {}: {}", reviewersSet.stream().collect(Collectors.joining(", ")),
                        e.getMessage());
                logger.debug("Details: ", e);
            }
        } else {
            logger.debug("No emails have been sent as there were no reviewers "
                    + "with valid email addresses in the received event");
        }
    }

    /**
     * Extract the username from the userIri.
     * 
     * @param userIri {@link IRI} of a user
     * @return the decoded username extracted from the given userIri
     */
    private String decodeUserIri(IRI userIri) {
        return URLDecoder.decode(userIri.getLocalName(), StandardCharsets.UTF_8);
    }

    /**
     * Get the label of an ontology from the default repository.
     * 
     * @param ontologyIri {@link IRI} of the ontology
     * @return the label (dcterms:title or rdfs:label) of the given ontology or the {@link IRI} of the ontology as
     *         {@link String} if no label exists
     */
    private String getOntologyLabel(IRI ontologyIri) {
        Optional<String> label;
        try (RepositoryConnection con = repositoryManager.getDefault().getConnection()) {
            // Try to get dcterms:title from the ontology
            Model model = QueryResults.asModel(con.getStatements(ontologyIri, DCTERMS.TITLE, null));
            if (model.isEmpty()) { // Use rdfs:label as fallback
                model = QueryResults.asModel(con.getStatements(ontologyIri, RDFS.LABEL, null));
            }
            label = Models.objectString(model);
        }
        return label.orElseGet(() -> ontologyIri.toString());
    }

}
