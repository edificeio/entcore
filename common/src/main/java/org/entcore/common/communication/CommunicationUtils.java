package org.entcore.common.communication;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing communication links between groups and users
 * Provides methods to add and remove communication links via event bus
 */
public class CommunicationUtils {
    private static final Logger log = LoggerFactory.getLogger(CommunicationUtils.class);
    private static final String COMMUNICATION_ADDRESS = "wse.communication";

    /**
     * Direction of communication links
     */
    public enum Direction {
        INCOMING,
        OUTGOING,
        BOTH,
        NONE;
        
        @Override
        public String toString() {
            return name();
        }
    }

    /**
     * Creates a communication link between two groups via event bus
     * This allows the members of each group to communicate with the members of the other group
     *
     * @param eb The event bus instance
     * @param startGroupId The ID of the source group
     * @param endGroupId The ID of the target group
     * @return A Future with the operation result
     */
    public static Future<String> addLinkBetweenGroups(EventBus eb, String startGroupId, String endGroupId) {
        if (eb == null) {
            log.warn("Event bus not initialized, skipping communication link setup");
            return Future.succeededFuture(startGroupId);
        }
        
        Promise<String> promise = Promise.promise();
        
        // Create bus message to add communication link between groups
        JsonObject message = new JsonObject()
                .put("action", "addLink")
                .put("startGroupId", startGroupId)
                .put("endGroupId", endGroupId);
        
        eb.request(COMMUNICATION_ADDRESS, message, reply -> {
            if (reply.succeeded()) {
                JsonObject result = (JsonObject) reply.result().body();
                if ("error".equals(result.getString("status"))) {
                    // Log error but don't fail the overall operation
                    log.warn("Failed to create communication link between groups {} and {}: {}", 
                            startGroupId, endGroupId, result.getString("message"));
                } else {
                    log.debug("Successfully created communication link between groups {} and {}", 
                            startGroupId, endGroupId);
                }
            } else {
                // Log error but don't fail the overall operation
                log.warn("Error creating communication link between groups {} and {}: {}", 
                        startGroupId, endGroupId, reply.cause().getMessage());
            }
            // Always complete with startGroupId to continue chain
            promise.complete(startGroupId);
        });
        
        return promise.future();
    }

    /**
     * Adds communication links between a group and its users via event bus
     * Typically used to enable communication from group to users (INCOMING)
     * or between users and group (BOTH)
     *
     * @param eb The event bus instance
     * @param groupId The ID of the group
     * @param direction The communication direction to set
     * @return A Future with the operation result
     */
    public static Future<JsonObject> addCommunicationLinks(EventBus eb, String groupId, Direction direction) {
        Promise<JsonObject> promise = Promise.promise();
        
        if (eb == null) {
            log.warn("Event bus not initialized, skipping communication setup for group {}", groupId);
            return Future.succeededFuture(new JsonObject().put("skipped", true));
        }
        
        JsonObject message = new JsonObject()
                .put("action", "addLinkWithUsers")
                .put("groupId", groupId)
                .put("direction", direction.toString());
        
        eb.request(COMMUNICATION_ADDRESS, message, reply -> {
            if (reply.succeeded()) {
                JsonObject result = (JsonObject) reply.result().body();
                if ("error".equals(result.getString("status"))) {
                    log.error("Failed to add communication links for group {}: {}", 
                            groupId, result.getString("message"));
                    promise.fail(result.getString("message"));
                } else {
                    promise.complete(result);
                }
            } else {
                log.error("Error calling communication bus service for group {}: {}", 
                        groupId, reply.cause().getMessage());
                promise.fail(reply.cause().getMessage());
            }
        });
        
        return promise.future();
    }

    /**
     * Removes communication links between a group and its users via event bus
     *
     * @param eb The event bus instance
     * @param groupId The ID of the group
     * @param direction The communication direction to remove
     * @return A Future with the operation result
     */
    public static Future<JsonObject> removeCommunicationLinks(EventBus eb, String groupId, Direction direction) {
        Promise<JsonObject> promise = Promise.promise();
        
        if (eb == null) {
            log.warn("Event bus not initialized, skipping communication removal for group {}", groupId);
            return Future.succeededFuture(new JsonObject().put("skipped", true));
        }
        
        JsonObject message = new JsonObject()
                .put("action", "removeLinkWithUsers")
                .put("groupId", groupId)
                .put("direction", direction.toString());
        
        eb.request(COMMUNICATION_ADDRESS, message, reply -> {
            if (reply.succeeded()) {
                JsonObject result = (JsonObject) reply.result().body();
                if ("error".equals(result.getString("status"))) {
                    log.error("Failed to remove communication links for group {}: {}", 
                            groupId, result.getString("message"));
                    promise.fail(result.getString("message"));
                } else {
                    promise.complete(result);
                }
            } else {
                log.error("Error calling communication bus service for group {}: {}", 
                        groupId, reply.cause().getMessage());
                promise.fail(reply.cause().getMessage());
            }
        });
        
        return promise.future();
    }
}