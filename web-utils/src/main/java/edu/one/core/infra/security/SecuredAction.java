package edu.one.core.infra.security;

public class SecuredAction {

	private final String name;
	private final String displayName;
	private final String type;

	public SecuredAction(String name, String displayName, String type) {
		this.name = name;
		this.displayName = displayName;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getType() {
		return type;
	}

}
