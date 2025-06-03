package org.entcore.common.pdf.metrics;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.metrics.MetricsOptions;
/**
 * Creates the singleton that will record metrics of the pdf generator client.
 * So far, it only handles MicroMeter implementation. If metricsOptions are not
 * configured then it creates a dummy recorder that records nothing.
 */
public class PdfMetricsRecorderFactory {

    private static final Logger log = LoggerFactory.getLogger(PdfMetricsRecorderFactory.class);

    private static Vertx vertx;
    private static MetricsOptions metricsOptions;
    private static PdfMetricsRecorder ingestJobMetricsRecorder;
    private static JsonObject config;
    public static void init(final Vertx vertx, final JsonObject config, String metricsOptions){
        if(PdfMetricsRecorderFactory.vertx != null){
            // already init
            return;
        }
        PdfMetricsRecorderFactory.vertx = vertx;
        PdfMetricsRecorderFactory.config = config == null? new JsonObject() : config;
        if(PdfMetricsRecorderFactory.config.getJsonObject("metricsOptions") == null) {
            if(metricsOptions == null){
                PdfMetricsRecorderFactory.metricsOptions = new MetricsOptions().setEnabled(false);
            }else{
                PdfMetricsRecorderFactory.metricsOptions = new MetricsOptions(new JsonObject(metricsOptions));
            }
        } else {
            PdfMetricsRecorderFactory.metricsOptions = new MetricsOptions(PdfMetricsRecorderFactory.config.getJsonObject("metricsOptions"));
        }
    }

    /**
     * @return The backend to record metrics. If metricsOptions is defined in the configuration then the backend used
     * is MicroMeter. Otherwise a dummy registrar is returned and it collects nothing.
     */
    public static PdfMetricsRecorder getPdfMetricsRecorder() {
        if(ingestJobMetricsRecorder == null) {
            if(vertx == null) {
                throw new IllegalStateException("pdf.metricsrecorder.vertx.not.set");
            }
            if(metricsOptions == null) {
                throw new IllegalStateException("pdf.metricsrecorder.factory.not.set");
            }
            if(metricsOptions.isEnabled()) {
                ingestJobMetricsRecorder = new MicrometerPdfMetricsRecorder(vertx, MicrometerPdfMetricsRecorder.Configuration.fromJson(metricsOptions.toJson()));
            } else {
                ingestJobMetricsRecorder = new MicrometerPdfMetricsRecorder.NoopIngestJobMetricsRecorder();
            }
        }
        return ingestJobMetricsRecorder;
    }

    public static void setPdfMetricsRecorder(final PdfMetricsRecorder recorder) {
        ingestJobMetricsRecorder = recorder;
    }
}
