package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface SchoolService {

	void create(JsonObject school, Handler<Either<String, JsonObject>> result);

	void get(String id, Handler<Either<String, JsonObject>> result);

	void getByClassId(String classId, Handler<Either<String, JsonObject>> result);

}
