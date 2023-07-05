package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ExplorerReindexResourcesResponse {
    private final int nbMessages;
    private final int nbBatch;
    private final Map<String, Object> metrics;

    @JsonCreator
    public ExplorerReindexResourcesResponse(@JsonProperty("nbMessages") final int nbMessages,
                                            @JsonProperty("nbBatch") final int nbBatch,
                                            @JsonProperty("metrics") final Map<String, Object> metrics) {
        this.nbBatch = nbBatch;
        this.nbMessages = nbMessages;
        this.metrics = metrics;
    }

    public int getNbMessages() {
        return nbMessages;
    }

    public int getNbBatch() {
        return nbBatch;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "ExplorerReindexResponse{" +
                "nbMessages=" + nbMessages +
                ", nbBatch=" + nbBatch +
                '}';
    }
}
