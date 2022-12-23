package org.entcore.common.explorer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IdAndVersion {
    private final String id;
    private final long version;

    @JsonCreator
    public IdAndVersion(@JsonProperty("id") String id, @JsonProperty("version") long version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }
}
