package org.entcore.directory.health;

import fr.wseduc.webutils.metrics.HealthCheckProbe;
import fr.wseduc.webutils.metrics.HealthCheckProbeResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.entcore.directory.Directory;

import static io.vertx.core.Future.succeededFuture;

/**
 * Readiness probe checking that the feeder answers on the event bus and is not stuck in an import.
 * <p>
 * Sends the {@code ping} action to the feeder, which answers without queueing. The probe is KO when:
 * <ul>
 *   <li>no feeder consumer is registered, or it does not answer within {@code timeout} ms;</li>
 *   <li>an import has been holding the feeder for more than {@code max-import-duration} ms. An exception thrown
 *   inside an import callback (e.g. an {@code OutOfMemoryError}) leaves the importer busy forever: every later
 *   action (AAF, AAF1D, EDT imports…) is then queued and never processed while the consumer still exists.</li>
 * </ul>
 * Configuration (all optional), in the directory {@code probes} list:
 * <pre>{"name": "org.entcore.directory.health.FeederProbe",
 *  "config": {"address": "entcore.feeder", "timeout": 5000, "max-import-duration": 7200000}}</pre>
 */
public class FeederProbe implements HealthCheckProbe {

	/** Default maximum duration of an import before the feeder is considered stuck: 2 hours. */
	private static final long DEFAULT_MAX_IMPORT_DURATION = 2 * 60 * 60 * 1000L;
	/** Default event bus reply timeout, below the default probes timeout (10 s). */
	private static final long DEFAULT_TIMEOUT = 5000L;

	private Vertx vertx;
	private String address;
	private long timeout;
	private long maxImportDuration;

	@Override
	public Future<Void> init(final Vertx vertx, final JsonObject config) {
		final JsonObject conf = config == null ? new JsonObject() : config;
		this.vertx = vertx;
		this.address = conf.getString("address", Directory.FEEDER);
		this.timeout = conf.getLong("timeout", DEFAULT_TIMEOUT);
		this.maxImportDuration = conf.getLong("max-import-duration", DEFAULT_MAX_IMPORT_DURATION);
		return succeededFuture();
	}

	@Override
	public String getName() {
		return "feeder";
	}

	@Override
	public Vertx getVertx() {
		return vertx;
	}

	@Override
	public Future<HealthCheckProbeResult> probe() {
		return vertx.eventBus()
				.<JsonObject>request(address, new JsonObject().put("action", "ping"),
						new DeliveryOptions().setSendTimeout(timeout))
				.map(reply -> evaluate(reply.body()))
				.otherwise(err -> new HealthCheckProbeResult(getName(), false,
						new JsonObject().put("error", String.valueOf(err.getMessage()))));
	}

	private HealthCheckProbeResult evaluate(final JsonObject body) {
		if (body == null || !"ok".equals(body.getString("status"))) {
			return new HealthCheckProbeResult(getName(), false, body);
		}
		final long importDuration = body.getLong("importDuration", 0L);
		final boolean stuck = importDuration > maxImportDuration;
		final JsonObject metadata = body.copy();
		metadata.remove("status");
		if (stuck) {
			metadata.put("error", "import.running.for.too.long");
		}
		return new HealthCheckProbeResult(getName(), !stuck, metadata);
	}
}
