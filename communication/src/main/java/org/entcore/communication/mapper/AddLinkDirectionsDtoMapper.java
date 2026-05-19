package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.AddLinkDirectionsDTO;
import org.entcore.communication.services.CommunicationService;

import java.util.HashMap;
import java.util.Map;

public class AddLinkDirectionsDtoMapper {

    public static AddLinkDirectionsDTO map(JsonObject json) {
        Map<String, CommunicationService.Direction> directions = new HashMap<>();
        if (!json.containsKey("ok")) {
            json.forEach(entry -> directions.put(entry.getKey(),
                    CommunicationService.Direction.fromString(entry.getValue().toString())));
        }
        return new AddLinkDirectionsDTO(directions);
    }
}