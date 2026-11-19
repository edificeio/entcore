package org.entcore.feeder.referential;

import java.util.ArrayList;
import java.util.List;

/**
 * Entities touched by a manual action of the feeder, sent to referential which rereads them in Neo4j (referential
 * POC, step 4 in fallback mode B). Neo4j ids, except {@code structureExternalIds}. No personal data.
 */
public class EntityChangedDTO {

	private String action;
	private List<String> userIds = new ArrayList<>();
	private List<String> structureIds = new ArrayList<>();
	private List<String> structureExternalIds = new ArrayList<>();
	private List<String> groupIds = new ArrayList<>();
	private List<String> subjectIds = new ArrayList<>();
	private boolean functions;

	public EntityChangedDTO() {
	}

	public EntityChangedDTO(String action) {
		this.action = action;
	}

	/** True if the action touched no known entity (not a getter: not serialized). */
	public boolean touchesNothing() {
		return userIds.isEmpty() && structureIds.isEmpty() && structureExternalIds.isEmpty() && groupIds.isEmpty()
				&& subjectIds.isEmpty() && !functions;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<String> getUserIds() {
		return userIds;
	}

	public void setUserIds(List<String> userIds) {
		this.userIds = userIds;
	}

	public List<String> getStructureIds() {
		return structureIds;
	}

	public void setStructureIds(List<String> structureIds) {
		this.structureIds = structureIds;
	}

	public List<String> getStructureExternalIds() {
		return structureExternalIds;
	}

	public void setStructureExternalIds(List<String> structureExternalIds) {
		this.structureExternalIds = structureExternalIds;
	}

	public List<String> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(List<String> groupIds) {
		this.groupIds = groupIds;
	}

	public List<String> getSubjectIds() {
		return subjectIds;
	}

	public void setSubjectIds(List<String> subjectIds) {
		this.subjectIds = subjectIds;
	}

	public boolean isFunctions() {
		return functions;
	}

	public void setFunctions(boolean functions) {
		this.functions = functions;
	}
}
