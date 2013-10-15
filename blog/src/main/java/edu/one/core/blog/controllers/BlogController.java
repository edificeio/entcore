package edu.one.core.blog.controllers;

import edu.one.core.blog.services.BlogService;
import edu.one.core.blog.services.impl.DefaultBlogService;
import edu.one.core.infra.Controller;
import java.util.Map;

import edu.one.core.infra.Either;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.Utils;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.ActionType;
import edu.one.core.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class BlogController extends Controller {

	private final BlogService blog;

	public BlogController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions,
		MongoDb mongo) {
		super(vertx, container, rm, securedActions);
		this.blog = new DefaultBlogService(mongo);
	}

	@SecuredAction("blog.view")
	public void blog(HttpServerRequest request) {
		renderView(request);
	}

	// TODO improve fields matcher and validater
	@SecuredAction("blog.create")
	public void create(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.expectMultiPart(true);
					request.endHandler(new VoidHandler() {
						@Override
						protected void handle() {
							blog.create(Utils.jsonFromMultimap(request.formAttributes()), user,
									defaultResponseHandler(request));
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				blog.update(blogId, Utils.jsonFromMultimap(request.formAttributes()),
						defaultResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		blog.delete(blogId, defaultResponseHandler(request, 204));
	}

	@SecuredAction(value = "blog.read", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		blog.get(blogId, defaultResponseHandler(request));
	}

	@SecuredAction("blog.list")
	public void list(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					blog.list(user, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> event) {
							if (event.isRight()) {
								renderJson(request, event.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.putString("error", event.left().getValue());
								renderJson(request, error, 400);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private Handler<Either<String, JsonObject>> defaultResponseHandler(final HttpServerRequest request) {
		return defaultResponseHandler(request, 200);
	}

	private Handler<Either<String, JsonObject>> defaultResponseHandler(final HttpServerRequest request,
		final int successCode) {
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					renderJson(request, event.right().getValue(), successCode);
				} else {
					JsonObject error = new JsonObject()
							.putString("error", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		};
	}

}
