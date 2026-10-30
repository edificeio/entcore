package org.entcore.feeder.aaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AAFFilesUploadRequest {
    private final String subPath;
    private final Map<String, String> files;
    private final String feeder;

    @JsonCreator
    public AAFFilesUploadRequest(@JsonProperty("subPath") final String subPath,
                                 @JsonProperty("files") final Map<String, String> files,
                                 @JsonProperty("feeder") final String feeder) {
        this.subPath = subPath;
        this.files = files;
        this.feeder = feeder;
    }

    public String getSubPath() {
        return subPath;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    /**
     * @return The feeder (AAF, AAF1D) whose import directory receives the files, or {@code null} to use the
     * default import directory.
     */
    public String getFeeder() {
        return feeder;
    }
}
