package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.common.audience.to.AudienceCheckRightRequestMessage;
import org.entcore.common.audience.to.AudienceCheckRightResponseMessage;
import org.entcore.common.user.UserInfos;

import java.util.HashSet;
import java.util.Set;

import static org.entcore.common.audience.AudienceHelper.getCheckRightsBusAddress;

public class EventBusAudienceAccessFilter implements AudienceAccessFilter {
  private final EventBus eventBus;

  public EventBusAudienceAccessFilter(final Vertx vertx) {
    this.eventBus = vertx.eventBus();
  }
  @Override
  public Future<Boolean> canAccess(final String module, final String resourceType,
                                   final UserInfos user, final Set<String> resourceIds) {
    final Promise<Boolean> promise = Promise.promise();
    eventBus.request(
        getCheckRightsBusAddress(module, resourceType),
        getMessage(user, module, resourceType, resourceIds),
        messageAsyncResult -> {
          if(messageAsyncResult.succeeded()) {
            final AudienceCheckRightResponseMessage response = Json.decodeValue((String) messageAsyncResult.result().body(), AudienceCheckRightResponseMessage.class);
            if(response.isSuccess()) {
              promise.complete(response.isAccess());
            } else {
              promise.fail(response.getErrorMsg());
            }
          } else {
            promise.fail(messageAsyncResult.cause());
          }
        }
    );
    return promise.future();
  }
  public static String getMessage(final UserInfos user, final String appName, final String resourceType,
                                  final Set<String> resourceIds) {
    return Json.encode(new AudienceCheckRightRequestMessage(appName, resourceType, user.getUserId(), new HashSet<>(user.getGroupsIds()), resourceIds));
  }
}
