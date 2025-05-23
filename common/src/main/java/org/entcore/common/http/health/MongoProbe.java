package org.entcore.common.http.health;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.metrics.HealthCheckProbe;
import fr.wseduc.webutils.metrics.HealthCheckProbeResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.Future.succeededFuture;

/**
 * Checks that MongoDB is reachable and that a simple read query can be performed.
 */
public class MongoProbe implements HealthCheckProbe {
  private Vertx vertx;

  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    this.vertx = vertx;
    return succeededFuture();
  }

  @Override
  public String getName() {
    return "mongo";
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    final Promise<HealthCheckProbeResult> promise = Promise.promise();
    MongoDb.getInstance().command("{ \"dbStats\": 1 }",res -> {
      boolean ok = "ok".equals(res.body().getString("status"));
      promise.tryComplete(new HealthCheckProbeResult(getName(), ok, null));
    });
    return promise.future();
  }
}
