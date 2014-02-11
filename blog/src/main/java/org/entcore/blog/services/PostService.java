package org.entcore.blog.services;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
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

	void get(String blogId, String postId, StateType state, Handler<Either<String, JsonObject>> result);

	void list(String blogId, StateType state, UserInfos user, Handler<Either<String, JsonArray>> result);

	void submit(String blogId, String postId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void publish(String blogId, String postId, Handler<Either<String, JsonObject>> result);

	void unpublish(String postId, Handler<Either<String, JsonObject>> result);

	void addComment(String blogId, String postId, String comment, UserInfos author,
			Handler<Either<String, JsonObject>> result);

	void deleteComment(String blogId, String commentId, UserInfos author, Handler<Either<String, JsonObject>> result);

	void listComment(String blogId, String postId, UserInfos author, Handler<Either<String, JsonArray>> result);

	void publishComment(String blogId, String commentId, Handler<Either<String, JsonObject>> result);

}
