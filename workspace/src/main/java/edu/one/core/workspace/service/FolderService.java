package edu.one.core.workspace.service;

import edu.one.core.infra.Either;
import edu.one.core.infra.security.resources.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface FolderService {

	void create(String name, String path, String application, UserInfos author,
				Handler<Either<String, JsonObject>> result);

	void move(String id, String path, UserInfos author, Handler<Either<String, JsonObject>> result);

	void copy(String id, String name, String path, UserInfos author,
				Handler<Either<String, JsonObject>> result);

	void trash(String id, UserInfos author, Handler<Either<String, JsonObject>> result);

	void delete(String id, UserInfos author, Handler<Either<String, JsonObject>> result);

	void list(String name, UserInfos author, boolean hierarchical,
				Handler<Either<String, JsonArray>> results);

	void restore(String id, UserInfos author, Handler<Either<String, JsonObject>> result);

}
