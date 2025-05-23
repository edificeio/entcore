package org.entcore.common.http.health;

import fr.wseduc.webutils.metrics.HealthCheckProbe;
import fr.wseduc.webutils.metrics.HealthCheckProbeResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;

import static io.vertx.core.Future.succeededFuture;

/**
 * Check that Postgresql is reachable and that a simple read query can be performed.
 */
public class PostgresProbe implements HealthCheckProbe {
  private Vertx vertx;

  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    this.vertx = vertx;
    return succeededFuture();
  }

  @Override
  public String getName() {
    return "postgres";
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    final Promise<HealthCheckProbeResult> promise = Promise.promise();
    Sql.getInstance().raw("SELECT count(*) FROM information_schema.tables", res -> {
      boolean ok = "ok".equals(res.body().getString("status"));
      promise.tryComplete(new HealthCheckProbeResult(getName(), ok, null));
    });
    return promise.future();
  }
}
