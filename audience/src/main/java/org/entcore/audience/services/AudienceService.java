package org.entcore.audience.services;

import io.vertx.core.Future;

import java.util.Set;

public interface AudienceService {
  /**
   * Purge audience data related to the supplied users.
   * @param userIds Ids of the users who have been deleted
   * @return a Future that completes when the deletion is done
   */
  Future<Void> deleteUsers(final Set<String> userIds);

  /**
   * Replace all references to {@code deletedUserId} by references to {@code keptdUserId}
   * @param keptUserId Id of the user to keep
   * @param deletedUserId Id of the user who has been merged into the other one
   * @return a Future that completes when the deletion is done
   */
  Future<Void> mergeUsers(final String keptUserId, final String deletedUserId);

  /**
   * Purge audiences data (reaction, views) for resources deletion
   * @param module the module of resources being deleted
   * @param resourceType the resource type of resources being deleted
   * @param resourceIds the list of resource ids being deleted
   * @return a Future that completes when the deletion is done
   */
  Future<Void> purgeDeletedResources(final String module, final String resourceType, final Set<String> resourceIds);
}
