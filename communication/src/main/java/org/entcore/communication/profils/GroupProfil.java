package org.entcore.communication.profils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.collections.Joiner;

public class GroupProfil {

	private final String id;
	private final String name;
	private final String type;

	public GroupProfil(String id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public JsonElement queryAddCommunicationLink(GroupProfil groupProfil) {
		if (groupProfil != null) {
			String query;
			Map<String, Object> params = new HashMap<>();
			if (getId() != null && getId().equals(groupProfil.getId())) {
				// communication au sein du groupe
				query =
					"MATCH (n:ProfileGroup)<-[:IN]-(m:User) " +
					"WHERE n.id = {id} " +
					"CREATE UNIQUE m-[:COMMUNIQUE]->n ";
				String query2 =
						"MATCH (n:ProfileGroup)<-[:IN]-(m:User) " +
						"WHERE n.id = {id} " +
						"CREATE UNIQUE m<-[:COMMUNIQUE]-n ";
				params.put("id", getId());
				return new JsonArray()
					.addObject(toJsonObject(query, params))
					.addObject(toJsonObject(query2, params));
			} else {
				// communication entre deux groupes
				query =
					"MATCH (n:ProfileGroup), (m:ProfileGroup) " +
					"WHERE n.id = {id1} AND m.id = {id2} " +
					"CREATE UNIQUE n-[:COMMUNIQUE]->m";
				params.put("id1", getId());
				params.put("id2", groupProfil.getId());
				return toJsonObject(query, params);
			}
		}
		return null;
	}

	public static JsonArray queryParentEnfantCommunication(List<String> groupsIds) {
		JsonArray queries = new JsonArray();
		if (groupsIds != null) {
			Map<String, Object> params = new HashMap<>();
			String query =
					"MATCH (n:ProfileGroup)<-[:IN]-(e:Student)-[:RELATED]->(p:Relative) " +
					"WHERE n.id IN ['" + Joiner.on("','").join(groupsIds) + "'] " +
					"CREATE UNIQUE e-[:COMMUNIQUE_DIRECT]->p";
			String query2 =
					"MATCH (n:ProfileGroup)<-[:IN]-(e:Student)-[:RELATED]->(p:Relative) " +
					"WHERE n.id IN ['" + Joiner.on("','").join(groupsIds) + "'] " +
					"CREATE UNIQUE e<-[:COMMUNIQUE_DIRECT]-p";
			queries.addObject(toJsonObject(query, params)).addObject(toJsonObject(query2, params));
		}
		return queries;
	}

	protected static JsonObject toJsonObject(String query, Map<String, Object> params) {
		return new JsonObject()
				.putString("query", query)
				.putObject("params", (params != null) ? new JsonObject(params) : new JsonObject());
	}

}
