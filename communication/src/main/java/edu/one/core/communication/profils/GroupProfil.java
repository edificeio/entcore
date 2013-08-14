package edu.one.core.communication.profils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Joiner;

//public abstract class GroupProfil {
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

	//public abstract JsonObject queryAddCommunicationLink(GroupProfil groupProfil);
	public JsonElement queryAddCommunicationLink(GroupProfil groupProfil) {
		if (groupProfil != null) {
			String query;
			Map<String, Object> params = new HashMap<>();
			if (getId() != null && getId().equals(groupProfil.getId())) {
				// communication au sein du groupe
				query =
					"START n=node:node_auto_index(id={id}) " +
					"MATCH n<-[:APPARTIENT]-m " +
					"CREATE UNIQUE m-[:COMMUNIQUE]->n ";
				String query2 =
						"START n=node:node_auto_index(id={id}) " +
						"MATCH n<-[:APPARTIENT]-m " +
						"CREATE UNIQUE m<-[:COMMUNIQUE]-n ";
				params.put("id", getId());
				return new JsonArray()
					.addObject(toJsonObject(query, params))
					.addObject(toJsonObject(query2, params));
			} else {
				// communication entre deux groupes
				query =
					"START n=node:node_auto_index(id={id1}), " +
					"m=node:node_auto_index(id={id2}) " +
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
			String query = "START n=node:node_auto_index({groupsIds}) " +
					"MATCH n<-[:APPARTIENT]-e-[:EN_RELATION_AVEC]->p " +
					"WHERE has(e.type) AND has(p.type) AND e.type = 'ELEVE' AND p.type = 'PERSRELELEVE' " +
					"CREATE UNIQUE e-[:COMMUNIQUE_DIRECT]->p";
			String query2 = "START n=node:node_auto_index({groupsIds}) " +
					"MATCH n<-[:APPARTIENT]-e-[:EN_RELATION_AVEC]->p " +
					"WHERE has(e.type) AND has(p.type) AND e.type = 'ELEVE' AND p.type = 'PERSRELELEVE' " +
					"CREATE UNIQUE e<-[:COMMUNIQUE_DIRECT]-p";
			params.put("groupsIds", "id:" + Joiner.on(" OR id:").join(groupsIds));
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
