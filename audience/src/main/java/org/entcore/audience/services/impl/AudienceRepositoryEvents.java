package org.entcore.audience.services.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.entcore.audience.services.AudienceService;
import org.entcore.common.user.RepositoryEvents;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handle clean up of audience data when users are deleted or merged.
 */
public class AudienceRepositoryEvents implements RepositoryEvents {
  private static final Logger log = LoggerFactory.getLogger(AudienceRepositoryEvents.class);

  private final AudienceService audienceService;

  public AudienceRepositoryEvents(final AudienceService audienceService) {
    this.audienceService = audienceService;
  }


  @Override
  public void deleteUsers(final JsonArray users) {
    final Set<String> userIds;
    if(users == null || users.isEmpty()) {
      userIds = Collections.emptySet();
    } else {
      userIds = users.stream().map(u -> ((JsonObject)u).getString("id"))
          .filter(StringUtils::isNotEmpty)
          .collect(Collectors.toSet());
    }
    log.info("Calling purge of users : " + userIds);
    audienceService.deleteUsers(userIds)
        .onSuccess(e -> log.info("Users have been successfully deleted from audience : " + userIds))
        .onFailure(th -> log.warn("An error occurred while deleting users from audience : " + userIds, th));
  }

  @Override
  public void mergeUsers(String keptdUserId, String deletedUserId) {
    log.info("Merge users " + deletedUserId + " -> " + keptdUserId);
    audienceService.mergeUsers(keptdUserId, deletedUserId)
        .onSuccess(e -> log.info("Users have been successfully merged"))
        .onFailure(th -> log.warn("An error occurred while merging users from audience", th));
  }


}
