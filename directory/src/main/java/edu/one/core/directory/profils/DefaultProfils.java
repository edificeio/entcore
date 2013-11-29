package edu.one.core.directory.profils;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Joiner;

import edu.one.core.common.neo4j.Neo;

public class DefaultProfils implements Profils {

	private final Neo neo;

	public DefaultProfils(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void listGroupsProfils(Object [] typeFilter, String schoolId,
			final Handler<JsonObject> handler) {
		Map<String, Object> params = new HashMap<>();
		String typesProfileGroup;
		if (typeFilter != null && typeFilter.length > 0) {
			typesProfileGroup =  "n:" + Joiner.on(" OR n:").join(typeFilter);
		} else {
			typesProfileGroup = "n:ProfileGroup";
		}
		String query;
		if (schoolId != null && !schoolId.trim().isEmpty()) {
			query = "MATCH n-[:DEPENDS*1..2]->(m:School) " +
					"WHERE (" + typesProfileGroup + ") AND m.id = {schoolId} ";
			params.put("schoolId", schoolId);
		} else {
			query = "MATCH n-[:DEPENDS*1..2]->(m:School) " +
					"WHERE (" + typesProfileGroup + ") AND m.id = {schoolId} ";
		}
		query += "RETURN distinct n.name as name, n.id as id, " +
				"HEAD(filter(x IN labels(m) WHERE x <> 'ProfileGroup' " +
				"AND x <> 'ClassProfileGroup' AND x <> 'SchoolProfileGroup')) as type";
		neo.send(query, params, new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					handler.handle(res.body());
				}
			}
		);
	}

}
