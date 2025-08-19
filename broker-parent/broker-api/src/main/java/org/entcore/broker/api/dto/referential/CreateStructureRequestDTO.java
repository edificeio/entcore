package org.entcore.broker.api.dto.referential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateStructureRequestDTO {

	private final String name;
	private final String type;

	@JsonCreator
	public CreateStructureRequestDTO(
			@JsonProperty("name") String name,
			@JsonProperty("type") String type) {
		this.name = name;
		this.type = type;
	}


	public String getName() {
		return name;
	}
	public String getType() {
		return type;
	}

}
