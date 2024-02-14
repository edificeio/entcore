package org.entcore.audience.services.impl;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.user.RepositoryEvents;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 
 */
public class AudienceRepositoryEvents implements RepositoryEvents {
  private static final Logger log = LoggerFactory.getLogger(AudienceRepositoryEvents.class);

  @Override
  public void deleteUsers(JsonArray users, Handler<List<ResourceChanges>> handler) {
    final Set<String> userIds = getUserIds(users);
    log.info("Calling purge of users : " + userIds);
    // TODO delete entries in reactions
    throw new UnsupportedOperationException("implement.deleteUsers");
  }

  @Override
  public void mergeUsers(String keepedUserId, String deletedUserId) {
    // TODO replace in reactions and views deletedUserId by  keepUserId
    throw new UnsupportedOperationException("implement.mergeUsers");
  }

  @Override
  public void removeShareGroups(JsonArray oldGroups) {
    // TODO remove users' reaction on concerned resources
    throw new UnsupportedOperationException("implement.removeShareGroups");
  }

  private Set<String> getUserIds(JsonArray users) {
    final Set<String> userIds;
    if(users == null || users.isEmpty()) {
      userIds = Collections.emptySet();
    } else {
      userIds = users.stream().map(u -> ((JsonObject)u).getString("id"))
          .filter(StringUtils::isNotEmpty)
          .collect(Collectors.toSet());
    }
    return userIds;
  }

}
