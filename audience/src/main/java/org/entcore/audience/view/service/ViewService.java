package org.entcore.audience.view.service;

import io.vertx.core.Future;
import org.entcore.audience.view.model.ResourceViewCounter;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Set;

public interface ViewService {
  Future<Void> registerView(final String module, final String resourceType, final String resourceId,
                            final UserInfos user);

  Future<List<ResourceViewCounter>> getViewCounts(String module, String resourceType, Set<String> resourceIds, UserInfos user);


  Future<ResourceViewDetails> getViewDetails(final String module, final String resourceType,
                                             final String resourceId, final UserInfos user);
}
