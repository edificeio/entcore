package org.entcore.communication.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.CommuniqueWithDTO;

public class CommuniqueWithDtoMapper {

    /** Neo4j property holding the intra-group communication rule of a group node. */
    private static final String NEO4J_USERS_FIELD = "users";
    /** DTO field exposing the intra-group communication rule. */
    private static final String INTERNAL_COMMUNICATION_RULE_FIELD = "internalCommunicationRule";
    private static final String COMMUNIQUE_WITH_FIELD = "communiqueWith";

    /**
     * Maps the raw result of {@code communiqueWith} (full Neo4j group nodes) to its DTO.
     * The nodes store the intra-group rule in {@code users}, which the DTO exposes as
     * {@code internalCommunicationRule}: without this renaming, the converter drops it silently.
     */
    public static CommuniqueWithDTO map(JsonObject json) {
        if (json == null) {
            return null;
        }
        final JsonObject group = withInternalCommunicationRule(json);
        final JsonArray communiqueWith = group.getJsonArray(COMMUNIQUE_WITH_FIELD);
        if (communiqueWith != null) {
            final JsonArray mapped = new JsonArray();
            for (Object item : communiqueWith) {
                mapped.add(item instanceof JsonObject ? withInternalCommunicationRule((JsonObject) item) : item);
            }
            group.put(COMMUNIQUE_WITH_FIELD, mapped);
        }
        return new CommuniqueWithDTO(group);
    }

    private static JsonObject withInternalCommunicationRule(JsonObject node) {
        final JsonObject copy = node.copy();
        final Object users = copy.getValue(NEO4J_USERS_FIELD);
        if (!copy.containsKey(INTERNAL_COMMUNICATION_RULE_FIELD) && users instanceof String) {
            copy.put(INTERNAL_COMMUNICATION_RULE_FIELD, users);
        }
        return copy;
    }
}
