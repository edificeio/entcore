package org.entcore.common.resources;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.broker.api.dto.resources.ResourcesDeletedDTO;
import org.entcore.broker.api.publisher.BrokerPublisherFactory;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.broker.proxy.ResourceBrokerPublisher;
import org.entcore.common.user.RepositoryEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RepositoryEvents that publishes resource deletion events to the broker.
 * 
 * This class follows the proxy pattern to extend regular repository events with broker notifications
 * when users or groups are deleted, which affect resources.
 */
public class ResourceBrokerRepositoryEvents implements RepositoryEvents {
    private static final Logger log = LoggerFactory.getLogger(ResourceBrokerRepositoryEvents.class);

    /**
     * The delegate repository events handler
     */
    private final RepositoryEvents delegateEvents;
    
    /**
     * Publisher for sending resource events to the broker
     */
    private final ResourceBrokerPublisher resourcePublisher;
    
    /**
     * The resource type to use in notifications
     */
    private final String resourceType;

    /**
     * Creates a new repository events handler that publishes deletion events to the broker
     * 
     * @param delegateEvents The actual repository events implementation to delegate to
     * @param vertx Vertx instance for the broker publisher
     * @param application Application name for the broker subject (e.g., "blog", "exercizer")
     * @param resourceType Resource type for notification payload (can be same as application or more specific)
     */
    public ResourceBrokerRepositoryEvents(final RepositoryEvents delegateEvents,
                                          final Vertx vertx,
                                          final String application,
                                          final String resourceType) {
        this.delegateEvents = delegateEvents;
        this.resourceType = resourceType;
        this.resourcePublisher = BrokerPublisherFactory.create(
            ResourceBrokerPublisher.class, 
            vertx,
            new AddressParameter("application", application)
        );
    }

    /**
     * Alternative constructor when application and resourceType are the same
     * 
     * @param delegateEvents The actual repository events implementation to delegate to
     * @param vertx Vertx instance for the broker publisher
     * @param application Application name used both for broker subject and resource type
     */
    public ResourceBrokerRepositoryEvents(final RepositoryEvents delegateEvents,
                                         final Vertx vertx,
                                         final String application) {
        this(delegateEvents, vertx, application, application);
    }

    @Override
    public void deleteGroups(JsonArray groups) {
        this.deleteGroups(groups, (e) -> {});
    }

    @Override
    public void deleteGroups(JsonArray groups, Handler<List<ResourceChanges>> handler) {
        delegateEvents.deleteGroups(groups, changed -> {
            // Handle the original callback first
            handler.handle(changed);
            
            // Extract IDs of affected resources that were actually deleted
            final Set<String> deletedResourceIds = changed.stream()
                .filter(change -> change.deleted)
                .map(change -> change.id)
                .collect(Collectors.toSet());
            
            notifyResourceDeletion(deletedResourceIds, "group");
        });
    }

    @Override
    public void deleteUsers(JsonArray users) {
        this.deleteUsers(users, (e) -> {});
    }

    @Override
    public void deleteUsers(JsonArray users, Handler<List<ResourceChanges>> handler) {
        delegateEvents.deleteUsers(users, changed -> {
            // Handle the original callback first
            handler.handle(changed);
            
            // Extract IDs of resources that were actually deleted
            final Set<String> deletedResourceIds = changed.stream()
                .filter(change -> change.deleted)
                .map(change -> change.id)
                .collect(Collectors.toSet());
            
            notifyResourceDeletion(deletedResourceIds, "user");
        });
    }

    /**
     * Sends a notification about deleted resources via the broker
     * 
     * @param deletedResourceIds Set of resource IDs that were deleted
     * @param trigger What triggered the deletion ("user" or "group")
     */
    private void notifyResourceDeletion(Set<String> deletedResourceIds, String trigger) {
        if (deletedResourceIds.isEmpty()) {
            log.debug("No resources to notify for deletion");
            return;
        }
            
        log.debug("Publishing deletion notification for " + deletedResourceIds.size() + 
                " resources deleted by " + trigger + " deletion");
        
        // Notify resource deletion via broker
        final ResourcesDeletedDTO notification = new ResourcesDeletedDTO(
            new ArrayList<>(deletedResourceIds), 
            resourceType
        );
        
        resourcePublisher.notifyResourcesDeleted(notification)
            .onSuccess(v -> log.debug("Successfully published resource deletion for " + 
                    deletedResourceIds.size() + " resources"))
            .onFailure(err -> log.error("Failed to notify resource deletion: " + 
                    err.getMessage(), err));
    }

    // Delegate all other methods to the original implementation

    @Override
    public void exportResources(boolean exportDocuments, boolean exportSharedResources, String exportId, String userId, JsonArray groups, String exportPath,
                                String locale, String host, Handler<Boolean> handler) {
        delegateEvents.exportResources(exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
    }

    @Override
    public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId,
                                JsonArray groups, String exportPath, String locale, String host, Handler<Boolean> handler) {
        delegateEvents.exportResources(resourcesIds, exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
    }

    @Override
    public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
                                String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {
        delegateEvents.importResources(importId, userId, userLogin, userName, importPath, locale, host, forceImportAsDuplication, handler);
    }

    @Override
    public void usersClassesUpdated(JsonArray updates) {
        delegateEvents.usersClassesUpdated(updates);
    }

    @Override
    public void transition(JsonObject structure) {
        delegateEvents.transition(structure);
    }

    @Override
    public void mergeUsers(String keepedUserId, String deletedUserId) {
        delegateEvents.mergeUsers(keepedUserId, deletedUserId);
    }

    @Override
    public void removeShareGroups(JsonArray oldGroups) {
        delegateEvents.removeShareGroups(oldGroups);
    }

    @Override
    public void tenantsStructuresUpdated(JsonArray addedTenantsStructures, JsonArray deletedTenantsStructures) {
        delegateEvents.tenantsStructuresUpdated(addedTenantsStructures, deletedTenantsStructures);
    }

    @Override
    public void timetableImported(String uai) {
        delegateEvents.timetableImported(uai);
    }

    @Override
    public Optional<String> getMainRepositoryName() {
        return delegateEvents.getMainRepositoryName();
    }
}