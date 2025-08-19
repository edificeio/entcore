package org.entcore.broker.api.dto.referential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DetachStructureRequestDTO {

	private final String structureId;
	private final String parentStructureId;

	@JsonCreator
	public DetachStructureRequestDTO(
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
