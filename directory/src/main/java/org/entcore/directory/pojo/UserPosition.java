package org.entcore.directory.pojo;

public class UserPosition {
	private final String id;
	private final String name;
	private final UserPositionSource source;

	public UserPosition(String id, String name, UserPositionSource source) {
		this.id = id;
		this.name = name;
		this.source = source;
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
}
