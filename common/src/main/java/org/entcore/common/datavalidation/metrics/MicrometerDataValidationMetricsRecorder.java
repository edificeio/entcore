package org.entcore.common.datavalidation.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;

public class MicrometerDataValidationMetricsRecorder implements DataValidationMetricsRecorder {
    private final Counter numOfEmailValidationCodesSent;
    private final Counter numOfMobileValidationCodesSent;
    private final Counter numOfMfaCodesSent;

    private final Counter numOfEmailValidationCodesConsumed;
    private final Counter numOfMobileValidationCodesConsumed;
    private final Counter numOfMfaCodesConsumed;

    public MicrometerDataValidationMetricsRecorder(final Configuration configuration) {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry == null) {
            throw new IllegalStateException("micrometer.registries.empty");
        }
        numOfEmailValidationCodesSent = Counter.builder("datavalidation.code.email.sent")
            .description("number of generated email validation codes")
            .register(registry);
        numOfMobileValidationCodesSent = Counter.builder("datavalidation.code.mobile.sent")
            .description("number of generated mobile validation codes")
            .register(registry);
        numOfMfaCodesSent = Counter.builder("mfa.code.sent")
            .description("number of generated mfa (email or sms) codes")
            .register(registry);
        numOfEmailValidationCodesConsumed = Counter.builder("datavalidation.code.email.consumed")
            .description("number of generated email validation codes")
            .register(registry);
        numOfMobileValidationCodesConsumed = Counter.builder("datavalidation.code.mobile.consumed")
            .description("number of generated mobile validation codes")
            .register(registry);
        numOfMfaCodesConsumed = Counter.builder("mfa.code.consumed")
            .description("number of generated mfa (email or sms) codes")
            .register(registry);
    }

    @Override public void onEmailCodeGenerated() {
        numOfEmailValidationCodesSent.increment(1);
    }
    @Override public void onMobileCodeGenerated() {
        numOfMobileValidationCodesSent.increment(1);
    };

    @Override public void onMfaCodeGenerated() {
        numOfMfaCodesSent.increment(1);
    };

    @Override public void onEmailCodeConsumed() {
        numOfEmailValidationCodesConsumed.increment(1);
    };

    @Override public void onMobileCodeConsumed() {
        numOfMobileValidationCodesConsumed.increment(1);
    };

    @Override public void onMfaCodeConsumed() {
        numOfMfaCodesConsumed.increment(1);
    };

    public static class Configuration {
        private final List<Duration> sla;

        public Configuration(List<Duration> sla) {
            this.sla = sla;
        }

        /**
         * <p>Creates the configuration of the metrics recorder based on the global configuration file.</p>
         * <p>
         *     It expects that the configuration contains a property <strong>metrics</strong> that contains the
         *     following fields :
         *     <ul>
         *         <li>sla, the desired buckets (in milliseconds) for the time of locks lifespan</li>
         *     </ul>
         * </p>
         * @param conf
         * @return
         */
        public static Configuration fromJson(final JsonObject conf) {
            final List<Duration> sla;
            if(conf == null || !conf.containsKey("metrics")) {
                sla = Collections.emptyList();
            } else {
                final JsonObject metrics = conf.getJsonObject("metrics");
                sla = metrics.getJsonArray("sla", new JsonArray()).stream()
                        .mapToInt(slaBucket -> (int)slaBucket)
                        .sorted()
                        .mapToObj(Duration::ofMillis)
                        .collect(Collectors.toList());
            }
            return new Configuration(sla);
        }
    }
}
