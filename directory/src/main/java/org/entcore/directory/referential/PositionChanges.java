package org.entcore.directory.referential;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification of a position written by an ADML (create, rename, delete): the fields of the feeder's
 * EntityChangedDTO, referential rereading the position and its structure in Neo4j. Neo4j ids, no personal data.
 */
public final class PositionChanges {

	private PositionChanges() {
	}

	public static Map<String, Object> of(String action, String positionId, String structureId) {
		final Map<String, Object> changes = new LinkedHashMap<>();
		changes.put("action", action);
		changes.put("userIds", Collections.emptyList());
		changes.put("structureIds", ids(structureId));
		changes.put("structureExternalIds", Collections.emptyList());
		changes.put("groupIds", Collections.emptyList());
		changes.put("subjectIds", Collections.emptyList());
		changes.put("positionIds", ids(positionId));
		changes.put("functions", false);
		changes.put("groupMembers", false);
		return changes;
	}

	private static List<String> ids(String id) {
		final List<String> ids = new ArrayList<>();
		if (id != null && !id.isEmpty()) {
			ids.add(id);
		}
		return ids;
	}
}
