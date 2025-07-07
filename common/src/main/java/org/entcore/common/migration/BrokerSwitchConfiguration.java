package org.entcore.common.migration;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

public interface BrokerSwitchConfiguration {
  String LEGACY_MIGRATION_ADDRESS = "broker.proxy.legacy.migration";


  static <T> Future<T> sendToBroker(final String action, final String service, Object request, Class<T> responseType, final EventBus eventBus) {
    final Promise<T> promise = Promise.promise();
    final JsonObject payload = new JsonObject()
      .put("action", action)
      .put("service", service)
      .put("params", JsonObject.mapFrom(request));
    eventBus.request(BrokerSwitchConfiguration.LEGACY_MIGRATION_ADDRESS, payload, reply -> {
      if (reply.succeeded()) {
        promise.tryComplete(StringUtils.parseJson((String) reply.result().body(), responseType));
      } else {
        promise.tryFail(reply.cause());
      }
    });
    return promise.future();
  }
}
