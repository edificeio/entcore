package org.entcore.feeder.aaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AAFFilesUploadRequest {
    private final String subPath;
    private final Map<String, String> files;

    @JsonCreator
    public AAFFilesUploadRequest(@JsonProperty("subPath") final String subPath,
                                 @JsonProperty("files") final Map<String, String> files) {
        this.subPath = subPath;
        this.files = files;
    }

    public String getSubPath() {
        return subPath;
    }

    public Map<String, String> getFiles() {
        return files;
    }
}
