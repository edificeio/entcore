package org.entcore.communication.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.CommuniqueWithDTO;
import org.entcore.communication.dto.rest.GroupDTO.InternalCommunicationRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CommuniqueWithDtoMapperTest {

    @Test
    public void mapsNeo4jUsersFieldToInternalCommunicationRule() {
        final JsonObject raw = new JsonObject()
                .put("id", "g1").put("name", "Manual").put("users", "BOTH")
                .put("communiqueWith", new JsonArray()
                        .add(new JsonObject().put("id", "g2").put("users", "INCOMING")));

        final CommuniqueWithDTO dto = CommuniqueWithDtoMapper.map(raw);

        assertEquals(InternalCommunicationRule.BOTH, dto.getInternalCommunicationRule());
        assertEquals(InternalCommunicationRule.INCOMING, dto.getCommuniqueWith().get(0).getInternalCommunicationRule());
        assertEquals("BOTH", dto.toJson().getString("internalCommunicationRule"));
    }

    @Test
    public void leavesRuleEmptyWhenGroupHasNoUsersField() {
        final CommuniqueWithDTO dto = CommuniqueWithDtoMapper.map(new JsonObject().put("id", "g1"));

        assertNull(dto.getInternalCommunicationRule());
    }
}
