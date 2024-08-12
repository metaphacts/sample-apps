# Example app for event subscribers

This app provides examples for event subscribers:
* *EmailEventSubscriber*
* *NotificationEventSubscriber*

For an overview of available events have a look at the [help page for events](https://help.metaphacts.com/resource/Help:Events).

## EmailEventSubscriber
This class is an example on how to react on [modeling workflow events](https://help.metaphacts.com/resource/Help:Events) and send emails based on these events.

The example listener reacts on `OntologyReviewerUpdatedEvent` and `OntologyReadyForReviewEvent` events, emails are sent through the platform's `com.metaphacts.services.email.EmailService`.

The ontology's reviewer names are used as email addresses for recipients when they represent email addresses.

A custom extension could make this extensible, e.g., by adding a custom configuration for a common target email address.

## NotificationEventSubscriber
This class is an example on how to react on [modeling workflow events](https://help.metaphacts.com/resource/Help:Events) and send notifications based on these events.
The example listener reacts on `OntologyCreatedEvent`, `OntologyDeletedEvent`, `VocabularyCreatedEvent`, and `VocabularyDeletedEvent` events, notifications are sent through the platform's `com.metaphacts.services.notification.NotificationService`.

The notifications are sent to the default topic as configured in the `NotificationService` configuration.

A custom extension could make this extensible, e.g., to allow configuration for of the topic or to derive the topic name from some property of the ontology or vocabulary.

## Configuration & Prerequisites

Please note that in order to be able to actually send emails or notifications, this app needs an implementation of the `EmailService` or `NotificationService`. metaphactory provides the bundled app `aws-services` which can be used to send emails or notifications using AWS SES or SNS, respectively. See [AWS Services App](https://help.metaphacts.com/resource/Help:Events#aws-services-installation) for details.