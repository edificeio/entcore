package org.entcore.common.pdf.metrics;

import io.vertx.core.buffer.Buffer;
import org.entcore.common.pdf.PdfGenerator;

import java.util.Optional;
/**
 * Recorder of metrics related to pdf generator
 */
public interface PdfMetricsRecorder {
    /**
     * Create a Context object then Register the fact a PDF generation has been started
     * @param taskKind kind of task requiring pdf generation
     * @param content the content that need to be converted to pdf
     * @return Context information about the generation
     */
    default PdfMetricsContext onPdfGenerationStart(final TaskKind taskKind, final Content content) {
        return onPdfGenerationStart(taskKind, content, PdfGenerator.SourceKind.html);
    }

    /**
     * Create a Context object then Register the fact a PDF generation has been started
     * @param taskKind kind of task requiring pdf generation
     * @param content the content that need to be converted to pdf
     * @param sourceKind kind of content to be converted
     * @return Context information about the generation
     */
    default PdfMetricsContext onPdfGenerationStart(final TaskKind taskKind, final Content content, final PdfGenerator.SourceKind sourceKind){
        final PdfMetricsContext context = new PdfMetricsContext(taskKind, content, sourceKind);
        onPdfGenerationStart(context);
        return context;
    }

    /**
     * Register the fact that a PDF generation has been started
     * @param context Context information about the generation
     */
    void onPdfGenerationStart(final PdfMetricsContext context);

    /**
     * Register the fact that a PDF generation has finished successfully
     * @param context Context information about the generation
     */
    void onPdfGenerationSucceed(final PdfMetricsContext context);

    /**
     * Register the fact that a PDF generation has finished with an error
     * @param context Context information about the generation
     * @param statusCode The HTTP error code
     */
    void onPdfGenerationFailed(final PdfMetricsContext context, final int statusCode);

    /**
     * Register the fact that a PDF generation has never finished (timeout, connexion closed...)
     * @param context Context information about the generation
     * @param phase specify the phase on wich connexion has been closed (request, response)
     * @param errorKind The kind of error triggered
     */
    void onPdfGenerationUnfinished(final PdfMetricsContext context, final Phase phase, final String errorKind);


    /**
     * Create a Context object related to a PDF generation
     * @param taskKind kind of task requiring pdf generation
     * @param content the content that need to be converted to pdf
     * @return Context information about the generation
     */
    default PdfMetricsContext createContext(final TaskKind taskKind, final Content content){
        return new PdfMetricsContext(taskKind, content);
    }

    /**
     * Create a Context object related to a PDF generation
     * @param taskKind kind of task requiring pdf generation
     * @param content the content that need to be converted to pdf
     * @param sourceKind kind of content to be converted
     * @return Context information about the generation
     */
    default PdfMetricsContext createContext(final TaskKind taskKind, final Content content, final PdfGenerator.SourceKind sourceKind){
        return new PdfMetricsContext(taskKind, content, sourceKind);
    }

    enum TaskKind {
        Preview, Print
    }

    enum Phase {
        Request, Response
    }

    /**
     * The content to be converted to pdf.
     * It could be either an URL or a Buffer representing the object
     */
    class Content {
        private final String url;
        private final Buffer binary;

        private Content(String url) {
            this.url = url;
            this.binary = null;
        }

        private Content(Buffer binary) {
            this.url = null;
            this.binary = binary;
        }

        public Optional<Buffer> getBinary() {
            return Optional.ofNullable(binary);
        }

        public Optional<String> getUrl() {
            return Optional.ofNullable(url);
        }

        public static Content fromText(final String text){
            return new Content(Buffer.buffer(text));
        }

        public static Content fromBuffer(final Buffer buffer){
            return new Content(buffer);
        }

        public static Content fromUrl(final String url){
            return new Content(url);
        }
    }

    class NoopIngestJobMetricsRecorder implements PdfMetricsRecorder {
        @Override
        public void onPdfGenerationStart(PdfMetricsContext context) {}

        @Override
        public void onPdfGenerationSucceed(PdfMetricsContext context) {}

        @Override
        public void onPdfGenerationFailed(PdfMetricsContext context, int statusCode) {}

        @Override
        public void onPdfGenerationUnfinished(PdfMetricsContext context, Phase phase, String errorKind) {}
    }
}
