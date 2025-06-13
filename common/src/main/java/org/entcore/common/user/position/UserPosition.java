package org.entcore.common.user.position;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class UserPosition {
	private final String id;
	private final String name;
	private final UserPositionSource source;
	private final String structureId;

	public UserPosition(String id, String name, UserPositionSource source, String structureId) {
		this.id = id;
		this.name = name;
		this.source = source;
		this.structureId = structureId;
	}

	/**
	 * Method providing a User Position built upon the Function codification
	 * @param dollarEncodedFunction function codification separated with dollars
	 *  - in AAF format :       [StructureExternalId]$[FunctionCode]$[FunctionName]$[PositionCode]$[PositionName]
	 *  - in free CSV format :  [StructureExternalId]$[PositionName] or [StructureExternalId]$$$$[PositionName]
	 * @param source the source type of data feed
	 * @return a User Position built upon Function information if possible
	 */
	public static Optional<UserPosition> getUserPositionFromEncodedFunction(String dollarEncodedFunction, UserPositionSource source) {
		Optional<UserPosition> userPosition = Optional.empty();
		String [] functionCodification = dollarEncodedFunction.split("\\$");
		// non aaf format
		if (functionCodification.length == 2) {
			userPosition = Optional.of(new UserPosition(null, functionCodification[1], source, functionCodification[0]));
		} else {
			// avoiding creating user position from teaching subjects
			if (!"ENS".equals(functionCodification[1]) 
				&& !"-".equals(functionCodification[1])
				&& functionCodification.length > 4 ) {
				if (functionCodification[2].isEmpty()){
					userPosition = Optional.of(new UserPosition(null, functionCodification[4], source, functionCodification[0]));
				} else {
					userPosition = Optional.of(new UserPosition(null, functionCodification[2] + " / " + functionCodification[4], source, functionCodification[0]));
				}
			}
		}
		return userPosition;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public UserPositionSource getSource() {
		return source;
	}

	public String getStructureId() {
		return structureId;
	}

	public JsonObject toJsonObject() {
		return new JsonObject()
			.put("name", name)
			.put("id", id)
			.put("source", source.name())
			.put("structureId", structureId);
	}
}
