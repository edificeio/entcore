package edu.one.core.blog.controllers;

import static edu.one.core.common.http.response.DefaultResponseHandler.*;
import static edu.one.core.common.user.UserUtils.*;

import edu.one.core.blog.services.BlogService;
import edu.one.core.blog.services.BlogTimelineService;
import edu.one.core.blog.services.impl.DefaultBlogService;
import edu.one.core.blog.services.impl.DefaultBlogTimelineService;
import edu.one.core.common.http.request.ActionsUtils;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.common.share.ShareService;
import edu.one.core.common.share.impl.MongoDbShareService;
import edu.one.core.infra.*;

import java.util.*;

import edu.one.core.common.user.UserUtils;
import edu.one.core.common.user.UserInfos;
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
	private final BlogTimelineService timelineService;
	private final ShareService shareService;

	public BlogController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions,
		MongoDb mongo) {
		super(vertx, container, rm, securedActions);
		this.blog = new DefaultBlogService(mongo);
		this.timelineService = new DefaultBlogTimelineService(eb, container, new Neo(eb, log), mongo);
		final Map<String, List<String>> groupedActions = new HashMap<>();
		groupedActions.put("manager", loadManagerActions(securedActions.values()));
		this.shareService = new MongoDbShareService(eb, mongo, "blogs", securedActions, groupedActions);
	}

	@SecuredAction("blog.view")
	public void blog(HttpServerRequest request) {
		renderView(request);
	}

	// TODO improve fields matcher and validater
	@SecuredAction("blog.create")
	public void create(final HttpServerRequest request) {
		getUserInfos(eb, request, new Handler<UserInfos>() {
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
						new Handler<Either<String, JsonObject>>() {
							@Override
							public void handle(Either<String, JsonObject> event) {
								if (event.isRight()) {
									UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
										@Override
										public void handle(UserInfos user) {
											if (user != null) {
												timelineService.notifyUpdateBlog(request, blogId, user,
														getBlogUri(blogId));
											}
										}
									});
									renderJson(request, event.right().getValue());
								} else {
									JsonObject error = new JsonObject()
											.putString("error", event.left().getValue());
									renderJson(request, error, 400);
								}
							}
						});
			}
		});
	}

	private String getBlogUri(String blogId) {
		return container.config().getString("host", "http://localhost:8018") +
			pathPrefix + "?blog=" + blogId;
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		blog.delete(blogId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					renderJson(request, event.right().getValue(), 204);
				} else {
					JsonObject error = new JsonObject()
							.putString("error", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		});
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
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					blog.list(user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void shareJson(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					shareService.shareInfos(user.getUserId(), blogId, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void shareJsonSubmit(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> actions = request.formAttributes().getAll("actions");
				final String groupId = request.formAttributes().get("groupId");
				final String userId = request.formAttributes().get("userId");
				if (actions == null || actions.isEmpty()) {
					badRequest(request);
					return;
				}
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							Handler<Either<String, JsonObject>> r = new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> event) {
									if (event.isRight()) {
										JsonObject n = event.right().getValue().getObject("notify-timeline");
										if (n != null) {
											timelineService.notifyShare(
												request, blogId, user, new JsonArray().add(n), getBlogUri(blogId));
										}
										renderJson(request, event.right().getValue());
									} else {
										JsonObject error = new JsonObject()
												.putString("error", event.left().getValue());
										renderJson(request, error, 400);
									}
								}
							};
							if (groupId != null) {
								shareService.groupShare(user.getUserId(), groupId, blogId, actions, r);
							} else if (userId != null) {
								shareService.userShare(user.getUserId(), userId, blogId, actions, r);
							} else {
								badRequest(request);
							}
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void removeShare(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}

		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> actions = request.formAttributes().getAll("actions");
				final String groupId = request.formAttributes().get("groupId");
				final String userId = request.formAttributes().get("userId");
				if (groupId != null) {
					shareService.removeGroupShare(groupId, blogId, actions, defaultResponseHandler(request));
				} else if (userId != null) {
					shareService.removeUserShare(userId, blogId, actions, defaultResponseHandler(request));
				} else {
					badRequest(request);
				}
			}
		});
	}

	private List<String> loadManagerActions(Collection<edu.one.core.infra.security.SecuredAction> actions) {
		List<String> managerActions = new ArrayList<>();
		if (actions != null) {
			for (edu.one.core.infra.security.SecuredAction a: actions) {
				if (a.getName() != null && "RESOURCE".equals(a.getType()) &&
						"blog.manager".equals(a.getDisplayName())) {
					managerActions.add(a.getName().replaceAll("\\.", "-"));
				}
			}
		}
		return  managerActions;
	}


	@SecuredAction(value = "blog.habilitation", type = ActionType.AUTHENTICATED)
	public void getActionsInfos(final HttpServerRequest request) {
		ActionsUtils.findWorkflowSecureActions(eb, request, this);
	}

}
