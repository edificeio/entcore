package org.entcore.common.pdf.metrics;

import io.vertx.core.buffer.Buffer;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.utils.StopWatch;

import java.util.Optional;

/**
 * A context object that stores all the information about a generation:
 * <ul>
 *     <li>watch: A StopWatch object that keep track of elapsed time since generation started</li>
 *     <li>taskKind: Wether the generation is related to a Preview or a Print</li>
 *     <li>content: URL or File Content to be converted</li>
 *     <li>sourceKind: Wether the source file is a Word, Csv...</li>
 * </ul>
 */
public class PdfMetricsContext {
    public final StopWatch watch = new StopWatch();
    public final PdfMetricsRecorder.TaskKind taskKind;
    public final PdfMetricsRecorder.Content content;
    public final Optional<PdfGenerator.SourceKind> sourceKind;

    public PdfMetricsContext(PdfMetricsRecorder.TaskKind taskKind, PdfMetricsRecorder.Content content, Optional<PdfGenerator.SourceKind> sourceKind) {
        this.taskKind = taskKind;
        this.content = content;
        this.sourceKind = sourceKind;
    }

    public String getSourceKind(){
        return sourceKind.map(e -> e.name()).orElse("HTML");
    }

    public Optional<Buffer> getFileContent(){
        return content.binary;
    }

    public Optional<String> getUrl(){
        return content.url;
    }

    public String getFileName(){
        return String.format("%s-%s-%s", System.currentTimeMillis(), taskKind.name(), sourceKind.map(e -> e.name()).orElse(""));
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
