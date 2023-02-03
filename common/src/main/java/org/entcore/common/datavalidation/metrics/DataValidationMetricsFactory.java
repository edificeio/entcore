package org.entcore.common.datavalidation.metrics;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates the singleton that will record metrics of the data validation and mfa workflows.
 * So far, it only handles MicroMeter implementation. If metricsOptions are not
 * configured then it creates a dummy recorder that records nothing.
 */
 public class DataValidationMetricsFactory {
    private static MetricsOptions metricsOptions;
    private static DataValidationMetricsRecorder metricsRecorder;
    private static JsonObject config;

    public static void init(final Vertx vertx, final JsonObject config){
        DataValidationMetricsFactory.config = config;
        if(config.getJsonObject("metricsOptions") == null) {
            final String metricsOptions = (String) vertx.sharedData().getLocalMap("server").get("metricsOptions");
            if(metricsOptions == null){
                DataValidationMetricsFactory.metricsOptions = new MetricsOptions().setEnabled(false);
            }else{
                DataValidationMetricsFactory.metricsOptions = new MetricsOptions(new JsonObject(metricsOptions));
            }
        } else {
            metricsOptions = new MetricsOptions(config.getJsonObject("metricsOptions"));
        }
    }

    /**
     * @return The backend to record metrics. If metricsOptions is defined in the configuration then the backend used
     * is MicroMeter. Otherwise a dummy registrar is returned and it collects nothing.
     */
    public static DataValidationMetricsRecorder getRecorder() {
        if(metricsRecorder == null) {
            if(metricsOptions == null) {
                throw new IllegalStateException("ingest.job.metricsrecorder.factory.not.set");
            }
            if(metricsOptions.isEnabled()) {
                metricsRecorder = new MicrometerDataValidationMetricsRecorder(MicrometerDataValidationMetricsRecorder.Configuration.fromJson(config));
            } else {
                metricsRecorder = new DataValidationMetricsRecorder.NoopRecorder();
            }
        }
        return metricsRecorder;
    }
}
