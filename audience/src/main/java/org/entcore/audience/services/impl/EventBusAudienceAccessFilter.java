package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.audience.to.AudienceCheckRightRequestMessage;
import java.util.Set;

import static org.entcore.common.audience.AudienceHelper.getCheckRightsBusAddress;

public class EventBusAudienceAccessFilter implements AudienceAccessFilter {
  private final EventBus eventBus;

  public EventBusAudienceAccessFilter(final Vertx vertx) {
    this.eventBus = vertx.eventBus();
  }
  @Override
  public Future<Boolean> canAccess(final String appName, final String resourceType,
                                   final UserInfos user, final Set<String> resourceIds) {
    final Promise<Boolean> promise = Promise.promise();
    eventBus.request(
        getCheckRightsBusAddress(appName, resourceType),
        getMessage(user, appName, resourceType, resourceIds),
        messageAsyncResult -> {
          if(messageAsyncResult.succeeded()) {
            final Boolean response = (Boolean) messageAsyncResult.result().body();
            promise.complete(response);
          } else {
            promise.fail(messageAsyncResult.cause());
          }
        }
    );
    return promise.future();
  }
  public static String getMessage(final UserInfos user, final String appName, final String resourceType,
                                  final Set<String> resourceIds) {
    return Json.encode(new AudienceCheckRightRequestMessage(appName, resourceType, user.getUserId(), resourceIds));
  }
}
