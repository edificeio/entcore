package org.entcore.audience.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Gaps between the directory and referential answers of the audience read (referential POC, step 8). */
public class AclComparatorTest {

	private static final String ACTION = "get-users-displayNames";

	@Test
	public void sameAnswersHaveNoGap() {
		final JsonObject names = new JsonObject().put("u1", "NAME u1").put("u2", "NAME u2");
		assertEquals(Collections.emptyList(), compare(names, names.copy()));
	}

	@Test
	public void gapsCarryIdsAndFieldNamesOnly() {
		final JsonObject neo4j = new JsonObject().put("u1", "NAME u1").put("u2", "NAME u2");
		final JsonObject pg = new JsonObject().put("u1", "OTHER NAME").put("u3", "NAME u3");

		final List<JsonObject> diffs = compare(neo4j, pg);

		assertEquals(3, diffs.size());
		assertEquals(new JsonObject().put("entityType", "USER").put("entityId", "u1").put("kind", "FIELD_MISMATCH")
				.put("fields", new JsonArray().add("displayName")), diffs.get(0));
		assertEquals("MISSING_IN_PG", diffs.get(1).getString("kind"));
		assertEquals("u2", diffs.get(1).getString("entityId"));
		assertEquals("EXTRA_IN_PG", diffs.get(2).getString("kind"));
		assertFalse("no value in a gap", diffs.toString().contains("NAME"));
	}

	@Test
	public void repeatedRowsAreOneUserWithSeveralValues() {
		final JsonArray neo4j = new JsonArray().add(row("u1", "A")).add(row("u1", "B"));
		final List<JsonObject> diffs = AclComparator.compare(ACTION, neo4j, new JsonArray().add(row("u1", "A")));
		assertEquals(new JsonArray().add("displayName"), diffs.get(0).getJsonArray("fields"));
	}

	@Test
	public void absentAndNullValuesAreEqual() {
		final JsonObject neo4j = new JsonObject().putNull("u1");
		final JsonArray pg = new JsonArray().add(new JsonObject().put("id", "u1"));
		assertEquals(Collections.emptyList(),
				AclComparator.compare(ACTION, AclComparator.rows(neo4j, "displayName"), pg));
		assertTrue(AclComparator.isCompared(ACTION));
		assertFalse(AclComparator.isCompared("list-users"));
	}

	private static List<JsonObject> compare(JsonObject neo4j, JsonObject pg) {
		final String field = AclComparator.field(ACTION);
		return AclComparator.compare(ACTION, AclComparator.rows(neo4j, field), AclComparator.rows(pg, field));
	}

	private static JsonObject row(String id, String displayName) {
		return new JsonObject().put("id", id).put("displayName", displayName);
	}
}
