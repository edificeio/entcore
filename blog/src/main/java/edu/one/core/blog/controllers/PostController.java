package edu.one.core.blog.controllers;

import static edu.one.core.blog.controllers.BlogResponseHandler.*;

import edu.one.core.blog.security.BlogResourcesProvider;
import edu.one.core.blog.services.PostService;
import edu.one.core.blog.services.impl.DefaultPostService;
import edu.one.core.infra.Controller;
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
import org.vertx.java.platform.Container;

import java.util.Map;

public class PostController extends Controller {

	private final PostService post;

	public PostController(Vertx vertx, Container container,
						  RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions,
						  MongoDb mongo) {
		super(vertx, container, rm, securedActions);
		this.post = new DefaultPostService(mongo);
	}

	// TODO improve fields matcher and validater
	@SecuredAction(value = "blog.contrib", type = ActionType.RESOURCE)
	public void create(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.expectMultiPart(true);
					request.endHandler(new VoidHandler() {
						@Override
						protected void handle() {
							post.create(blogId, Utils.jsonFromMultimap(request.formAttributes()), user,
									defaultResponseHandler(request));
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.contrib", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				post.update(postId, Utils.jsonFromMultimap(request.formAttributes()),
						defaultResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "blog.contrib", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		post.delete(postId, defaultResponseHandler(request, 204));
	}

	@SecuredAction(value = "blog.read", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		post.get(postId, BlogResourcesProvider.getStateType(request), defaultResponseHandler(request));
	}

	@SecuredAction(value = "blog.read", type = ActionType.RESOURCE)
	public void list(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					post.list(blogId, BlogResourcesProvider.getStateType(request),
							user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.contrib", type = ActionType.RESOURCE)
	public void submit(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					post.submit(postId, user, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void publish(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		post.publish(postId, defaultResponseHandler(request));
	}

	@SecuredAction(value = "blog.contrib", type = ActionType.RESOURCE)
	public void unpublish(final HttpServerRequest request) {
		final String postId = request.params().get("postId");
		if (postId == null || postId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		post.unpublish(postId, defaultResponseHandler(request));
	}

}
