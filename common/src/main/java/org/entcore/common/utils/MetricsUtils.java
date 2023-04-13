package org.entcore.common.utils;

import io.micrometer.core.instrument.Timer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class MetricsUtils {
    private MetricsUtils() {}

    /**
     *
     * @param name Name of the field with the sla values.
     * @param config Metrics configuration
     * @return The list of time buckets for the SLA.
     */
    public static List<Duration> getSla(final String name, final JsonObject config) {
        return config.getJsonArray(name, new JsonArray()).stream()
                .mapToLong(long.class::cast)
                .sorted()
                .mapToObj(Duration::ofMillis)
                .collect(Collectors.toList());
    }

    /**
     * Add the sla to the timer builder or publish a default histogram.
     * @param timerBuilder Builder to populate
     * @param sla Sla to use
     * @param defaultMaximumExpectedValueInMs
     * @return the timerBuilder for fluency
     */
    public static Timer.Builder setTimerSla(
            final Timer.Builder timerBuilder,
            final List<Duration> sla,
            final long defaultMaximumExpectedValueInMs) {
        if(sla == null || sla.isEmpty()) {
            timerBuilder
                .publishPercentileHistogram()
                .maximumExpectedValue(Duration.ofMillis(defaultMaximumExpectedValueInMs));
        } else {
            timerBuilder.sla(sla.toArray(new Duration[0]));
        }
        return timerBuilder;
    }
}
