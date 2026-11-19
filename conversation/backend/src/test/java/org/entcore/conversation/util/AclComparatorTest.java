package org.entcore.conversation.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Gaps between the directory and referential answers of the pilot reads (referential POC, step 6). */
public class AclComparatorTest {

	@Test
	public void sameAnswersHaveNoGap() {
		final JsonArray users = new JsonArray().add(user("u1", "Student")).add(user("u2", "Teacher"));
		assertEquals(Collections.emptyList(), AclComparator.compare("list-users", users, users.copy()));
	}

	@Test
	public void gapsCarryIdsAndFieldNamesOnly() {
		final JsonArray neo4j = new JsonArray().add(user("u1", "Student")).add(user("u2", "Teacher"));
		final JsonArray pg = new JsonArray().add(user("u1", "Relative")).add(user("u3", "Teacher"));

		final List<JsonObject> diffs = AclComparator.compare("list-users", neo4j, pg);

		assertEquals(3, diffs.size());
		assertEquals(new JsonObject().put("entityType", "USER").put("entityId", "u1").put("kind", "FIELD_MISMATCH")
				.put("fields", new JsonArray().add("type")), diffs.get(0));
		assertEquals("MISSING_IN_PG", diffs.get(1).getString("kind"));
		assertEquals("EXTRA_IN_PG", diffs.get(2).getString("kind"));
		assertFalse("no value in a gap", diffs.toString().contains("Relative"));
	}

	@Test
	public void repeatedRowsAreOneUserWithSeveralTypes() {
		final JsonArray neo4j = new JsonArray().add(user("u1", "Teacher")).add(user("u1", "Personnel"));
		final List<JsonObject> diffs = AclComparator.compare("list-users", neo4j, new JsonArray().add(user("u1", "Teacher")));
		assertEquals(new JsonArray().add("type"), diffs.get(0).getJsonArray("fields"));
	}

	@Test
	public void structuresAreASetAndThePreferredOneIsNotCompared() {
		final JsonArray neo4j = new JsonArray().add(new JsonObject().put("id", "u1").put("preferredStructureId", "s2")
				.put("structures", new JsonArray().add(structure("s1")).add(structure("s2"))));
		final JsonArray pg = new JsonArray().add(new JsonObject().put("id", "u1")
				.put("structures", new JsonArray().add(structure("s2")).add(structure("s1"))));
		assertEquals(Collections.emptyList(), AclComparator.compare("list-users-structures", neo4j, pg));
	}

	@Test
	public void absentAndNullFieldsAreEqual() {
		final JsonArray neo4j = new JsonArray().add(new JsonObject().put("id", "g1").put("name", "G").putNull("displayName")
				.put("nbUsers", 3).put("type", "ManualGroup"));
		final JsonArray pg = new JsonArray().add(new JsonObject().put("id", "g1").put("name", "G").put("nbUsers", 3)
				.put("type", "ManualGroup"));
		assertEquals(Collections.emptyList(), AclComparator.compare("getGroupsInfos", neo4j, pg));
		assertTrue(AclComparator.isCompared("getGroupsInfos"));
		assertFalse(AclComparator.isCompared("getUserQuota"));
	}

	private static JsonObject user(String id, String type) {
		return new JsonObject().put("id", id).put("login", id + ".login").put("username", "NAME " + id).put("type", type);
	}

	private static JsonObject structure(String id) {
		return new JsonObject().put("id", id).put("name", "School " + id);
	}
}
