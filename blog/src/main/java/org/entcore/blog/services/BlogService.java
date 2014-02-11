package org.entcore.blog.services;


import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface BlogService {

	enum CommentType { NONE, IMMEDIATE, RESTRAINT };

	enum PublishType { IMMEDIATE, RESTRAINT };

	List<String> FIELDS = Arrays.asList("author", "title", "description",
			"thumbnail", "comment-type", "created", "modified", "shared", "publish-type");

	List<String> UPDATABLE_FIELDS = Arrays.asList("title", "description",
			"thumbnail", "comment-type", "modified", "publish-type");

	void create(JsonObject blog, UserInfos author, Handler<Either<String, JsonObject>> result);

	void update(String blogId, JsonObject blog, Handler<Either<String, JsonObject>> result);

	void delete(String blogId, Handler<Either<String, JsonObject>> result);

	void get(String blogId, Handler<Either<String, JsonObject>> result);

	void list(UserInfos user, Handler<Either<String, JsonArray>> result);

}
