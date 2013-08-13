package edu.one.core.communication.profils;

import org.vertx.java.core.json.JsonObject;

public class ProfilFactory {

	public GroupProfil getGroupProfil(JsonObject groupProfil) {
		if (groupProfil != null && groupProfil.getString("type") != null) {
			return new GroupProfil(
					groupProfil.getString("id"),
					groupProfil.getString("name"),
					groupProfil.getString("type"));
		}
		throw new IllegalArgumentException("Invalid group profil : " + groupProfil);
	}

}
