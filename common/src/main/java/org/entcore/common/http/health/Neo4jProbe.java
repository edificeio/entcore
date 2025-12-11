package org.entcore.common.http.health;

import fr.wseduc.webutils.metrics.HealthCheckProbe;
import fr.wseduc.webutils.metrics.HealthCheckProbeResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;

import static io.vertx.core.Future.succeededFuture;

/**
 * Checks that Neo4j is reachable and that a simple read query can be executed.
 */
public class Neo4jProbe implements HealthCheckProbe {
  private Vertx vertx;

  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    this.vertx = vertx;
    return succeededFuture();
  }

  @Override
  public String getName() {
    return "neo4j";
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    final Promise<HealthCheckProbeResult> promise = Promise.promise();
    Neo4j.getInstance().execute("MATCH (:Structure) RETURN count(*)", (JsonObject) null, res -> {
      boolean ok = "ok".equals(res.body().getString("status"));
      promise.tryComplete(new HealthCheckProbeResult(getName(), ok, null));
    });
    return promise.future();
  }
}
