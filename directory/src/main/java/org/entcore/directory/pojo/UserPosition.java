package org.entcore.directory.pojo;

public class UserPosition implements Comparable<UserPosition> {
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

	@Override
	public int compareTo(UserPosition o) {
		return this.name.compareTo(o.getName());
	}
}
