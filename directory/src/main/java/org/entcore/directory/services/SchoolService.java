package org.entcore.directory.services;

import edu.one.core.infra.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface SchoolService {

	List<String> SCHOOL_FIELDS = Arrays.asList("id", "name", "UAI");

	List<String> SCHOOL_REQUIRED_FIELDS = Arrays.asList("id", "name");

	void create(JsonObject school, Handler<Either<String, JsonObject>> result);

	void get(String id, Handler<Either<String, JsonObject>> result);

	void getByClassId(String classId, Handler<Either<String, JsonObject>> result);

}
