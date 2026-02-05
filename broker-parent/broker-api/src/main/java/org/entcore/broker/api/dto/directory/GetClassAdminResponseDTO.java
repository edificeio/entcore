package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetClassAdminResponseDTO {
    private final Object data;

    @JsonCreator
    public GetClassAdminResponseDTO(@JsonProperty("data") Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
