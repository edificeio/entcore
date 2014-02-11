package org.entcore.directory.services;


import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface ClassService {

	List<String> CLASS_FIELDS = Arrays.asList("id", "name", "level");

	List<String> CLASS_REQUIRED_FIELDS = Arrays.asList("id", "name");

	List<String> UPDATE_CLASS_FIELDS = Arrays.asList("level", "name");

	void create(String schoolId, JsonObject c, Handler<Either<String, JsonObject>> result);

	void update(String classId, JsonObject c, Handler<Either<String, JsonObject>> result);

	void findUsers(String classId, UserService.UserType[] expectedTypes, Handler<Either<String, JsonArray>> results);

	void get(String classId, Handler<Either<String, JsonObject>> result);

	void addUser(String classId, String userId, UserInfos user, Handler<Either<String, JsonObject>> result);

}
