package org.entcore.audience.view.service.impl;

import io.vertx.core.Future;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.model.ResourceViewCounter;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.audience.view.service.ViewService;
import org.entcore.common.user.UserInfos;

import java.util.List;
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
  public Future<List<ResourceViewCounter>> getViewCounts(String module, String resourceType, Set<String> resourceIds) {
    return viewDao.getCounts(module, resourceType, resourceIds);
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
