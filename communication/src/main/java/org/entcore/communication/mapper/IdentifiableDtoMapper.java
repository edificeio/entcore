package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.IdentifiableDTO;

public class IdentifiableDtoMapper {

    public static IdentifiableDTO map(JsonObject json) {
      return new IdentifiableDTO(json.getString("id"), json.getString("name"));
    }

}
