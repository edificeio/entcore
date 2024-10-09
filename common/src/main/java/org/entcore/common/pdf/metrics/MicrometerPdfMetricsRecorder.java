package org.entcore.common.pdf.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.micrometer.backends.BackendRegistries;
import org.entcore.common.pdf.PdfGenerator;

import java.io.File;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Record pdf metrics using micrometer
 */
public class MicrometerPdfMetricsRecorder implements PdfMetricsRecorder {
    private final Vertx vertx;
    private final String hostName;
    private final Timer succeedConversionTime;
    private final Timer failedConversionTime;
    private final Timer unfinishedConversionTime;
    private final Configuration configuration;
    private final AtomicInteger ongoingConversions = new AtomicInteger(0);
    private final MeterRegistry registry = BackendRegistries.getDefaultNow();
    private final Map<CounterKey, Counter> succeedConversions = new HashMap<>();
    private final Map<CounterKey, Counter> failedConversions = new HashMap<>();
    private final Map<CounterKey, Counter> unfinishedConversions = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(MicrometerPdfMetricsRecorder.class);

    public MicrometerPdfMetricsRecorder(Vertx vertx, Configuration configuration) {
        this.vertx = vertx;
        this.configuration = configuration;
        // define metrics
        succeedConversionTime = Timer.builder("pdf.conversion.time.succeed").description("Pdf conversion time on succeed").register(registry);
        unfinishedConversionTime = Timer.builder("pdf.conversion.time.unfinished").description("Pdf conversion time on unfinished").register(registry);
        failedConversionTime = Timer.builder("pdf.conversion.time.failed").description("Pdf conversion time on failed").register(registry);
        Gauge.builder("pdf.conversion.error.rate", () -> {
            final double totalUnfinished = unfinishedConversions.values().stream().mapToDouble(Counter::count).sum();
            final double totalSucceed = succeedConversions.values().stream().mapToDouble(Counter::count).sum();
            final double totalFailed = failedConversions.values().stream().mapToDouble(Counter::count).sum();
            final double total = totalSucceed + totalFailed + totalUnfinished;
            if(total == 0d){
                return 0;
            }
            return (totalFailed + totalUnfinished) / total;
        }).description("Conversion error rate").register(registry);
        Gauge.builder("pdf.conversion.pending", () -> ongoingConversions.get())
                .description("Number of pending conversion").register(registry);
        // keep hostname
        String tempHostname;
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            tempHostname = addr.getHostName();
        } catch (Exception e) {
            tempHostname = "";
        }
        this.hostName = tempHostname;
    }

    /**
     * Create a Succeed Counter by specifying following tags:
     * <ul>
     *     <li>HostName: the machine name that call pdf generator</li>
     *     <li>TaskKind: the kind of task that succeed (Preview, Print)</li>
     *     <li>SourceKind: the kind of file to be converted (Word, ...)</li>
     * </ul>
     * @param context Context information about the generation
     * @return Counter
     */
    private Counter getSucceed(final PdfMetricsContext context){
        final CounterKey key = new CounterKey(null, null, 200, context.getTaskKind(), context.getSourceKind());
        final Counter counter = succeedConversions.computeIfAbsent(key, (k) -> {
            return Counter.builder("pdf.conversion.success")
                    .description("Number of successful conversions")
                    .tag("hostName", hostName)
                    .tag("taskKind", context.getTaskKind().name())
                    .tag("sourceKind", context.getSourceKind().name())
                    .register(registry);
        });
        succeedConversions.put(key, counter);
        return counter;
    }


    /**
     * Create a Failed Counter by specifying following tags:
     * <ul>
     *     <li>HostName: the machine name that call pdf generator</li>
     *     <li>TaskKind: the kind of task that succeed (Preview, Print)</li>
     *     <li>SourceKind: the kind of file to be converted (Word, ...)</li>
     *     <li>ErrorCode: the HTTP error code result</li>
     * </ul>
     * @param context Context information about the generation
     * @return Counter
     */
    private Counter getFailed(final PdfMetricsContext context, final int status){
        final CounterKey key = new CounterKey(null, null, status, context.getTaskKind(), context.getSourceKind());
        final Counter counter = failedConversions.computeIfAbsent(key, (k) -> {
            return Counter.builder("pdf.conversion.failure")
                    .description("Number of failed conversions")
                    .tag("hostName", hostName)
                    .tag("taskKind", context.getTaskKind().name())
                    .tag("sourceKind", context.getSourceKind().name())
                    .tag("errorCode", status+"")
                    .register(registry);
        });
        failedConversions.put(key, counter);
        return counter;
    }

    /**
     * Create an Unfinished Counter by specifying following tags:
     * <ul>
     *     <li>HostName: the machine name that call pdf generator</li>
     *     <li>TaskKind: the kind of task that succeed (Preview, Print)</li>
     *     <li>SourceKind: the kind of file to be converted (Word, ...)</li>
     *     <li>PhaseKind: wether the connexion has been closed during Request or Response</li>
     *     <li>ErrorKind: the kind of error that closed the connexion</li>
     * </ul>
     * @param context Context information about the generation
     * @return Counter
     */
    private Counter getUnfinished(final PdfMetricsContext context, final Phase phase, final String errorKind){
        final CounterKey key = new CounterKey(null, null, null, context.getTaskKind(), context.getSourceKind());
        final Counter counter = unfinishedConversions.computeIfAbsent(key, (k) -> {
            return Counter.builder("pdf.conversion.unfinished")
                    .description("Number of unfinished conversions")
                    .tag("hostName", hostName)
                    .tag("taskKind", context.getTaskKind().name())
                    .tag("sourceKind", context.getSourceKind().name())
                    .tag("phaseKind", phase.name())
                    .tag("errorKind", errorKind)
                    .register(registry);
        });
        unfinishedConversions.put(key, counter);
        return counter;
    }

    @Override
    public void onPdfGenerationStart(PdfMetricsContext context) {
        ongoingConversions.addAndGet(1);
    }

    @Override
    public void onPdfGenerationSucceed(PdfMetricsContext context) {
        ongoingConversions.addAndGet(-1);
        getSucceed(context).increment();
        succeedConversionTime.record(context.getElapsedTime());
    }

    @Override
    public void onPdfGenerationFailed(PdfMetricsContext context, int statusCode) {
        ongoingConversions.addAndGet(-1);
        getFailed(context, statusCode).increment();
        failedConversionTime.record(context.getElapsedTime());
    }

    /**
     * Register the fact that a generation has never finished
     * If configuration.parkingEnabled is true, the content of the file to be converted is parked into configuration.parkingPath.
     * If the source is an URL, nothing is parked.
     *
     * @param context Context information about the generation
     * @param phase specify the phase on wich connexion has been closed (request, response)
     * @param errorKind The kind of error triggered
     */
    @Override
    public void onPdfGenerationUnfinished(PdfMetricsContext context, Phase phase, String errorKind) {
        final Duration duration = context.getElapsedTime();
        ongoingConversions.addAndGet(-1);
        getUnfinished(context, phase, errorKind).increment();
        unfinishedConversionTime.record(duration);
        // parking file that could not be convert (because of timeout?)
        if(configuration.parkingEnabled){
            if(context.getFileContent().isPresent()){
                final String fileName = context.getFileName();
                final Buffer buffer = context.getFileContent().get();
                final String path = configuration.parkingPath + File.separator + fileName;
                vertx.fileSystem().writeFile(path, buffer, (result)->{
                    if(result.succeeded()){
                        log.info(String.format("Pdf conversion was finished prematurely so parking file: name='%s', duration=%s sec, phase=%s, error=%s", fileName, duration.getSeconds(), phase.name(), errorKind));
                    }else{
                        log.warn(String.format("Pdf conversion was finished prematurely but parking file '%s' failed", fileName), result.cause());
                    }
                });
            }else{
                log.info(String.format("Pdf conversion was finished prematurely but URL cannot be parked: url='%s', duration=%s sec, phase=%s, error=%s", context.getUrl().orElse(""), duration.getSeconds(), phase.name(), errorKind));
            }
        }else{
            log.warn(String.format("Pdf conversion was finished prematurely but parking is disabled: duration=%s sec, phase=%s, error=%s", duration.getSeconds(), phase.name(), errorKind));
        }
    }


    public static class Configuration {
        final String parkingPath;
        final boolean parkingEnabled;
        public Configuration(String parkingPath, boolean parkingEnabled) {
            this.parkingPath = parkingPath;
            this.parkingEnabled = parkingEnabled;
        }

        /**
         * Create a config object from json
         * <p>
         *     It expects that the configuration contains following fields :
         *     <ul>
         *         <li>parking-path: the file path on wich file are parked</li>
         *         <li>parking-enabled: wether file are parked if conversion never finished</li>
         *     </ul>
         * </p>
         * @param config json configuration
         * @return Configuration object
         */
        static Configuration fromJson(final JsonObject config) {
            return new Configuration(
                    config.getString("parking-path", "/srv/storage/tmp/pdf/"),
                    config.getBoolean("parking-enabled", false)
            );
        }
    }

    /**
     * CounterKey is a Key object that let us store our Counters into an HashMap.
     * The hashcode is computed from the fields (but each of those fields could be null)
     */
    static class CounterKey {
        /**
         * The phase of the conversion (REQUEST or RESPONSE).
         */ 
        private final Phase phase;
        /**
         * The kind of error on failed.
         */
        private final String errorKind;
        /**
         * The HTTP status code on failed.
         */
        private final Integer statusCode;
        /**
         * The kind of task (PREVIEW or PRINT).
         */
        private final TaskKind taskKind;
        /**
         * The kind of source (WORD, CSV, etc.).
         */
        private final String sourceKind;

        public CounterKey(Phase phase, String errorKind, Integer statusCode, TaskKind taskKind, PdfGenerator.SourceKind sourceKind) {
            this.phase = phase;
            this.errorKind = errorKind;
            this.statusCode = statusCode;
            this.taskKind = taskKind;
            this.sourceKind = sourceKind.name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CounterKey that = (CounterKey) o;

            if (phase != that.phase) return false;
            if (errorKind != null ? !errorKind.equals(that.errorKind) : that.errorKind != null) return false;
            if (statusCode != null ? !statusCode.equals(that.statusCode) : that.statusCode != null) return false;
            if (taskKind != that.taskKind) return false;
            return sourceKind == that.sourceKind;
        }

        @Override
        public int hashCode() {
            int result = phase != null ? phase.hashCode() : 0;
            result = 31 * result + (errorKind != null ? errorKind.hashCode() : 0);
            result = 31 * result + (statusCode != null ? statusCode.hashCode() : 0);
            result = 31 * result + (taskKind != null ? taskKind.hashCode() : 0);
            result = 31 * result + (sourceKind != null ? sourceKind.hashCode() : 0);
            return result;
        }
    }
}
