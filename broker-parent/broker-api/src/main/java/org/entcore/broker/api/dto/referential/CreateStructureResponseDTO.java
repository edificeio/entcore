package org.entcore.broker.api.dto.referential;

public class CreateStructureResponseDTO {
	private final String id;

	public CreateStructureResponseDTO(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
