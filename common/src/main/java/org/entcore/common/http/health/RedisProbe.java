package org.entcore.common.http.health;

import fr.wseduc.webutils.metrics.HealthCheckProbe;
import fr.wseduc.webutils.metrics.HealthCheckProbeResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;
import org.entcore.common.redis.Redis;

import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Arrays.asList;

/**
 * Check that Redis is reachable and that a simple read query can be performed.
 */
public class RedisProbe implements HealthCheckProbe {
  private Vertx vertx;

  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    this.vertx = vertx;
    return succeededFuture();
  }

  @Override
  public String getName() {
    return "redis";
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    final RedisAPI client = Redis.getClient().getClient();
    final String key = "probe-ent-" + UUID.randomUUID();
    return client.set(asList(key, "healthcheck", "EX", "1"))
      .map(e -> new HealthCheckProbeResult(getName(), true, null))
      .otherwise(th -> new HealthCheckProbeResult(getName(), false, new JsonObject().put("error", th.getMessage())));
  }
}
