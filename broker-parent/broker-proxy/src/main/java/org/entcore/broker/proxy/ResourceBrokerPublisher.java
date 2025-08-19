package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerPublisher;
import org.entcore.broker.api.dto.resources.ResourcesDeletedDTO;
import org.entcore.broker.api.dto.resources.ResourceSharesChangedDTO;

/**
 * Interface for publishing resource-related events to the broker.
 * Applications can implement this interface to notify other applications
 * about changes to resources.
 * 
 * This interface requires the 'application' parameter to be set when creating an instance:
 * BrokerPublisherFactory.create(ResourceBrokerPublisher.class, vertx, Map.of("application", "blog"))
 */
public interface ResourceBrokerPublisher {
    
    /**
     * Notifies subscribers that resources have been deleted.
     * 
     * @param notification DTO containing the IDs of deleted resources and their application
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "resource.{application}.deleted")
    Future<Void> notifyResourcesDeleted(ResourcesDeletedDTO notification);
    
    /**
     * Notifies subscribers that the shares for resources have changed.
     * 
     * @param notification DTO containing the resource IDs, application ID, and new shares information
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "resource.{application}.shares.changed")
    Future<Void> notifyResourceSharesChanged(ResourceSharesChangedDTO notification);
}