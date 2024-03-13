package org.entcore.audience.view.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.audience.view.service.ViewService;
import org.entcore.common.user.UserInfos;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ViewServiceImpl implements ViewService {

  private final ViewDao viewDao;

  public ViewServiceImpl(ViewDao viewDao) {
    this.viewDao = viewDao;
  }

  @Override
  public Future<Void> registerView(String module, String resourceType, String resourceId, UserInfos user) {
    return viewDao.registerView(module, resourceType, resourceId, user.getUserId(), user.getType());
  }

  @Override
  public Future<Map<String, Integer>> getViewCounters(String module, String resourceType, Set<String> resourceIds) {
    Promise<Map<String, Integer>> promise = Promise.promise();
    Map<String, Integer> viewCounters = new HashMap<>();
    viewDao.getCountersByResource(module, resourceType, resourceIds)
            .onSuccess(viewCountersByResource -> {
              resourceIds.forEach(resourceId -> viewCounters.put(resourceId, viewCountersByResource.getOrDefault(resourceId, 0)));
              promise.complete(viewCounters);
            })
            .onFailure(promise::fail);
    return promise.future();
  }

  @Override
  public Future<ResourceViewDetails> getViewDetails(String module, String resourceType, String resourceId) {
    return viewDao.getViewDetails(module, resourceType, resourceId);
  }

  @Override
  public Future<Void> mergeUserViews( String keptUserId, String deletedUserId) {
    return viewDao.mergeUserViews(keptUserId, deletedUserId);
  }

  @Override
  public Future<Void> deleteAllViewsOfResources(Set<String> resourceIds) {
    return viewDao.deleteAllViewsOfResources(resourceIds);
  }
}
