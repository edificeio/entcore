package org.entcore.directory.services;


import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface ClassService {

	void create(String schoolId, JsonObject c, Handler<Either<String, JsonObject>> result);

	void update(String classId, JsonObject c, Handler<Either<String, JsonObject>> result);

	void findUsers(String classId, JsonArray expectedTypes, Handler<Either<String, JsonArray>> results);

	void get(String classId, Handler<Either<String, JsonObject>> result);

	void addUser(String classId, String userId, UserInfos user, Handler<Either<String, JsonObject>> result);

}
