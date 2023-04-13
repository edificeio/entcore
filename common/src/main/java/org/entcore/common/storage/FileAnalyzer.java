package org.entcore.common.storage;

import io.vertx.core.Future;
import org.entcore.common.messaging.to.UploadedFileMessage;

import java.util.List;

/**
 * Analyzes a single file whose content is stored in memory.
 */
public interface FileAnalyzer {
    /**
     * @param metadata Metadata describing the file to be analyzed.
     * @param content Whole content of the file
     * @return A report on the analysis and what was done
     */
    Future<Report> analyze(final UploadedFileMessage metadata, final byte[] content);

    /**
     * Information about what was done while analyzing this file.
     */
    static class Report {
        /** The file was fully processed.*/
        private final boolean ok;
        /** Ordered list of the actions which were performed. */
        private final List<String> actions;

        public Report(final boolean ok, final List<String> actions) {
            this.ok = ok;
            this.actions = actions;
        }

        public boolean isOk() {
            return ok;
        }

        public List<String> getActions() {
            return actions;
        }
    }
}
