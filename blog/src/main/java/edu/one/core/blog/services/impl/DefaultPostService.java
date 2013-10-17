package edu.one.core.blog.services.impl;

import com.mongodb.QueryBuilder;
import edu.one.core.blog.services.BlogService;
import edu.one.core.blog.services.PostService;
import edu.one.core.infra.*;
import edu.one.core.infra.security.resources.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;

public class DefaultPostService implements PostService {

	private final MongoDb mongo;
	private static final String POST_COLLECTION = "posts";

	public DefaultPostService(MongoDb mongo) {
		this.mongo = mongo;
	}

	@Override
	public void create(String blogId, JsonObject post, UserInfos author,
					   final Handler<Either<String, JsonObject>> result) {
		JsonObject now = MongoDb.now();
		JsonObject blogRef = new JsonObject()
				.putString("$ref", "blogs")
				.putString("$id", blogId);
		JsonObject owner = new JsonObject()
				.putString("userId", author.getUserId())
				.putString("username", author.getUsername())
				.putString("login", author.getLogin());
		post.putObject("created", now)
				.putObject("modified", now)
				.putObject("author", owner)
				.putString("state", StateType.DRAFT.name())
				.putArray("comments", new JsonArray())
				.putNumber("views", 0)
				.putObject("blog", blogRef);
		JsonObject b = Utils.validAndGet(post, FIELDS, FIELDS);
		if (validationError(result, b)) return;
		mongo.save(POST_COLLECTION, b, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				result.handle(Utils.validResult(res));
			}
		});
	}

	@Override
	public void update(String postId, JsonObject post, final Handler<Either<String, JsonObject>> result) {
		post.putObject("modified", MongoDb.now());
		JsonObject b = Utils.validAndGet(post, UPDATABLE_FIELDS, Collections.<String>emptyList());
		if (validationError(result, b)) return;
		QueryBuilder query = QueryBuilder.start("_id").is(postId);
		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		for (String attr: b.getFieldNames()) {
			modifier.set(attr, b.getValue(attr));
		}
		mongo.update(POST_COLLECTION, MongoQueryBuilder.build(query), modifier.build(),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						result.handle(Utils.validResult(event));
					}
				});
	}

	@Override
	public void delete(String postId, final Handler<Either<String, JsonObject>> result) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId);
		mongo.delete(POST_COLLECTION, MongoQueryBuilder.build(query),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						result.handle(Utils.validResult(event));
					}
				});
	}

	@Override
	public void get(final String postId, StateType state, final Handler<Either<String, JsonObject>> result) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId)
				.put("state").is(state.name());
		mongo.findOne(POST_COLLECTION, MongoQueryBuilder.build(query),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Either<String, JsonObject> res = Utils.validResult(event);
				if (res.isRight() && res.right().getValue().size() > 0) {
					QueryBuilder query2 = QueryBuilder.start("_id").is(postId)
							.put("state").is(StateType.PUBLISHED.name());
					MongoUpdateBuilder incView = new MongoUpdateBuilder();
					incView.inc("views", 1);
					mongo.update(POST_COLLECTION, MongoQueryBuilder.build(query2), incView.build());
				}
				result.handle(res);
			}
		});
	}

	@Override
	public void list(String blogId, StateType state, final UserInfos user,
				final Handler<Either<String, JsonArray>> result) {
		final QueryBuilder query = QueryBuilder.start("blog.$id").is(blogId)
				.put("state").is(state.name());
		final JsonObject sort = new JsonObject().putNumber("modified", -1);
		if (StateType.PUBLISHED.equals(state)) {
			mongo.find(POST_COLLECTION, MongoQueryBuilder.build(query), sort, null,
					new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							result.handle(Utils.validResults(event));
						}
					});
		} else {
			QueryBuilder query2 = QueryBuilder.start("_id").is(blogId).put("shared").elemMatch(
					QueryBuilder.start("userId").is(user.getUserId()).put("manager").is(true).get());
			mongo.count("blogs", MongoQueryBuilder.build(query2), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject res = event.body();
					if (res != null && "ok".equals(res.getString("status")) &&
						1 != res.getInteger("count")) {
						query.put("author.userId").is(user.getUserId());
					}
					mongo.find(POST_COLLECTION, MongoQueryBuilder.build(query), sort, null,
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									result.handle(Utils.validResults(event));
								}
							});
				}
			});
		}
	}

	@Override
	public void submit(String postId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId)
				.put("state").is(StateType.DRAFT.name()).put("author.userId").is(user.getUserId());
		final JsonObject q = MongoQueryBuilder.build(query);
		JsonObject keys = new JsonObject().putNumber("blog", 1);
		JsonArray fetch = new JsonArray().addString("blog");
		mongo.findOne(POST_COLLECTION, q, keys, fetch,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getObject("result", new JsonObject()).size() > 0) {
					BlogService.PublishType type = Utils.stringToEnum(event.body().getObject("result")
							.getObject("blog",  new JsonObject()).getString("publish-type"),
							BlogService.PublishType.RESTRAINT, BlogService.PublishType.class);
					StateType state;
					if (BlogService.PublishType.RESTRAINT.equals(type)) {
						state = StateType.SUBMITTED;
					} else {
						state = StateType.PUBLISHED;
					}
					MongoUpdateBuilder updateQuery = new MongoUpdateBuilder().set("state", state.name());
					mongo.update(POST_COLLECTION, q, updateQuery.build(), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							result.handle(Utils.validResult(res));
						}
					});
				} else {
					result.handle(Utils.validResult(event));
				}
			}
		});
	}

	@Override
	public void publish(String postId, final Handler<Either<String, JsonObject>> result) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId);
		MongoUpdateBuilder updateQuery = new MongoUpdateBuilder().set("state", StateType.PUBLISHED.name());
		mongo.update(POST_COLLECTION, MongoQueryBuilder.build(query), updateQuery.build(),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				result.handle(Utils.validResult(res));
			}
		});
	}

	@Override
	public void unpublish(String postId, final Handler<Either<String, JsonObject>> result) {
		QueryBuilder query = QueryBuilder.start("_id").is(postId);
		MongoUpdateBuilder updateQuery = new MongoUpdateBuilder().set("state", StateType.DRAFT.name());
		mongo.update(POST_COLLECTION, MongoQueryBuilder.build(query), updateQuery.build(),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				result.handle(Utils.validResult(res));
			}
		});
	}

	private boolean validationError(Handler<Either<String, JsonObject>> result, JsonObject b) {
		if (b == null) {
			result.handle(new Either.Left<String, JsonObject>("Validation error : invalids fields."));
			return true;
		}
		return false;
	}

}
