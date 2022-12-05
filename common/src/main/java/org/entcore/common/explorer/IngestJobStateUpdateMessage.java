package org.entcore.common.explorer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class IngestJobStateUpdateMessage {
    private final String entityId;
    private final long version;
    private final IngestJobState state;

    @JsonCreator
    public IngestJobStateUpdateMessage(@JsonProperty("entityId") final String entityId,
                                       @JsonProperty("version") final long version,
                                       @JsonProperty("state") final IngestJobState state) {
        this.entityId = entityId;
        this.version = version;
        this.state = state;
    }

    public String getEntityId() {
        return entityId;
    }

    public long getVersion() {
        return version;
    }

    public IngestJobState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "IngestJobStateUpdateMessage{" +
                "entityId='" + entityId + '\'' +
                ", version=" + version +
                ", state=" + state +
                '}';
    }
}
