/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.communication.profils;

import org.vertx.java.core.json.JsonObject;

public class ProfilFactory {

	public GroupProfil getGroupProfil(JsonObject groupProfil) {
		if (groupProfil != null) {
			return new GroupProfil(
					groupProfil.getString("id"),
					groupProfil.getString("name"), null);
		}
		throw new IllegalArgumentException("Invalid group profil : " + groupProfil);
	}

}
