# Example app for event subscribers

This app provides examples for event subscribers:
* *EmailEventSubscriber*
* *NotificationEventSubscriber*

For an overview of available events have a look at the [help page for events](https://help.metaphacts.com/resource/Help:Events).

## EmailEventSubscriber
This class is an example on how to react on [modeling workflow events](https://help.metaphacts.com/resource/Help:Events) and send emails based on these events.
Emails are sent through the platform's <code>com.metaphacts.services.email.EmailService</code>.

## NotificationEventSubscriber
This class is an example on how to react on [modeling workflow events](https://help.metaphacts.com/resource/Help:Events) and send notifications based on these events.
Notifications are sent through the platform's <code>com.metaphacts.services.notification.NotificationService</code>.