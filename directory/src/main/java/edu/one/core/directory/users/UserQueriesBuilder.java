package edu.one.core.directory.users;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class UserQueriesBuilder {

	private final JsonArray queries = new JsonArray();

	private static String createEntity(JsonObject json) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE (c { ");
		for (String attr : json.getFieldNames()) {
			sb.append(attr + ":{" + attr + "}, ");
		}
		sb.delete(sb.lastIndexOf(","), sb.length());
		return sb.toString() + "})";
	}

	public UserQueriesBuilder createUser(JsonObject row) {
		String id = row.getString("id");
		String login = row.getString("ENTPersonLogin");
		if (id == null || login == null) {
			throw new IllegalArgumentException("Invalid user : " + row.encode());
		}
		queries.add(new JsonObject()
		.putString("query", createEntity(row))
		.putObject("params", row));
		userLoginUnicity(id, login);
		return this;
	}

	private void userLoginUnicity(String id, String login) {
		String loginUnicity =
			"START m=node:node_auto_index({login}), " +
			"n=node:node_auto_index(id={nodeId}) " +
			"WITH count(m) as nb, n " +
			"WHERE nb > 1 " +
			"SET n.ENTPersonLogin = n.ENTPersonLogin + nb ";
		queries.add(toJsonObject(loginUnicity, new JsonObject()
		.putString("nodeId", id)
		.putString("login", "ENTPersonLogin:" + login + "*")));
	}

	public UserQueriesBuilder linkClass(String userId, String classId) {
		String query =
				"START n=node:node_auto_index(id={userId}), m=node:node_auto_index(id={classId}) " +
				"WHERE has(n.type) AND has(m.type) AND " +
				"m.type = 'CLASSE' AND n.type IN ['ENSEIGNANT', 'ELEVE'] " +
				"CREATE UNIQUE n-[:APPARTIENT]->m ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("userId", userId)
		.putString("classId", classId)));
		return this;
	}

	public UserQueriesBuilder linkGroupProfils(String userId, String type) {
		String parent = "";
		if ("PERSRELELEVE".equals(type)) {
			parent = "<-[:EN_RELATION_AVEC]-e";
		}
		String query =
				"START n=node:node_auto_index(id={userId}) " +
				"MATCH n" + parent + "-[:APPARTIENT]->c<-[:DEPENDS]-gp " +
				"WHERE has(n.type) AND n.type = {type} AND has(c.type) AND c.type = 'CLASSE' " +
				"AND has(gp.type) AND gp.type = {groupType} " +
				"CREATE UNIQUE n-[:APPARTIENT]->gp ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("userId", userId)
		.putString("type", type)
		.putString("groupType", "GROUP_CLASSE_" + type)));
		String query2 =
				"START n=node:node_auto_index(id={userId}) " +
				"MATCH n" + parent + "-[:APPARTIENT]->c<-[:DEPENDS]-gc-[:DEPENDS]->gp " +
				"WHERE has(n.type) AND n.type = {type} AND has(c.type) AND c.type = 'CLASSE' " +
				"AND has(gp.type) AND gp.type = {groupType} " +
				"CREATE UNIQUE n-[:APPARTIENT]->gp ";
		queries.add(toJsonObject(query2, new JsonObject()
		.putString("userId", userId)
		.putString("type", type)
		.putString("groupType", "GROUP_ETABEDUCNAT_" + type)));
		return this;
	}

	public UserQueriesBuilder defaultCommunication(String userId, String type) {
		String query =
				"START n=node:node_auto_index(id={userId}) " +
				"MATCH n-[:APPARTIENT]->gp " +
				"WHERE has(n.type) AND n.type = {type} " +
				"AND has(gp.type) AND (gp.type = {groupClassType} OR gp.type = {groupSchoolType}) " +
				"CREATE UNIQUE n-[:COMMUNIQUE]->gp ";
		String query2 =
				"START n=node:node_auto_index(id={userId}) " +
				"MATCH n-[:APPARTIENT]->gp " +
				"WHERE has(n.type) AND n.type = {type} " +
				"AND has(gp.type) AND (gp.type = {groupClassType} OR gp.type = {groupSchoolType}) " +
				"CREATE UNIQUE n<-[:COMMUNIQUE]-gp ";
		JsonObject params = new JsonObject()
		.putString("userId", userId)
		.putString("type", type)
		.putString("groupClassType", "GROUP_CLASSE_" + type)
		.putString("groupSchoolType", "GROUP_ETABEDUCNAT_" + type);
		queries.add(toJsonObject(query, params));
		queries.add(toJsonObject(query2, params));
		return this;
	}

	private JsonObject toJsonObject(String query, JsonObject params) {
		return new JsonObject()
		.putString("query", query)
		.putObject("params", (params != null) ? params : new JsonObject());
	}

	public JsonArray build() {
		return queries;
	}

}
