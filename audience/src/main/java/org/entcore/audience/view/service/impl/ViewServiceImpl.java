package org.entcore.audience.view.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.model.ResourceViewCounter;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.audience.view.service.ViewService;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  public Future<List<ResourceViewCounter>> getViewCounts(String module, String resourceType, Set<String> resourceIds) {
    Promise<List<ResourceViewCounter>> promise = Promise.promise();
    viewDao.getCountersByResource(module, resourceType, resourceIds)
            .onSuccess(viewCounters -> promise.complete(resourceIds.stream()
                    .map(resourceId -> new ResourceViewCounter(resourceId, viewCounters.getOrDefault(resourceId, 0))).collect(Collectors.toList())))
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
