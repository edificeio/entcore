package edu.one.core.blog.security;

import com.mongodb.QueryBuilder;
import edu.one.core.blog.controllers.BlogController;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.MongoQueryBuilder;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class BlogResourcesProvider implements ResourcesProvider {

	private MongoDb mongo;

	public BlogResourcesProvider(MongoDb mongo) {
		this.mongo = mongo;
	}

	@Override
	public void authorize(HttpServerRequest request, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		String method = binding.getServiceMethod()
				.substring(BlogController.class.getName().length() + 1);
		switch (method) {
			case "update":
			case "delete":
			case "get":
				authorizeBlog(request, user, binding.getServiceMethod(), handler);
				break;
			default:
				handler.handle(false);
		}
	}

	private void authorizeBlog(HttpServerRequest request,
			UserInfos user, String serviceMethod, Handler<Boolean> handler) {
		String id = request.params().get("blogId");
		if (id != null && !id.trim().isEmpty()) {
			QueryBuilder query = QueryBuilder.start("_id").is(id).put("shared").elemMatch(
				QueryBuilder.start("userId").is(user.getUserId()).or(
					QueryBuilder.start(serviceMethod.replaceAll("\\.", "-")).is(true).get(),
					QueryBuilder.start("manager").is(true).get()
				).get()
			);
			executeCountQuery(request, "blogs", MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
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
