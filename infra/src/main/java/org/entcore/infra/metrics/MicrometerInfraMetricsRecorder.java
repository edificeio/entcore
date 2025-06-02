package org.entcore.infra.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Supplies the metric 'ent.version' that contains a codified version of the ENT.
 * It takes the value of "ent-version" from the configuration and then applies the following transformation :
 * - only numbers, hyphens and dots are preserved
 * - each group of digits between hyphens and dots are grouped together, right-padded with 0 if needed
 *  - the groups are "concatenated" together and parsed to a long to get the exposed metric.
 *  The metric is tagged with the version as specified in the configuration.
 */
public class MicrometerInfraMetricsRecorder implements InfraMetricsRecorder {
  private static final Logger log = LoggerFactory.getLogger(MicrometerInfraMetricsRecorder.class);
  private static final int VERSION_PADDING = 2;
  public MicrometerInfraMetricsRecorder(final Vertx vertx) {
    final MeterRegistry registry = BackendRegistries.getDefaultNow();
    if(registry == null) {
      throw new IllegalStateException("micrometer.registries.empty");
    }

    getLiteralVersion(vertx).onSuccess(lVersion -> {
      String literalVersion = lVersion;
      final AtomicLong version = new AtomicLong();
      try {
        version.set(getEntVersion(literalVersion));
      } catch (Exception e) {
        log.error("An error occurred while creating the metrics to expose ent version");
        literalVersion = "error";
        version.set(-1);
      }
      Gauge.builder("ent.version", version::get)
        .tag("ent-version", literalVersion).register(registry);
    });
  }

  private Future<String> getLiteralVersion(Vertx vertx) {
    final Promise<String> promise = Promise.promise();
    String entVersion = vertx.getOrCreateContext().config().getString("ent-version");
    if(org.apache.commons.lang3.StringUtils.isEmpty(entVersion)) {
      vertx.sharedData().getAsyncMap("server").compose(server -> server.get("ent-version")).onSuccess(version ->
        promise.complete(StringUtils.isEmpty(entVersion) ? "na" : entVersion)
      ).onFailure(ex -> promise.fail(ex));
    } else {
      promise.complete(entVersion);
    }
    return promise.future();
  }

  private Long getEntVersion(final String entVersion) {
    final String cleanVersion = entVersion
      .replaceAll("[^\\d-\\.]","")
      .replaceAll("-", ".")
      .replaceAll("\\.{2,}", "");
    final String[] parts = cleanVersion.split("\\.");
    long version = 0;
    for(int i = 0; i < parts.length ; i++) {
      final String part = parts[i].trim();
      if(isNotEmpty(part)) {
        version += (long) (Integer.parseInt(part) * Math.pow(10, VERSION_PADDING * (parts.length - (i + 1))));
      }
    }
    return version;
  }
}
