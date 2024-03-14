package org.entcore.audience.view.dao;

import io.vertx.core.Future;
import org.entcore.audience.view.model.ResourceViewDetails;

import java.util.Map;
import java.util.Set;

public interface ViewDao {
  Future<Void> registerView(String module, String resourceType, String resourceId, String userId, String type);

  Future<Map<String, Integer>> getCountersByResource(String module, String resourceType, Set<String> resourceIds);

  Future<ResourceViewDetails> getViewDetails(final String module, final String resourceType, final String resourceId);

  Future<Void> mergeUserViews(String keptUserId, String deletedUserId);

  Future<Void> deleteAllViewsOfResources(Set<String> resourceIds);
}
