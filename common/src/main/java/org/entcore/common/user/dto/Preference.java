package org.entcore.common.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.Json;

public interface Preference {

    default String encode() {
        return Json.encode(this);
    }

    /** Returns true if the preference data is valid and can be persisted. */
    @JsonIgnore
    default boolean validate() {
        return true;
    }
}
