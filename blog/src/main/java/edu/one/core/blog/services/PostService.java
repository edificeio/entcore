package edu.one.core.blog.services;

import edu.one.core.infra.Either;
import edu.one.core.infra.security.resources.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface PostService {

	enum StateType { DRAFT, SUBMITTED, PUBLISHED };

	List<String> FIELDS = Arrays.asList("author", "title", "content",
			"blog", "state", "comments", "created", "modified", "views");

	List<String> UPDATABLE_FIELDS = Arrays.asList("title", "content", "modified");

	void create(String blogId, JsonObject post, UserInfos author, Handler<Either<String, JsonObject>> result);

	void update(String postId, JsonObject post, Handler<Either<String, JsonObject>> result);

	void delete(String postId, Handler<Either<String, JsonObject>> result);

	void get(String postId, StateType state, Handler<Either<String, JsonObject>> result);

	void list(String blogId, StateType state, UserInfos user, Handler<Either<String, JsonArray>> result);

	void submit(String postId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void publish(String postId, Handler<Either<String, JsonObject>> result);

	void unpublish(String postId, Handler<Either<String, JsonObject>> result);

}
