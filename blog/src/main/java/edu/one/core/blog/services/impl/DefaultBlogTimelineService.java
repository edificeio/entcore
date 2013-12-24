package edu.one.core.blog.services.impl;

import com.google.common.base.Joiner;
import com.mongodb.QueryBuilder;
import edu.one.core.blog.services.BlogTimelineService;
import edu.one.core.common.notification.TimelineHelper;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.MongoQueryBuilder;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultBlogTimelineService implements BlogTimelineService {

	private static final Logger log = LoggerFactory.getLogger(DefaultBlogTimelineService.class);
	private static final String NOTIFICATION_TYPE = "BLOG";

	private final Neo neo;
	private final MongoDb mongo;
	private final TimelineHelper notification;
	private final Container container;

	public DefaultBlogTimelineService(Vertx vertx, EventBus eb, Container container, Neo neo, MongoDb mongo) {
		this.neo = neo;
		this.mongo = mongo;
		this.notification = new TimelineHelper(vertx, eb, container);
		this.container = container;
	}

	@Override
	public void notifyShare(final HttpServerRequest request, final String blogId, final UserInfos user,
			final JsonArray sharedArray, final String resourceUri) {
		if (sharedArray != null && user != null && blogId != null && request != null && resourceUri != null) {
			QueryBuilder query = QueryBuilder.start("_id").is(blogId);
			JsonObject keys = new JsonObject().putNumber("title", 1);
			mongo.findOne("blogs", MongoQueryBuilder.build(query), keys, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(final Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						List<String> shareIds = getSharedIds(sharedArray);
						if (!shareIds.isEmpty()) {
							Map<String, Object> params = new HashMap<>();
							params.put("userId", user.getUserId());
							neo.send(neoQuery(shareIds), params, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> res) {
									if ("ok".equals(res.body().getString("status"))) {
										JsonObject r = res.body().getObject("result");
										List<String> recipients = new ArrayList<>();
										for (String attr: r.getFieldNames()) {
											String id = r.getObject(attr).getString("id");
											if (id != null) {
												recipients.add(id);
											}
										}
										String blogTitle = event.body()
												.getObject("result", new JsonObject()).getString("title");
										JsonObject p = new JsonObject()
												.putString("uri", container.config().getString("userbook-host") +
														"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
												.putString("username", user.getUsername())
												.putString("blogTitle", blogTitle)
												.putString("resourceUri", resourceUri);
										try {
											notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
													NOTIFICATION_TYPE + "_SHARE", recipients,
													blogId, "notification/notify-share.html", p);
										} catch (IOException e) {
											log.error("Unable to send timeline notification", e);
										}
									}
								}
							});
						}
					}
				}
			});
		}
	}

	@Override
	public void notifyUpdateBlog(final HttpServerRequest request, final String blogId, final UserInfos user,
			final String resourceUri) {
		if (resourceUri != null && user != null && blogId != null && request != null) {
			QueryBuilder query = QueryBuilder.start("_id").is(blogId);
			JsonObject keys = new JsonObject().putNumber("shared", 1).putNumber("title", 1);
			findRecipiants("blogs", query, keys, null, user, new Handler<Map<String, Object>>() {
				@Override
				public void handle(Map<String, Object> event) {
					if (event != null) {
						List<String> recipients = (List<String>) event.get("recipients");
						JsonObject blog = (JsonObject) event.get("blog");
						if (recipients != null) {
							JsonObject p = new JsonObject()
									.putString("uri", container.config().getString("userbook-host") +
											"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
									.putString("username", user.getUsername())
									.putString("blogTitle", blog.getString("title"))
									.putString("resourceUri", resourceUri);
							try {
								notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
										NOTIFICATION_TYPE + "_UPDATE", recipients,
										blogId, "notification/notify-update-blog.html", p);
							} catch (IOException e) {
								log.error("Unable to send timeline notification", e);
							}
						}
					}
				}
			});
		}
	}

	@Override
	public void notifyPublishPost(final HttpServerRequest request, final String blogId, final String postId,
			final UserInfos user, final String resourceUri) {
		if (resourceUri != null && user != null && blogId != null && request != null) {
			QueryBuilder query = QueryBuilder.start("_id").is(postId);
			JsonObject keys = new JsonObject().putNumber("title", 1).putNumber("blog", 1);
			JsonArray fetch = new JsonArray().addString("blog");
			findRecipiants("posts", query, keys, fetch, user, new Handler<Map<String, Object>>() {
				@Override
				public void handle(Map<String, Object> event) {
					if (event != null) {
						List<String> recipients = (List<String>) event.get("recipients");
						JsonObject blog = (JsonObject) event.get("blog");
						if (recipients != null) {
							JsonObject p = new JsonObject()
									.putString("uri", container.config().getString("userbook-host") +
											"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
									.putString("username", user.getUsername())
									.putString("blogTitle", blog.getObject("blog",
											new JsonObject()).getString("title"))
									.putString("blogUri", resourceUri)
									.putString("postTitle", blog.getString("title"))
									.putString("postUri", resourceUri + "&post=" + postId);
							try {
								notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
										NOTIFICATION_TYPE + "_POST_PUBLISH", recipients,
										blogId, postId, "notification/notify-publish-post.html", p);
							} catch (IOException e) {
								log.error("Unable to send timeline notification", e);
							}
						}
					}
				}
			});
		}
	}

	private void findRecipiants(String collection, QueryBuilder query, JsonObject keys,
			final JsonArray fetch, final UserInfos user,
				final Handler<Map<String, Object>> handler) {
		mongo.findOne(collection, MongoQueryBuilder.build(query), keys, fetch, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					final JsonObject blog = event.body().getObject("result", new JsonObject());
					JsonArray shared;
					if (fetch == null) {
						shared = blog.getArray("shared");
					} else {
						shared = blog.getObject("blog", new JsonObject()).getArray("shared");
					}
					if (shared != null) {
						List<String> shareIds = getSharedIds(shared);
						if (!shareIds.isEmpty()) {
							Map<String, Object> params = new HashMap<>();
							params.put("userId", user.getUserId());
							neo.send(neoQuery(shareIds), params, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> res) {
									if ("ok".equals(res.body().getString("status"))) {
										JsonObject r = res.body().getObject("result");
										List<String> recipients = new ArrayList<>();
										for (String attr: r.getFieldNames()) {
											String id = r.getObject(attr).getString("id");
											if (id != null) {
												recipients.add(id);
											}
										}
										Map<String, Object> t = new HashMap<>();
										t.put("recipients", recipients);
										t.put("blog", blog);
										handler.handle(t);
									} else {
										handler.handle(null);
									}
								}
							});
						} else {
							handler.handle(null);
						}
					} else {
						handler.handle(null);
					}
				} else {
					handler.handle(null);
				}
			}
		});
	}

	private List<String> getSharedIds(JsonArray shared) {
		List<String> shareIds = new ArrayList<>();
		for (Object o : shared) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject userShared = (JsonObject) o;
			String userOrGroupId = userShared.getString("groupId",
					userShared.getString("userId"));
			if (userOrGroupId != null && !userOrGroupId.trim().isEmpty()) {
				shareIds.add(userOrGroupId);
			}
		}
		return shareIds;
	}

	private String neoQuery(List<String> shareIds) {
		return "MATCH n<-[:APPARTIENT*0..1]-(u:User) " +
				"WHERE (n:User OR n:ProfileGroup) AND n.id IN ['" +
				Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
				"RETURN distinct u.id as id ";
	}

}
