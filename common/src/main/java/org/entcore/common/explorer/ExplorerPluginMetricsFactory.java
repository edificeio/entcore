package org.entcore.common.explorer;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import org.entcore.common.explorer.impl.MicrometerExplorerPluginMetricsRecorder;

/**
 * Creates the singleton that will record metrics of the explorer plugin.
 * So far, it only handles MicroMeter implementation. If Micrometer is not
 * configured then it creates a dummy recorder that records nothing.
 */
public class ExplorerPluginMetricsFactory {
    private static JsonObject globalConfig;
    private static IExplorerPluginMetricsRecorder explorerPluginMetricsRecorder;
    public static void init(final JsonObject config){
        globalConfig = config;
    }

    /**
     * Look into the global configuration to see how metrics were configured. If
     * Micrometer is configured then we create a Micrometer recorder. Otherwise we
     * send back a dummy recorder that records nothing.
     * @return An object to record metrics
     */
    public static IExplorerPluginMetricsRecorder getExplorerPluginMetricsRecorder() {
        if(explorerPluginMetricsRecorder == null) {
            if(globalConfig == null) {
                throw new IllegalStateException("explorer.plugin.metricsrecorder.factory.not.set");
            }
            final JsonObject metricsOptionsRaw = globalConfig.getJsonObject("metricsOptions");
            if(metricsOptionsRaw == null) {
                explorerPluginMetricsRecorder = new IExplorerPluginMetricsRecorder.NoopExplorerPluginMetricsRecorder();
            } else {
                final MetricsOptions metricsOptions = metricsOptionsRaw.mapTo(MetricsOptions.class);
                if(metricsOptions.isEnabled() && metricsOptions instanceof MicrometerMetricsOptions) {
                    explorerPluginMetricsRecorder = new MicrometerExplorerPluginMetricsRecorder();
                } else {
                    explorerPluginMetricsRecorder = new IExplorerPluginMetricsRecorder.NoopExplorerPluginMetricsRecorder();
                }
            }
        }
        return explorerPluginMetricsRecorder;
    }
}
