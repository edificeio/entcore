package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class MuteResponse {
    private final boolean succeeded;
    private final Throwable cause;
    public MuteResponse(boolean b) {
        succeeded = b;
        cause = null;
    }
    @JsonCreator
    public MuteResponse(@JsonProperty("succeeded") final boolean succeeded,
                        @JsonProperty("cause") final Throwable cause) {
        this.succeeded = succeeded;
        this.cause = cause;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public Throwable getCause() {
        return cause;
    }
}
