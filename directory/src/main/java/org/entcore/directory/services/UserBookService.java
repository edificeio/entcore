package org.entcore.directory.services;


import edu.one.core.infra.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface UserBookService {

	List<String> UPDATE_USERBOOK_FIELDS = Arrays.asList("health", "mood", "picture", "motto");

	void update(String userId, JsonObject userBook, Handler<Either<String, JsonObject>> result);

	void get(String userId, Handler<Either<String, JsonObject>> result);

}
