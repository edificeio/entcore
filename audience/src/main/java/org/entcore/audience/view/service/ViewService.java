package org.entcore.audience.view.service;

import io.vertx.core.Future;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.common.user.UserInfos;

import java.util.Map;
import java.util.Set;

public interface ViewService {
  Future<Void> registerView(final String module, final String resourceType, final String resourceId,
                            final UserInfos user);

  Future<Map<String, Integer>> getViewCounters(String module, String resourceType, Set<String> resourceIds);


  Future<ResourceViewDetails> getViewDetails(final String module, final String resourceType,
                                             final String resourceId);

  Future<Void> mergeUserViews(String keptUserId, String deletedUserId);

  Future<Void> deleteAllViewsOfResources(Set<String> resourceIds);
}
