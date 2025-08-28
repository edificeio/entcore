package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerPublisher;
import org.entcore.broker.api.dto.applications.ApplicationStatusDTO;

/**
 * Interface for publishing application status events to the broker.
 * Services can implement this interface to notify other applications
 * about changes in application status (started, stopped, etc.)
 * <p>
 * This interface requires the 'application' parameter to be set when creating an instance:
 * BrokerPublisherFactory.create(ApplicationStatusBrokerPublisher.class, vertx, AddressParameter("application", "blog"))
 */
public interface ApplicationStatusBrokerPublisher {

    /**
     * Publishes a notification that the application has started.
     * Uses the 'application' parameter passed during creation to route the notification.
     *
     * @param status Application startup status information
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "application.{application}.start")
    Future<Void> notifyStarted(ApplicationStatusDTO status);

    /**
     * Publishes a notification that the application has stopped.
     * Uses the 'application' parameter passed during creation to route the notification.
     *
     * @param status Application stop status information
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "application.{application}.stop")
    Future<Void> notifyStopped(ApplicationStatusDTO status);
}