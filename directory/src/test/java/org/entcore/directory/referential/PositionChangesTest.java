package org.entcore.directory.referential;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/** Notification of a position written by an ADML: the contract of the feeder's EntityChangedDTO (referential POC). */
public class PositionChangesTest {

	@Test
	public void sameFieldsAsTheFeederNotification() throws Exception {
		final Map<String, Object> changes = PositionChanges.of("directory-create-position", "p1", "s1");

		final JsonObject json = new JsonObject(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(changes));

		assertEquals(new TreeSet<>(Arrays.asList("action", "userIds", "structureIds", "structureExternalIds",
				"groupIds", "subjectIds", "positionIds", "functions", "groupMembers")), json.fieldNames());
		assertEquals(Collections.singletonList("p1"), json.getJsonArray("positionIds").getList());
		assertEquals(Collections.singletonList("s1"), json.getJsonArray("structureIds").getList());
	}

	@Test
	public void deletedPositionHasNoStructure() {
		assertEquals(Collections.emptyList(), PositionChanges.of("directory-delete-position", "p1", null).get("structureIds"));
	}
}
