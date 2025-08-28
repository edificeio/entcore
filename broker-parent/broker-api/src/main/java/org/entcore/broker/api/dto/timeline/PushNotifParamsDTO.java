package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

/**
 * DTO for push notification specific parameters
 */
public class PushNotifParamsDTO {
    private final String title;
    private final String body;

    @JsonCreator
    public PushNotifParamsDTO(
            @JsonProperty("title") String title,
            @JsonProperty("body") String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("title", title)
                .put("body", body);
    }
}
