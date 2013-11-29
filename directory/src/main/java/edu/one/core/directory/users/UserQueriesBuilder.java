package edu.one.core.directory.users;

import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Joiner;

public class UserQueriesBuilder {

	private final JsonArray queries = new JsonArray();

	private static String createEntity(JsonObject json, String type) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE (c:").append(type).append(" { ");
		for (String attr : json.getFieldNames()) {
			sb.append(attr).append(":{").append(attr).append("}, ");
		}
		sb.delete(sb.lastIndexOf(","), sb.length());
		return sb.toString() + "})";
	}

	public UserQueriesBuilder createUser(JsonObject row, String type) {
		String id = row.getString("id");
		String login = row.getString("login");
		if (id == null || login == null) {
			throw new IllegalArgumentException("Invalid user : " + row.encode());
		}
		queries.add(new JsonObject()
		.putString("query", createEntity(row, "User:" + type))
		.putObject("params", row));
		//userLoginUnicity(id, login);
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
				"MATCH (n), (m:Class) " +
				"WHERE (n:Student OR n:Teacher) AND n.id = {userId} AND m.id = {classId} " +
				"CREATE UNIQUE n-[:APPARTIENT]->m ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("userId", userId)
		.putString("classId", classId)));
		return this;
	}

	public UserQueriesBuilder linkSchool(String userId, String classId) {
		String query =
				"MATCH (m:Class)-[:APPARTIENT]->(s:School), (n) " +
				"WHERE  (n:Student OR n:Teacher) AND n.id = {userId} AND m.id = {classId} " +
				"CREATE UNIQUE n-[:APPARTIENT]->s ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("userId", userId)
		.putString("classId", classId)));
		return this;
	}

	public UserQueriesBuilder linkChildrens(String parentId, List<String> childrenIds) {
		String query =
				"MATCH (n:Student), (m:Relative) " +
				"WHERE m.id = {parentId} AND n.id IN ['" + Joiner.on("','").join(childrenIds) + "'] " +
				"CREATE UNIQUE n-[:EN_RELATION_AVEC]->m ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("parentId", parentId)));
		return this;
	}

	public UserQueriesBuilder linkGroupProfils(String userId, String type) {
		String parent = "";
		if ("Relative".equals(type)) {
			parent = "<-[:EN_RELATION_AVEC]-e";
		}
		String query =
				"MATCH (n:" + type + ")" + parent + "-[:APPARTIENT]->(c:Class)<-[:DEPENDS]-(gp:Class"
						+ type + "Group) " +
				"WHERE n.id = {userId} " +
				"CREATE UNIQUE n-[:APPARTIENT]->gp ";
		queries.add(toJsonObject(query, new JsonObject()
		.putString("userId", userId)));
		String query2 =
				"MATCH (n:" + type + ")" + parent + "-[:APPARTIENT]->(c:Class)<-[:DEPENDS]-(gc:ClassProfileGroup)" +
						"-[:DEPENDS]->(gp:School"+ type + "Group) " +
				"WHERE n.id = {userId} " +
				"CREATE UNIQUE n-[:APPARTIENT]->gp ";
		queries.add(toJsonObject(query2, new JsonObject()
		.putString("userId", userId)));
		return this;
	}

	public UserQueriesBuilder defaultCommunication(String userId, String type) {
		String query =
				"MATCH (n:" + type + ")-[:APPARTIENT]->(gp) " +
				"WHERE n.id = {userId} AND (gp:Class" + type + "Group OR gp:School" + type + "Group) " +
				"CREATE UNIQUE n-[:COMMUNIQUE]->gp ";
		String query2 =
				"MATCH (n:" + type + ")-[:APPARTIENT]->(gp) " +
				"WHERE n.id = {userId} AND (gp:Class" + type + "Group OR gp:School" + type + "Group) " +
				"CREATE UNIQUE n<-[:COMMUNIQUE]-gp ";
		JsonObject params = new JsonObject()
		.putString("userId", userId);
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
