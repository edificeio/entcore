package org.entcore.feeder.referential;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entities touched by a manual action: the ids of its message (and of its "data"), plus the id created by a creation,
 * read in the reply ("result": [{"id": ...}]). Coarse on purpose: referential rereads them all in Neo4j.
 */
public final class ReferentialChanges {

	private static final List<String> USER_KEYS = Arrays.asList("userId", "userIds", "users", "studentId", "relativeId",
			"childrenIds");
	private static final List<String> STRUCTURE_KEYS = Arrays.asList("structureId", "structureIds", "parentStructureId");
	private static final List<String> GROUP_KEYS = Arrays.asList("classId", "classIds", "groupId", "groupIds");
	private static final List<String> SUBJECT_KEYS = Arrays.asList("subjectId");
	private static final List<String> POSITION_KEYS = Arrays.asList("positionId", "positionIds", "userPositionIds");
	private static final List<String> FUNCTION_ACTIONS = Arrays.asList("manual-create-function",
			"manual-delete-function");
	/** Kind of entity created by an action, whose id is in the reply. */
	private static final Map<String, String> CREATED = new HashMap<>();

	static {
		CREATED.put("manual-create-structure", "structure");
		CREATED.put("manual-create-class", "group");
		CREATED.put("manual-create-user", "user");
		CREATED.put("manual-create-group", "group");
		CREATED.put("manual-add-subject", "subject");
	}

	private ReferentialChanges() {
	}

	/** Changes of the successful action {@code action} with message {@code body} and reply {@code reply}. */
	public static EntityChangedDTO of(String action, JsonObject body, Object reply) {
		final EntityChangedDTO changes = new EntityChangedDTO(action);
		for (JsonObject source : Arrays.asList(body, body.getJsonObject("data"))) {
			if (source == null) {
				continue;
			}
			collect(source, USER_KEYS, changes.getUserIds());
			collect(source, STRUCTURE_KEYS, changes.getStructureIds());
			collect(source, Arrays.asList("structureExternalId"), changes.getStructureExternalIds());
			collect(source, GROUP_KEYS, changes.getGroupIds());
			collect(source, SUBJECT_KEYS, changes.getSubjectIds());
			collect(source, POSITION_KEYS, changes.getPositionIds());
		}
		changes.setFunctions(FUNCTION_ACTIONS.contains(action));
		final String created = CREATED.get(action);
		if (created != null && reply instanceof JsonObject) {
			final List<String> target;
			switch (created) {
				case "structure": target = changes.getStructureIds(); break;
				case "user": target = changes.getUserIds(); break;
				case "subject": target = changes.getSubjectIds(); break;
				default: target = changes.getGroupIds();
			}
			final Object result = ((JsonObject) reply).getValue("result");
			final JsonArray rows = result instanceof JsonArray ? (JsonArray) result
					: result instanceof JsonObject ? new JsonArray().add(result) : new JsonArray();
			for (Object row : rows) {
				if (row instanceof JsonObject) {
					add(((JsonObject) row).getValue("id"), target);
				}
			}
		}
		return changes;
	}

	private static void collect(JsonObject source, List<String> keys, List<String> target) {
		for (String key : keys) {
			add(source.getValue(key), target);
		}
	}

	private static void add(Object value, List<String> target) {
		if (value instanceof String && !((String) value).trim().isEmpty() && !target.contains(value)) {
			target.add((String) value);
		} else if (value instanceof JsonArray) {
			for (Object item : (JsonArray) value) {
				add(item, target);
			}
		}
	}
}
