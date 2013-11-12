package edu.one.core.blog.security;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import edu.one.core.blog.controllers.BlogController;
import edu.one.core.blog.controllers.PostController;
import edu.one.core.blog.services.PostService;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.MongoQueryBuilder;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class BlogResourcesProvider implements ResourcesProvider {

	private MongoDb mongo;

	public BlogResourcesProvider(MongoDb mongo) {
		this.mongo = mongo;
	}

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		final String serviceMethod = binding.getServiceMethod();
		if (serviceMethod != null && serviceMethod.startsWith(BlogController.class.getName())) {
			String method = serviceMethod
					.substring(BlogController.class.getName().length() + 1);
			switch (method) {
				case "update":
				case "delete":
				case "get":
				case "shareJson":
				case "shareJsonSubmit":
				case "removeShare":
					authorizeBlog(request, user, binding.getServiceMethod(), handler);
					break;
				default:
					handler.handle(false);
			}
		} else if (serviceMethod != null && serviceMethod.startsWith(PostController.class.getName())) {
			String method = serviceMethod
					.substring(PostController.class.getName().length() + 1);
			switch (method) {
				case "get":
					authorizeGetPost(request, user, binding.getServiceMethod(), handler);
					break;
				case "list":
				case "create":
				case "submit":
				case "publish":
				case "comments":
				case "comment":
				case "deleteComment":
				case "publishComment":
					authorizeBlog(request, user, binding.getServiceMethod(), handler);
					break;
				case "update":
				case "delete":
				case "unpublish":
					authorizeUpdateDeletePost(request, user, binding.getServiceMethod(), handler);
					break;
				default:
					handler.handle(false);
			}
		} else {
			handler.handle(false);
		}
	}

	private void authorizeBlog(HttpServerRequest request,
							   UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("blogId");
		if (id != null && !id.trim().isEmpty()) {
			QueryBuilder query = getDefaultQueryBuilder(user, serviceMethod, id);
			executeCountQuery(request, "blogs", MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	private QueryBuilder getDefaultQueryBuilder(UserInfos user, String serviceMethod, String id) {
		List<DBObject> groups = new ArrayList<>();
		groups.add(QueryBuilder.start("userId").is(user.getUserId())
				.put(serviceMethod.replaceAll("\\.", "-")).is(true).get());
		groups.add(QueryBuilder.start("userId").is(user.getUserId())
				.put("manager").is(true).get());
		for (String gpId: user.getProfilGroupsIds()) {
			groups.add(QueryBuilder.start("groupId").is(gpId)
					.put(serviceMethod.replaceAll("\\.", "-")).is(true).get());
			groups.add(QueryBuilder.start("groupId").is(gpId)
					.put("manager").is(true).get());
		}
		return QueryBuilder.start("_id").is(id).put("shared").elemMatch(
			new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
		);
	}

	private void authorizeUpdateDeletePost(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String postId = request.params().get("postId");
		if (postId != null && !postId.trim().isEmpty()) {
			checkContribResource(request, user, handler, postId);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeGetPost(HttpServerRequest request,
			final UserInfos user, String serviceMethod, final Handler<Boolean> handler) {
		String blogId = request.params().get("blogId");
		String postId = request.params().get("postId");
		if (blogId != null && !blogId.trim().isEmpty() &&
				postId != null && !postId.trim().isEmpty()) {
			PostService.StateType state = getStateType(request);
			if (PostService.StateType.PUBLISHED.equals(state)) {
				QueryBuilder query = getDefaultQueryBuilder(user, serviceMethod, blogId);
				executeCountQuery(request, "blogs", MongoQueryBuilder.build(query), 1, handler);
			} else {
				checkContribResource(request, user, handler, postId);
			}
		} else {
			handler.handle(false);
		}
	}

	private void checkContribResource(final HttpServerRequest request,
				final UserInfos user, final Handler<Boolean> handler, String postId) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId);
		request.pause();
		mongo.findOne("posts", MongoQueryBuilder.build(query), null, new JsonArray().addString("blog"),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				if ("ok".equals(event.body().getString("status"))) {
					JsonObject res = event.body().getObject("result");
					if (res == null) {
						handler.handle(false);
						return;
					}
					if (res.getObject("author") != null  &&
							user.getUserId().equals(res.getObject("author").getString("userId"))) {
						handler.handle(true);
						return;
					}
					if (res.getObject("blog") != null  &&
							res.getObject("blog").getArray("shared") != null) {
						for (Object o: res.getObject("blog").getArray("shared")) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject json = (JsonObject) o;
							if (json != null && json.getBoolean("manager", false) &&
									(user.getUserId().equals(json.getString("userId")) ||
											user.getProfilGroupsIds().contains(json.getString("groupId")))) {
								handler.handle(true);
								return;
							}
						}
					}
					handler.handle(false);
				}
			}
		});
	}

	public static PostService.StateType getStateType(HttpServerRequest request) {
		String s = request.params().get("state");
		PostService.StateType state;
		if (s == null || s.trim().isEmpty()) {
			state = PostService.StateType.PUBLISHED;
		} else {
			try {
				state = PostService.StateType.valueOf(s.toUpperCase());
			} catch (IllegalArgumentException | NullPointerException e) {
				state = PostService.StateType.PUBLISHED;
			}
		}
		return state;
	}

	private void executeCountQuery(final HttpServerRequest request, String collection,
			JsonObject query, final int expectedCountResult,
			final Handler<Boolean> handler) {
		request.pause();
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonObject res = event.body();
				handler.handle(
					res != null &&
					"ok".equals(res.getString("status")) &&
					expectedCountResult == res.getInteger("count")
				);
			}
		});
	}

}
