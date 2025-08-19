package org.entcore.broker.api.dto.referential;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AttachStructureResponseDTO {

	private final String structureId;

	public AttachStructureResponseDTO(String structureId) {
		this.structureId = structureId;
	}

	public String getStructureId() {
		return structureId;
	}
}
