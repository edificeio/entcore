package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.directory.services.ClassService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultClassService implements ClassService {

	private final Neo neo;

	public DefaultClassService(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void create(String schoolId, JsonObject classe, Handler<Either<String, JsonObject>> result) {
		if (classe == null) {
			classe = new JsonObject();
		}
		String classId = UUID.randomUUID().toString();
		classe.putString("id", classId);
		JsonObject c = Utils.validAndGet(classe, CLASS_FIELDS, CLASS_REQUIRED_FIELDS);
		if (validationError(c, result)) return;
		String query =
				"MATCH (n:`School` { id : {schoolId}}) " +
				"CREATE n<-[:APPARTIENT]-(c:Class {props})," +
				"c<-[:DEPENDS]-(spg:ProfileGroup:Visible:ClassProfileGroup:ClassStudentGroup {studentGroup})," +
				"c<-[:DEPENDS]-(tpg:ProfileGroup:Visible:ClassProfileGroup:ClassTeacherGroup {teacherGroup})," +
				"c<-[:DEPENDS]-(rpg:ProfileGroup:Visible:ClassProfileGroup:ClassRelativeGroup {relativeGroup})";
		final String className = c.getString("name");
		JsonObject params = new JsonObject()
				.putString("schoolId", schoolId)
				.putObject("props", c)
				.putObject("studentGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_ELEVE")
				).putObject("teacherGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_ENSEIGNANT")
				).putObject("relativeGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_PERSRELELEVE")
				);
		JsonObject p = new JsonObject().putString("schoolId", schoolId).putString("classId", classId);
		StatementsBuilder queries = new StatementsBuilder()
				.add(query, params)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolStudentGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassStudentGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolTeacherGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassTeacherGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolRelativeGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassRelativeGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p);
		neo.executeTransaction(queries.build(), null, true, validUniqueResultHandler(result));
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
