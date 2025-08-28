package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.List;

/**
 * DTO for notification preview data
 */
public class NotificationPreviewDTO {
    private final String text;
    private final List<String> images;

    @JsonCreator
    public NotificationPreviewDTO(
            @JsonProperty("text") String text,
            @JsonProperty("images") List<String> images) {
        this.text = text;
        this.images = images != null ? images : Collections.emptyList();
    }

    public String getText() {
        return text;
    }

    public List<String> getImages() {
        return images;
    }

    /**
     * Converts this preview object to a JsonObject for use with TimelineHelper
     *
     * @return JsonObject representation of this preview
     */
    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        if (text != null) {
            json.put("text", text);
        }
        if (images != null && !images.isEmpty()) {
            json.put("images", new JsonArray(images));
        }
        return json;
    }
}
