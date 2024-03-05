package org.entcore.common.audience.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotifyResourceDeletionResponseMessage {

    private final boolean success;

    private final String errorMessage;

    @JsonCreator
    public NotifyResourceDeletionResponseMessage(@JsonProperty("success") final boolean success,
                                                 @JsonProperty("errorMessage") final String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public NotifyResourceDeletionResponseMessage(boolean success) {
        this(true, null);
    }

    public NotifyResourceDeletionResponseMessage(String errorMessage) {
        this(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
