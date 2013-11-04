package edu.one.core.blog.controllers;

import static edu.one.core.blog.controllers.BlogResponseHandler.*;
import static edu.one.core.common.user.UserUtils.*;

import edu.one.core.blog.services.BlogService;
import edu.one.core.blog.services.BlogTimelineService;
import edu.one.core.blog.services.impl.DefaultBlogService;
import edu.one.core.blog.services.impl.DefaultBlogTimelineService;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.common.share.ShareService;
import edu.one.core.common.share.impl.MongoDbShareService;
import edu.one.core.infra.*;

import java.util.*;

import edu.one.core.common.user.UserUtils;
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
	private final BlogTimelineService timelineService;
	private final List<String> managerActions;
	private final Map<String, List<String>> groupedActions;
	private final ShareService shareService;

	public BlogController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions,
		MongoDb mongo) {
		super(vertx, container, rm, securedActions);
		this.blog = new DefaultBlogService(mongo);
		this.timelineService = new DefaultBlogTimelineService(eb, container, new Neo(eb, log), mongo);
		this.managerActions = loadManagerActions(securedActions.values());
		this.groupedActions = new HashMap<>();
		this.groupedActions.put("manager", managerActions);
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
					timelineService.deletedBlog(blogId);
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
	public void share(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		if (blogId == null || blogId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		blog.shared(blogId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight() &&  event.right().getValue().getArray("shared") != null) {
					JsonArray shared = event.right().getValue().getArray("shared");
					List<String> checked = new ArrayList<>();
					if (shared != null && shared.size() > 0) {
						for (Object o : shared) {
							JsonObject userShared = (JsonObject) o;
							String userOrGroupId = userShared.getString("groupId",
									userShared.getString("userId"));
							for (String attrName : userShared.getFieldNames()) {
								if ("userId".equals(attrName) || "groupId".equals(attrName)) {
									continue;
								}
								if ("manager".equals(attrName)) {
									for (String m: managerActions) {
										checked.add(m + "_" + userOrGroupId);
									}
									continue;
								}
								if (userShared.getBoolean(attrName, false)) {
									checked.add(attrName + "_" + userOrGroupId);
								}
							}
						}
					}
					shareUserAndGroupResource(request, blogId, checked);
				} else {
					notFound(request);
				}
			}
		});
	}

	@SecuredAction(value = "blog.manager", type = ActionType.RESOURCE)
	public void shareSubmit(final HttpServerRequest request) {
		final String blogId = request.params().get("blogId");
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final String id = request.formAttributes().get("resourceId");
				if (blogId == null || blogId.trim().isEmpty() || !blogId.equals(id)) {
					badRequest(request);
					return;
				}
				findVisibleUsers(eb, request, new Handler<JsonArray>() {
					@Override
					public void handle(final JsonArray visibleUsers) {
						findVisibleProfilsGroups(eb, request, new Handler<JsonArray>() {
							@Override
							public void handle(JsonArray visibleGroups) {
								final List<String> shares = request.formAttributes().getAll("shares");
								final List<String> shareGroups = request.formAttributes().getAll("shareGroups");
								final List<String> visibleGroupsIds = new ArrayList<>();
								for (int i = 0; i < visibleGroups.size(); i++) {
									JsonObject j = visibleGroups.get(i);
									if (j != null && j.getString("id") != null) {
										visibleGroupsIds.add(j.getString("id"));
									}
								}
								final List<String> visibleUsersIds = new ArrayList<>();
								for (int i = 0; i < visibleUsers.size(); i++) {
									JsonObject j = visibleUsers.get(i);
									if (j != null && j.getString("id") != null) {
										visibleUsersIds.add(j.getString("id"));
									}
								}
								Map<String, JsonObject> sharesMap = new HashMap<>();
								for (String share : shares) {
									String[] s = share.split("_");
									if (s.length != 2) continue;
									String[] actions = s[0].split(",");
									if (actions.length < 1) continue;
									if (!visibleUsersIds.contains(s[1])) continue;
									if (Arrays.asList(actions).containsAll(managerActions)) {
										JsonObject j = sharesMap.get(s[1]);
										if (j == null) {
											j = new JsonObject().putString("userId", s[1]);
											sharesMap.put(s[1], j);
										}
										j.putBoolean("manager", true);
									} else {
										for (int i = 0; i < actions.length; i++) {
											JsonObject j = sharesMap.get(s[1]);
											if (j == null) {
												j = new JsonObject().putString("userId", s[1]);
												sharesMap.put(s[1], j);
											}
											j.putBoolean(actions[i].replaceAll("\\.", "-"), true);
										}
									}
								}
								for (String shareGroup : shareGroups) {
									String[] s = shareGroup.split("_");
									if (s.length != 2) continue;
									String[] actions = s[0].split(",");
									if (actions.length < 1) continue;
									if (!visibleGroupsIds.contains(s[1])) continue;
									if (Arrays.asList(actions).containsAll(managerActions)) {
										JsonObject j = sharesMap.get(s[1]);
										if (j == null) {
											j = new JsonObject().putString("groupId", s[1]);
											sharesMap.put(s[1], j);
										}
										j.putBoolean("manager", true);
									} else {
										for (int i = 0; i < actions.length; i++) {
											JsonObject j = sharesMap.get(s[1]);
											if (j == null) {
												j = new JsonObject().putString("groupId", s[1]);
												sharesMap.put(s[1], j);
											}
											j.putBoolean(actions[i].replaceAll("\\.", "-"), true);
										}
									}
								}
								final JsonArray sharedArray = new JsonArray();
								for (JsonObject jo : sharesMap.values()) {
									sharedArray.add(jo);
								}
								visibleGroupsIds.addAll(visibleUsersIds);
								blog.share(blogId, sharedArray, visibleGroupsIds, new Handler<Either<String, JsonObject>>() {
									@Override
									public void handle(Either<String, JsonObject> event) {
										if (event.isRight()) {
											getUserInfos(eb, request, new Handler<UserInfos>() {
												@Override
												public void handle(UserInfos user) {
													timelineService.notifyShare(request
														, blogId, user, sharedArray, getBlogUri(blogId));
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
				});
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

}
