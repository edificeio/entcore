package org.entcore.common.pdf.metrics;

import io.vertx.core.buffer.Buffer;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.utils.StopWatch;

import java.time.Duration;
import java.util.Optional;

/**
 * A context object that stores all the information about a generation
 */
public class PdfMetricsContext {
    /**
     * The stopwatch used to measure the time taken for the PDF generation process.
     */
    private final StopWatch watch = new StopWatch();

    /**
     * The type of task being performed, such as PREVIEW or PRINT.
     */
    private final PdfMetricsRecorder.TaskKind taskKind;

    /**
     * The content to be converted, either a URL or a file content.
     */ 
    private final PdfMetricsRecorder.Content content;

    /**
     * The kind of source file, such as WORD, CSV, etc.
     */
    private final PdfGenerator.SourceKind sourceKind;

    public PdfMetricsContext(PdfMetricsRecorder.TaskKind taskKind, PdfMetricsRecorder.Content content) {
        this(taskKind, content, PdfGenerator.SourceKind.html);
    }

    public PdfMetricsContext(PdfMetricsRecorder.TaskKind taskKind, PdfMetricsRecorder.Content content, PdfGenerator.SourceKind sourceKind) {
        this.taskKind = taskKind;
        this.content = content;
        this.sourceKind = sourceKind;
    }

    public PdfGenerator.SourceKind getSourceKind(){
        return sourceKind;
    }

    public Optional<Buffer> getFileContent(){
        return content.getBinary();
    }

    public Optional<String> getUrl(){
        return content.getUrl();
    }

    public StopWatch getWatch() {
        return watch;
    }

    public String getFileName(){
        return String.format("%s-%s-%s", System.currentTimeMillis(), taskKind.name(), getSourceKind());
    }

    public Duration getElapsedTime(){
        return watch.elapsedTime();
    }

    public PdfMetricsRecorder.TaskKind getTaskKind(){
        return taskKind;
    }

    public PdfMetricsRecorder.Content getContent(){
        return content;
    }

    @Override
    public String toString() {
        return "PdfMetricsContext{" +
                "watch=" + watch +
                ", taskKind=" + taskKind +
                ", content=" + content +
                ", sourceKind=" + sourceKind +
                '}';
    }
}
