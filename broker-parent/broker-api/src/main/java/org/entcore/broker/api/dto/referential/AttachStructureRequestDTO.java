package org.entcore.broker.api.dto.referential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttachStructureRequestDTO {

	private final String structureId;
	private final String parentStructureId;

	@JsonCreator
	public AttachStructureRequestDTO(
			@JsonProperty("structureId") String structureId,
			@JsonProperty("parentStructureId") String parentStructureId) {
		this.structureId = structureId;
		this.parentStructureId = parentStructureId;
	}

	public String getStructureId() {
		return structureId;
	}

	public String getParentStructureId() {
		return parentStructureId;
	}
}
