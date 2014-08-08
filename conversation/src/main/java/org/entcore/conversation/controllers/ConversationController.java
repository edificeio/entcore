/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.conversation.controllers;


import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.DefaultConversationService;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;

public class ConversationController extends Controller {

	private final ConversationService conversationService;
	private final TimelineHelper notification;

	public ConversationController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		this.conversationService = new DefaultConversationService(vertx,
				container.config().getString("app-name", Conversation.class.getSimpleName()));
		notification = new TimelineHelper(vertx, eb, container);
	}

	@SecuredAction("conversation.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("conversation.create.draft")
	public void createDraft(final HttpServerRequest request) {
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String parentMessageId = request.params().get("In-Reply-To");
					bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject message) {
							conversationService.saveDraft(parentMessageId, message, user, defaultResponseHandler(request, 201));
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "conversation.create.draft", type = ActionType.AUTHENTICATED)
	public void updateDraft(final HttpServerRequest request) {
		final String messageId = request.params().get("id");
		if (messageId == null || messageId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject message) {
							conversationService.updateDraft(messageId, message, user,
									defaultResponseHandler(request));
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("conversation.send")
	public void send(final HttpServerRequest request) {
		final String messageId = request.params().get("id");
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String parentMessageId = request.params().get("In-Reply-To");
					bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject message) {
							conversationService.send(parentMessageId, messageId, message, user,
									new Handler<Either<String, JsonObject>>() {
										@Override
										public void handle(Either<String, JsonObject> event) {
											if (event.isRight()) {
												timelineNotification(request, event.right().getValue(), user);
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
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void timelineNotification(HttpServerRequest request, JsonObject sentMessage, UserInfos user) {
		log.debug(sentMessage.encode());
		JsonArray r = sentMessage.getArray("sentIds");
		String id = sentMessage.getString("id");
		String subject = sentMessage.getString("subject", "<span translate key=\"timeline.no.subject\"></span>");
		sentMessage.removeField("sentIds");
		sentMessage.removeField("id");
		sentMessage.removeField("subject");
		if (r == null || id == null || user == null) {
			return;
		}
		final JsonObject params = new JsonObject()
				.putString("uri", container.config().getString("userbook-host") +
						"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.putString("username", user.getUsername())
				.putString("subject", subject)
				.putString("messageUri", container.config().getString("host", "http://localhost:8019") +
						pathPrefix + "/conversation#/read-mail/" + id);
		String type = container.config().getString("app-name", Conversation.class.getSimpleName()).toUpperCase();
		List<String> recipients = new ArrayList<>();
		for (Object o : r) {
			if (!(o instanceof String)) continue;
			recipients.add((String) o);
		}
		notification.notifyTimeline(request, user, type, type + "_SENT",
				recipients, id, "notification/notify-send-message.html", params);
	}

	@SecuredAction(value = "conversation.list", type = ActionType.AUTHENTICATED)
	public void list(final HttpServerRequest request) {
		final String folder = request.params().get("folder");
		final String p = Utils.getOrElse(request.params().get("page"), "0", false);
		if (folder == null || folder.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					int page;
					try {
						page = Integer.parseInt(p);
					} catch (NumberFormatException e) { page = 0; }
					conversationService.list(folder, user, page, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								for (Object o : r.right().getValue()) {
									if (!(o instanceof JsonObject)) {
										continue;
									}
									translateGroupsNames((JsonObject) o, request);
								}
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.putString("error", r.left().getValue());
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

	private void translateGroupsNames(JsonObject message, HttpServerRequest request) {
		JsonArray d3 = new JsonArray();
		for (Object o2 : message.getArray("displayNames", new JsonArray())) {
			if (!(o2 instanceof String)) {
				continue;
			}
			String [] a = ((String) o2).split("\\$");
			if (a.length != 4) {
				continue;
			}
			JsonArray d2 = new JsonArray().add(a[0]);
			if (a[2] != null && !a[2].trim().isEmpty()) {
				final String groupDisplayName = (a[3] != null && !a[3].trim().isEmpty()) ? a[3] : null;
				d2.addString(UserUtils.groupDisplayName(a[2], groupDisplayName, I18n.acceptLanguage(request)));
			} else {
				d2.add(a[1]);
			}
			d3.addArray(d2);
		}
		message.putArray("displayNames", d3);
	}

	@SecuredAction(value = "conversation.count", type = ActionType.AUTHENTICATED)
	public void count(final HttpServerRequest request) {
		final String folder = request.params().get("folder");
		final String unread = request.params().get("unread");
		if (folder == null || folder.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					Boolean b = null;
					if (unread != null && !unread.isEmpty()) {
						b = Boolean.valueOf(unread);
					}
					conversationService.count(folder, b, user, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "conversation.visible", type = ActionType.AUTHENTICATED)
	public void visible(final HttpServerRequest request) {
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					String parentMessageId = request.params().get("In-Reply-To");
					conversationService.findVisibleRecipients(parentMessageId, user,
							I18n.acceptLanguage(request), defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "conversation.get", type = ActionType.AUTHENTICATED)
	public void getMessage(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.get(id, user, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> r) {
							if (r.isRight()) {
								translateGroupsNames(r.right().getValue(), request);
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.putString("error", r.left().getValue());
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

	@SecuredAction(value = "conversation.trash", type = ActionType.AUTHENTICATED)
	public void trash(final HttpServerRequest request) {
		final List<String> ids = request.params().getAll("id");
		if (ids == null || ids.isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.trash(ids, user, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "conversation.restore", type = ActionType.AUTHENTICATED)
	public void restore(final HttpServerRequest request) {
		final List<String> ids = request.params().getAll("id");
		if (ids == null || ids.isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.restore(ids, user, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "conversation.delete", type = ActionType.AUTHENTICATED)
	public void delete(final HttpServerRequest request) {
		final List<String> ids = request.params().getAll("id");
		if (ids == null || ids.isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.delete(ids, user, defaultResponseHandler(request, 204));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	public void conversationEventBusHandler(Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "send" : send(message);
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	private void send(final Message<JsonObject> message) {
		JsonObject m = message.body().getObject("message");
		if (m == null) {
			message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.message"));
		}
		final HttpServerRequest request = new JsonHttpServerRequest(
				message.body().getObject("request", new JsonObject()));
		final UserInfos user = new UserInfos();
		user.setUserId(message.body().getString("userId"));
		user.setUsername(message.body().getString("username"));
		conversationService.send(null, null, m, user,
				new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							timelineNotification(request, event.right().getValue(), user);
							JsonObject s = new JsonObject().putString("status", "ok")
									.putArray("result", new JsonArray().add(new JsonObject()));
							message.reply(s);
						} else {
							JsonObject error = new JsonObject()
									.putString("error", event.left().getValue());
							message.reply(error);
						}
					}
				});
	}

}
