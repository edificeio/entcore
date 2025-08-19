package org.entcore.broker.api.dto.referential;

public class DetachStructureResponseDTO {

	private final String structureId;

	public DetachStructureResponseDTO(String structureId) {
		this.structureId = structureId;
	}

	public String getStructureId() {
		return structureId;
	}
}
