package org.entcore.common.user.dto;

import io.vertx.core.json.Json;

public interface Preference {

    default String encode() {
        return Json.encode(this);
    }
}
