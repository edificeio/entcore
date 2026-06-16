package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.RemoveRelationsResultDTO;
import org.entcore.communication.services.CommunicationService;

public class RemoveRelationsResultDtoMapper {

    public static RemoveRelationsResultDTO map(JsonObject json) {
        String sender = json.getString("sender");
        String receiver = json.getString("receiver");
        return new RemoveRelationsResultDTO(
                sender != null ? CommunicationService.Direction.fromString(sender) : null,
                receiver != null ? CommunicationService.Direction.fromString(receiver) : null
        );
    }
}