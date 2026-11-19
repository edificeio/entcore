package org.entcore.audience.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Gaps between the directory answer (Neo4j) and the referential answer (PostgreSQL) of the entcore.directory bus
 * read of audience (referential POC, step 8, copy of conversation's comparator): rows matched by id, fields compared
 * as text, a field absent equal to null. A gap carries the entity id and the field names, never a value.
 */
public final class AclComparator {

	/** Compared fields per bus action. */
	private static final Map<String, List<String>> FIELDS = new HashMap<>();
	private static final Map<String, String> ENTITY_TYPES = new HashMap<>();

	static {
		FIELDS.put("get-users-displayNames", Collections.singletonList("displayName"));
		ENTITY_TYPES.put("get-users-displayNames", "USER");
	}

	private AclComparator() {
	}

	/** True if the action is a read compared by the ACL. */
	public static boolean isCompared(String action) {
		return FIELDS.containsKey(action);
	}

	/** Rows [{id, field: value}] of an answer {id: value}, the shape of "get-users-displayNames". */
	public static JsonArray rows(JsonObject byId, String field) {
		final JsonArray rows = new JsonArray();
		byId.forEach(entry -> rows.add(new JsonObject().put("id", entry.getKey()).put(field, entry.getValue())));
		return rows;
	}

	/** Field of the answer {id: value} of {@code action}. */
	public static String field(String action) {
		return FIELDS.get(action).get(0);
	}

	/** Gaps of {@code action}: {entityType, entityId, kind, fields}. */
	public static List<JsonObject> compare(String action, JsonArray neo4j, JsonArray pg) {
		final List<String> fields = FIELDS.get(action);
		final Map<String, Map<String, Set<String>>> left = index(neo4j, fields);
		final Map<String, Map<String, Set<String>>> right = index(pg, fields);
		final List<JsonObject> diffs = new ArrayList<>();
		for (Map.Entry<String, Map<String, Set<String>>> row : left.entrySet()) {
			final Map<String, Set<String>> other = right.get(row.getKey());
			if (other == null) {
				diffs.add(diff(action, row.getKey(), "MISSING_IN_PG", new JsonArray()));
				continue;
			}
			final JsonArray mismatches = new JsonArray();
			for (String field : fields) {
				if (!row.getValue().get(field).equals(other.get(field))) {
					mismatches.add(field);
				}
			}
			if (!mismatches.isEmpty()) {
				diffs.add(diff(action, row.getKey(), "FIELD_MISMATCH", mismatches));
			}
		}
		for (String id : right.keySet()) {
			if (!left.containsKey(id)) {
				diffs.add(diff(action, id, "EXTRA_IN_PG", new JsonArray()));
			}
		}
		return diffs;
	}

	/** Values of each field by row id; a row repeated with other values (several profiles) adds them to a set. */
	private static Map<String, Map<String, Set<String>>> index(JsonArray rows, List<String> fields) {
		final Map<String, Map<String, Set<String>>> index = new LinkedHashMap<>();
		if (rows == null) {
			return index;
		}
		for (Object o : rows) {
			if (!(o instanceof JsonObject) || ((JsonObject) o).getString("id") == null) {
				continue;
			}
			final JsonObject row = (JsonObject) o;
			final Map<String, Set<String>> values = index.computeIfAbsent(row.getString("id"), id -> {
				final Map<String, Set<String>> empty = new HashMap<>();
				fields.forEach(field -> empty.put(field, new TreeSet<>()));
				return empty;
			});
			for (String field : fields) {
				values.get(field).addAll(texts(row.getValue(field)));
			}
		}
		return index;
	}

	/** Text of a value; a list gives its elements, an object its id and name. */
	private static List<String> texts(Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		if (value instanceof JsonArray) {
			final List<String> texts = new ArrayList<>();
			((JsonArray) value).forEach(item -> texts.addAll(texts(item)));
			return texts;
		}
		if (value instanceof JsonObject) {
			final JsonObject object = (JsonObject) value;
			return Collections.singletonList(object.getString("id") + "|" + object.getString("name"));
		}
		final String text = String.valueOf(value);
		return text.isEmpty() ? Collections.emptyList() : Collections.singletonList(text);
	}

	private static JsonObject diff(String action, String id, String kind, JsonArray fields) {
		return new JsonObject().put("entityType", ENTITY_TYPES.get(action)).put("entityId", id).put("kind", kind)
				.put("fields", fields);
	}
}
