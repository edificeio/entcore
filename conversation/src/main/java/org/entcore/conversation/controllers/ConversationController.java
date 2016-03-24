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


import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.DefaultConversationService;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getUserInfos;

public class ConversationController extends BaseController {

	private final static String QUOTA_BUS_ADDRESS = "org.entcore.workspace.quota";

	private String conversationName;
	private Storage storage;
	private int threshold;

	private ConversationService conversationService;
	private TimelineHelper notification;
	private EventStore eventStore;
	private enum ConversationEvent {GET_RESOURCE, ACCESS }

	public ConversationController(Storage storage) {
		this.storage = storage;
	}

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		this.conversationService = new DefaultConversationService(vertx,
				container.config().getString("app-name", Conversation.class.getSimpleName()));
		notification = new TimelineHelper(vertx, eb, container);
		eventStore = EventStoreFactory.getFactory().getEventStore(Conversation.class.getSimpleName());
		this.threshold = container.config().getInteger("alertStorage", 80);
		this.conversationName = container.config().getString("app-name", Conversation.class.getSimpleName()).toUpperCase();
	}

	@Get("conversation")
	@SecuredAction("conversation.view")
	public void view(HttpServerRequest request) {
		renderView(request);
		eventStore.createAndStoreEvent(ConversationEvent.ACCESS.name(), request);
	}

	@Post("draft")
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

	@Put("draft/:id")
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

	@Post("send")
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
				.putString("uri", container.config().getString("host", "http://localhost:8019") +
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
		notification.notifyTimeline(request, "messagerie.send-message", user, recipients, id, params);
	}

	@Get("list/:folder")
	@SecuredAction(value = "conversation.list", type = ActionType.AUTHENTICATED)
	public void list(final HttpServerRequest request) {
		final String folder = request.params().get("folder");
		final String restrain = request.params().get("restrain");
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
					conversationService.list(folder, restrain, user, page, new Handler<Either<String, JsonArray>>() {
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
		JsonArray toName = message.getArray("toName");
		if (toName != null) {
			JsonArray d2 = new JsonArray();
			message.putArray("toName", d2);
			for (Object o : toName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.addString(UserUtils.groupDisplayName((String) o, null, I18n.acceptLanguage(request)));
			}
		}
		JsonArray ccName = message.getArray("ccName");
		if (ccName != null) {
			JsonArray d2 = new JsonArray();
			message.putArray("ccName", d2);
			for (Object o : ccName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.addString(UserUtils.groupDisplayName((String) o, null, I18n.acceptLanguage(request)));
			}
		}
	}

	@Get("count/:folder")
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

	@Get("visible")
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

	@Get("message/:id")
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
								eventStore.createAndStoreEvent(ConversationEvent.GET_RESOURCE.name(), request,
										new JsonObject().putString("resource", id));
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

	@Put("trash")
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

	@Put("restore")
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

	@Delete("delete")
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
					conversationService.delete(ids, user, new Handler<Either<String,JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								badRequest(request, event.left().getValue());
								return;
							}

							JsonArray results = event.right().getValue();

							final int resultsModulo = 4;

							final AtomicLong totalSize = new AtomicLong(0);
							final AtomicInteger finalCountdown = new AtomicInteger(results.size() / resultsModulo);

							if(finalCountdown.get() <= 0){
								renderJson(request, new JsonObject(), 204);
								return;
							}
							final Handler<Void> finalCountdownHandler = new Handler<Void>() {
								@Override
								public void handle(Void event) {
									if(finalCountdown.decrementAndGet() == 0){
										updateUserQuota(user.getUserId(), totalSize.get(), new Handler<Void>() {
											public void handle(Void event) {
												renderJson(request, new JsonObject(), 204);
											}
										});

									}
								}
							};

							for(int i = 0; i < results.size(); i+=resultsModulo){
								//Result from the get all attachments request - attachment list
								JsonArray result = (JsonArray) results.get(i+1);
								//Result from the attachments deletion request - retrieves only deleted attachments
								JsonArray result2 = (JsonArray) results.get(i+2);

								JsonArray allAttachments = new JsonArray();
								JsonArray deletedAttachments = new JsonArray();
								for(Object item : result){
									JsonArray itemAttachments = ((JsonObject) item).getArray("attachments", new JsonArray());
									for(Object attachment: itemAttachments){
										allAttachments.add(attachment);
									}
								}
								for(Object item : result2){
									JsonArray itemAttachments = ((JsonObject) item).getArray("attachments", new JsonArray());
									for(Object attachment: itemAttachments){
										deletedAttachments.add(attachment);
									}
								}

								if(allAttachments.size() < 1){
									finalCountdownHandler.handle(null);
									continue;
								}

								final AtomicInteger countdown = new AtomicInteger(allAttachments.size());
								final Handler<Void> countdownHandler = new Handler<Void>() {
									@Override
									public void handle(Void event) {
										if(countdown.decrementAndGet() == 0){
											finalCountdownHandler.handle(event);
										}
									}
								};

								for(Object attachmentObj: allAttachments){
									final JsonObject attachment = (JsonObject) attachmentObj;
									final String attachmentId = attachment.getString("id");
									final Long attachmentSize = attachment.getLong("size");

									boolean toDelete = false;
									for(Object obj : deletedAttachments){
										JsonObject att = (JsonObject) obj;
										if(att.getString("id").equals(attachment.getString("id"))){
											toDelete = true;
											break;
										}
									}

									if(!toDelete){
										totalSize.addAndGet(-attachmentSize);
										countdownHandler.handle(null);
									} else {
										storage.removeFile(attachmentId, new Handler<JsonObject>() {
											@Override
											public void handle(JsonObject event) {
												if (!"ok".equals(event.getString("status"))) {
													log.error("["+ConversationController.class.getSimpleName()+"] Error while tying to delete attachment file (_id: {"+attachmentId+"})");
													countdownHandler.handle(null);
												} else {
													totalSize.addAndGet(-attachmentSize);
													countdownHandler.handle(null);
												}
											}
										});
									}
								}
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	//Get max folder depth
	@Get("max-depth")
	@SecuredAction(value="conversation.max.depth", type=ActionType.AUTHENTICATED)
	public void getMaxDepth(final HttpServerRequest request){
		renderJson(request, new JsonObject().putNumber("max-depth", Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH)));
	}

	//List folders at a given depth, or trashed folders at depth 1 only.
	@Get("folders/list")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listFolders(final HttpServerRequest request){
		final String parentId = request.params().get("parentId");
		final String listTrash = request.params().get("trash");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				if(listTrash != null){
					conversationService.listTrashedFolders(user, arrayResponseHandler(request));
				} else {
					conversationService.listFolders(parentId, user, arrayResponseHandler(request));
				}
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Create a new folder at root level or inside a user folder.
	@Post("folder")
	@SecuredAction(value = "conversation.folder.create", type = ActionType.AUTHENTICATED)
	public void createFolder(final HttpServerRequest request) {

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				RequestUtils.bodyToJson(request, pathPrefix + "createFolder", new Handler<JsonObject>() {
					public void handle(JsonObject body) {
						final String name = body.getString("name");
						final String parentId = body.getString("parentId", null);

						if(name == null || name.trim().length() == 0){
							badRequest(request);
							return;
						}
						conversationService.createFolder(name, parentId, user, defaultResponseHandler(request, 201));
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Update a folder
	@Put("folder/:folderId")
	@SecuredAction(value = "conversation.folder.update", type = ActionType.AUTHENTICATED)
	public void updateFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

		if(folderId == null || folderId.trim().length() == 0){
			badRequest(request);
			return;
		}

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				RequestUtils.bodyToJson(request, pathPrefix + "updateFolder", new Handler<JsonObject>() {
					public void handle(JsonObject data) {
						conversationService.updateFolder(folderId , data, user, defaultResponseHandler(request, 200));
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Move messages into a folder
	@Put("move/userfolder/:folderId")
	@SecuredAction(value = "conversation.message.move", type = ActionType.AUTHENTICATED)
	public void move(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");
		final List<String> messageIds = request.params().getAll("id");

		if(messageIds == null || messageIds.size() == 0){
			badRequest(request);
			return;
		}

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.moveToFolder(messageIds, folderId, user, defaultResponseHandler(request));
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Move messages into a system folder
	@Put("move/root")
	@SecuredAction(value = "conversation.message.move", type = ActionType.AUTHENTICATED)
	public void rootMove(final HttpServerRequest request) {
		final List<String> messageIds = request.params().getAll("id");

		if(messageIds == null || messageIds.size() == 0){
			badRequest(request);
			return;
		}

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.backToSystemFolder(messageIds, user, defaultResponseHandler(request));
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Trash a folder
	@Put("folder/trash/:folderId")
	@SecuredAction(value = "conversation.folder.trash", type = ActionType.AUTHENTICATED)
	public void trashFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.trashFolder(folderId, user, defaultResponseHandler(request));
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Restore a trashed folder
	@Put("folder/restore/:folderId")
	@SecuredAction(value = "conversation.folder.restore", type = ActionType.AUTHENTICATED)
	public void restoreFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.restoreFolder(folderId, user, defaultResponseHandler(request));
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Delete a trashed folder
	@Delete("folder/:folderId")
	@SecuredAction(value = "conversation.folder.delete", type = ActionType.AUTHENTICATED)
	public void deleteFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.deleteFolder(folderId, user, new Handler<Either<String,JsonArray>>() {
					public void handle(Either<String, JsonArray> event) {
						if(event.isLeft()){
							badRequest(request, event.left().getValue());
							return;
						}

						JsonArray results = event.right().getValue();

						final JsonArray attachmentsQuota =
								((JsonArray) results.get(0)).size() > 0 ?
									((JsonObject) ((JsonArray) results.get(0)).get(0)).getArray("attachments", new JsonArray())
									: new JsonArray();
						final JsonArray attachmentsDeletion =
								((JsonArray) results.get(2)).size() > 0 ?
									((JsonObject) ((JsonArray) results.get(2)).get(0)).getArray("attachments", new JsonArray())
									: new JsonArray();

						final AtomicLong totalSize = new AtomicLong(0L);
						final AtomicInteger deletionCountdown = new AtomicInteger(attachmentsDeletion.size());

						final VoidHandler finalHandler = new VoidHandler() {
							@Override
							protected void handle() {
								if(deletionCountdown.decrementAndGet() == 0){
									renderJson(request, new JsonObject(), 200);
								}
							}
						};

						for(Object attachmentObj: attachmentsQuota){
							final JsonObject attachment = (JsonObject) attachmentObj;
							final Long attachmentSize = attachment.getLong("size");

							totalSize.addAndGet(-attachmentSize);
						}

						updateUserQuota(user.getUserId(), totalSize.get(), new Handler<Void>() {
							@Override
							public void handle(Void event) {
								if(deletionCountdown.get() == 0){
									renderJson(request, new JsonObject(), 200);
									return;
								}

								for(Object attachmentObj: attachmentsDeletion){
									final JsonObject attachment = (JsonObject) attachmentObj;
									final String attachmentId = attachment.getString("id");

									storage.removeFile(attachmentId, new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject event) {
											if (!"ok".equals(event.getString("status"))) {
												log.error("["+ConversationController.class.getSimpleName()+"] Error while tying to delete attachment file (_id: {"+attachmentId+"})");
											}
											finalHandler.handle(null);
										}
									});
								}
							}
						});
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Post an new attachment to a drafted message
	@Post("message/:messageId/attachment")
	@SecuredAction(value = "conversation.message.post.attachment", type = ActionType.AUTHENTICATED)
	public void postAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("messageId");
		request.pause();

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				getUserQuota(user.getUserId(), new Handler<JsonObject>() {
					public void handle(JsonObject j) {
						if(j == null || "error".equals(j.getString("status"))){
							badRequest(request, j == null ? "" : j.getString("message"));
							return;
						}

						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);

						request.resume();
						ConversationController.this.storage.writeUploadFile(request, (quota - storage), new Handler<JsonObject>() {
							public void handle(JsonObject uploaded) {
								if (!"ok".equals(uploaded.getString("status"))) {
									badRequest(request, uploaded.getString("message"));
									return;
								}

								updateUserQuota(user.getUserId(), uploaded.getObject("metadata", new JsonObject()).getLong("size", 0L));
								conversationService.addAttachment(messageId, user, uploaded, defaultResponseHandler(request));
							}
						});
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Download an attachment
	@Get("message/:messageId/attachment/:attachmentId")
	@SecuredAction(value = "conversation.message.get.attachment", type = ActionType.AUTHENTICATED)
	public void getAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("messageId");
		final String attachmentId = request.params().get("attachmentId");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}

				conversationService.getAttachment(messageId, attachmentId, user, new Handler<Either<String,JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if(event.isLeft()){
							badRequest(request, event.left().getValue());
							return;
						}
						if(event.isRight() && event.right().getValue() == null){
							badRequest(request, event.right().getValue().toString());
							return;
						}

						JsonObject neoResult = event.right().getValue();
						String fileId = neoResult.getString("id");
						if(fileId == null || fileId.trim().length() == 0){
							notFound(request, "invalid.file.id");
							return;
						}

						JsonObject metadata = new JsonObject()
							.putString("filename", neoResult.getString("filename"))
							.putString("content-type", neoResult.getString("contentType"));

						storage.sendFile(fileId, neoResult.getString("filename"), request, false, metadata);
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Delete an attachment
	@Delete("message/:messageId/attachment/:attachmentId")
	@SecuredAction(value = "conversation.message.delete.attachment", type = ActionType.AUTHENTICATED)
	public void deleteAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("messageId");
		final String attachmentId = request.params().get("attachmentId");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}

				conversationService.removeAttachment(messageId, attachmentId, user, new Handler<Either<String,JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if(event.isLeft()){
							badRequest(request, event.left().getValue());
							return;
						}
						if(event.isRight() && event.right().getValue() == null){
							badRequest(request, event.right().getValue().toString());
							return;
						}

						JsonObject neoResult = event.right().getValue();

						boolean deletionCheck = neoResult.getBoolean("deletionCheck", false);
						final String fileId = neoResult.getString("fileId");
						final long fileSize = neoResult.getLong("fileSize");

						if(!deletionCheck){
							updateUserQuota(user.getUserId(), -fileSize);
							renderJson(request, neoResult);

							return;
						}

						storage.removeFile(fileId, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject result) {
								if (!"ok".equals(result.getString("status"))) {
									log.error("["+ConversationController.class.getSimpleName()+"] Error while tying to delete attachment file (_id: {"+fileId+"})");
									badRequest(request);
									return;
								}

								updateUserQuota(user.getUserId(), -fileSize);
								renderJson(request, result);
							}
						});
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	@Put("message/:messageId/forward/:forwardedId")
	@SecuredAction(value = "conversation.message.forward.attachments", type = ActionType.AUTHENTICATED)
	public void forwardAttachments(final HttpServerRequest request){
		final String messageId = request.params().get("messageId");
		final String forwardedId = request.params().get("forwardedId");

		//1 - get user infos
		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				//2 - get user quota
				getUserQuota(user.getUserId(), new Handler<JsonObject>() {
					public void handle(JsonObject j) {
						if(j == null || "error".equals(j.getString("status"))){
							badRequest(request, j == null ? "" : j.getString("message"));
							return;
						}

						final long quotaLeft = j.getLong("quota", 0l) - j.getLong("storage", 0l);

						//3 - get forwarded message attachments
						conversationService.get(forwardedId, user, new Handler<Either<String,JsonObject>>() {
							@Override
							public void handle(Either<String, JsonObject> event) {
								if(event.isLeft()){
									badRequest(request, event.left().getValue());
									return;
								}
								if(event.isRight() && event.right().getValue() == null){
									badRequest(request, event.right().getValue().toString());
									return;
								}
								final JsonObject neoResult = event.right().getValue();
								final JsonArray attachments = neoResult.getArray("attachments");

								long attachmentsSize = 0l;
								for(Object genericObj : attachments){
									JsonObject attachment = (JsonObject) genericObj;
									attachmentsSize += attachment.getLong("size", 0l);
								}
								final long finalAttachmentsSize = attachmentsSize;

								// if total attachment size > quota left, return 403
								if(attachmentsSize > quotaLeft){
									forbidden(request, "forward.failed.quota");
									return;
								}

								//4 - forward attachments, add relationships between the message and the already existing attachments
								conversationService.forwardAttachments(forwardedId, messageId, user, new Handler<Either<String,JsonObject>>() {
									@Override
									public void handle(Either<String, JsonObject> event) {
										if(event.isLeft()){
											badRequest(request, event.left().getValue());
											return;
										}

										//5 - update user quota
										updateUserQuota(user.getUserId(), finalAttachmentsSize, new Handler<Void>(){
											@Override
											public void handle(Void event) {
												ok(request);
											}
										});
									}

								});
							}
						});
					}
				});
			}
		};
		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}


	@BusAddress("org.entcore.conversation")
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

	private void getUserQuota(String userId, final Handler<JsonObject> handler){
		JsonObject message = new JsonObject();
		message.putString("action", "getUserQuota");
		message.putString("userId", userId);

		eb.send(QUOTA_BUS_ADDRESS, message, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				handler.handle(reply.body());
			}
		});
	}

	private void updateUserQuota(final String userId, long size){
		updateUserQuota(userId, size, null);
	}

	private void updateUserQuota(final String userId, long size, final Handler<Void> continuation){
		JsonObject message = new JsonObject();
		message.putString("action", "updateUserQuota");
		message.putString("userId", userId);
		message.putNumber("size", size);
		message.putNumber("threshold", threshold);

		eb.send(QUOTA_BUS_ADDRESS, message, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				JsonObject obj = reply.body();
				UserUtils.addSessionAttribute(eb, userId, "storage", obj.getLong("storage"), null);
				if (obj.getBoolean("notify", false)) {
					notifyEmptySpaceIsSmall(userId);
				}

				if(continuation != null)
					continuation.handle(null);
			}
		});
	}

	private void notifyEmptySpaceIsSmall(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()),
				"messagerie.storage", null, recipients, null, new JsonObject());
	}

}
