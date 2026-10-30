package org.entcore.feeder.aaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AAFFilesCleanRequest {
    private final String feeder;
    private final String subPath;

    @JsonCreator
    public AAFFilesCleanRequest(@JsonProperty("feeder") final String feeder,
                                @JsonProperty("subPath") final String subPath) {
        this.feeder = feeder;
        this.subPath = subPath;
    }

    /** @return The feeder (AAF, AAF1D) whose import directory is cleaned, or {@code null} for the default one. */
    public String getFeeder() {
        return feeder;
    }

    /** @return The uploaded directory to remove, or {@code null} to remove every uploaded directory. */
    public String getSubPath() {
        return subPath;
    }
}
