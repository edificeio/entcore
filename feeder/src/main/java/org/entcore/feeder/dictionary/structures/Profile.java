/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.dictionary.structures;

import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Profile {

	protected final String id;
	protected final String externalId;
	protected final Importer importer = Importer.getInstance();
	protected JsonObject profile;
	protected final Set<String> functions = Collections.synchronizedSet(new HashSet<String>());

	protected Profile(JsonObject profile) {
		this(profile.getString("externalId"), profile);
	}

	protected Profile(JsonObject profile, JsonArray functions) {
		this(profile);
		if (functions != null) {
			for (Object o : functions) {
				if (!(o instanceof String)) continue;
				functions.add(o);
			}
		}
	}

	protected Profile(String externalId, JsonObject struct) {
		if (struct != null && externalId != null && externalId.equals(struct.getString("externalId"))) {
			this.id = struct.getString("id");
		} else {
			throw new IllegalArgumentException("Invalid structure with externalId : " + externalId);
		}
		this.externalId = externalId;
		this.profile = struct;
	}
	private TransactionHelper getTransaction() {
		return importer.getTransaction();
	}

	public void update(JsonObject struct) {
		if (this.profile.equals(struct)) {
			return;
		}
		String query =
				"MATCH (p:Profile { id : {id}}) " +
				"WITH p " +
				"WHERE p.checksum IS NULL OR p.checksum <> {checksum} " +
				"SET " + Neo4j.nodeSetPropertiesFromJson("p", struct, "id", "externalId");
		getTransaction().add(query, struct);
		this.profile = struct;
	}

	public void create() {
		String query =
				"CREATE (p:Profile {props})<-[:HAS_PROFILE]-(g:Group:ProfileGroup:DefaultProfileGroup) ";
		JsonObject params = new JsonObject()
				.putString("id", id)
				.putString("externalId", externalId)
				.putObject("props", profile);
		getTransaction().add(query, params);
	}

	public void createFunctionIfAbsent(String functionExternalId, String name) {
		if (functions.add(functionExternalId)) {
			String query =
					"MATCH (p:Profile { externalId : {profileExternalId}}) " +
					"CREATE p<-[:COMPOSE]-(f:Function {props}) ";
			JsonObject params = new JsonObject()
					.putString("profileExternalId", externalId)
					.putObject("props", new JsonObject()
							.putString("externalId", functionExternalId)
							.putString("id", UUID.randomUUID().toString())
							.putString("name", name)
					);
			getTransaction().add(query, params);
		}
	}

}
