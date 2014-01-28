package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.services.SchoolService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultSchoolService implements SchoolService {

	private final Neo neo;

	public DefaultSchoolService(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void create(JsonObject school, Handler<Either<String, JsonObject>> result) {
		if (school == null) {
			school = new JsonObject();
		}
		school.putString("id", UUID.randomUUID().toString());
		JsonObject s = Utils.validAndGet(school, SCHOOL_FIELDS, SCHOOL_REQUIRED_FIELDS);
		if (validationError(s, result)) return;
		String query =
				"CREATE (s:School {props})," +
				"s<-[:DEPENDS]-(spg:ProfileGroup:Visible:SchoolProfileGroup:SchoolStudentGroup {studentGroup})," +
				"s<-[:DEPENDS]-(tpg:ProfileGroup:Visible:SchoolProfileGroup:SchoolTeacherGroup {teacherGroup})," +
				"s<-[:DEPENDS]-(rpg:ProfileGroup:Visible:SchoolProfileGroup:SchoolRelativeGroup {relativeGroup})," +
				"s<-[:DEPENDS]-(ppg:ProfileGroup:Visible:SchoolProfileGroup:SchoolPrincipalGroup {principalGroup}) " +
				"RETURN s.id as id";
		final String schoolName = s.getString("name");
		JsonObject params = new JsonObject()
				.putObject("props", s)
				.putObject("studentGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", schoolName + "_ELEVE")
				).putObject("teacherGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", schoolName + "_ENSEIGNANT")
				).putObject("relativeGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", schoolName + "_PERSRELELEVE")
				).putObject("principalGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", schoolName + "_DIRECTEUR")
				);
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void get(String id, Handler<Either<String, JsonObject>> result) {
		String query = "match (s:`School`) where s.id = {id} return s.id as id, s.UAI as UAI, s.name as name";
		neo.execute(query, new JsonObject().putString("id", id), validUniqueResultHandler(result));
	}

	private boolean validationError(JsonObject c,
		Handler<Either<String, JsonObject>> result, String ... params) {
		if (c == null) {
			result.handle(new Either.Left<String, JsonObject>("school.invalid.fields"));
			return true;
		}
		return validationParamsError(result, params);
	}

	private boolean validationParamsError(Handler<Either<String, JsonObject>> result, String ... params) {
		if (params.length > 0) {
			for (String s : params) {
				if (s == null) {
					result.handle(new Either.Left<String, JsonObject>("school.invalid.parameter"));
					return true;
				}
			}
		}
		return false;
	}

}
