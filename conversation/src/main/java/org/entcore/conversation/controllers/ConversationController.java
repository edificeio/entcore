/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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

import io.vertx.core.AsyncResult;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerResponse;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.Zip;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.filters.MessageOwnerFilter;
import org.entcore.conversation.filters.MessageUserFilter;
import org.entcore.conversation.filters.MultipleMessageUserFilter;
import org.entcore.conversation.filters.VisiblesFilter;
import org.entcore.conversation.filters.FoldersFilter;
import org.entcore.conversation.filters.FoldersMessagesFilter;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.Neo4jConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getUserInfos;

public class ConversationController extends BaseController {

	private final static String QUOTA_BUS_ADDRESS = "org.entcore.workspace.quota";

	private Storage storage;
	private int threshold;

	private ConversationService conversationService;
	private Neo4jConversationService neoConversationService;
	private TimelineHelper notification;
	private EventStore eventStore;
	private enum ConversationEvent {GET_RESOURCE, ACCESS }
	private final String exportPath;

	public ConversationController(Storage storage, String exportPath) {
		this.storage = storage;
		this.exportPath = exportPath;
	}

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		/*
		this.conversationService = new DefaultConversationService(vertx,
				config.getString("app-name", Conversation.class.getSimpleName()));
				*/
		this.conversationService = new SqlConversationService(vertx, config.getString("db-schema", "conversation"));
		this.neoConversationService = new Neo4jConversationService();
		notification = new TimelineHelper(vertx, eb, config);
		eventStore = EventStoreFactory.getFactory().getEventStore(Conversation.class.getSimpleName());
		this.threshold = config.getInteger("alertStorage", 80);
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
						public void handle(final JsonObject message) {

							if(!message.containsKey("from")){
								message.put("from", user.getUserId());
							}

							final Handler<JsonObject> parentHandler = new Handler<JsonObject>() {
								@Override
								public void handle(JsonObject parent) {
									final String threadId;
									if( parent != null){
										threadId = parent.getString("thread_id");
									}else{
										threadId = null;
									}
									neoConversationService.addDisplayNames(message, parent, new Handler<JsonObject>() {
										public void handle(JsonObject message) {
											conversationService.saveDraft(parentMessageId, threadId, message, user, defaultResponseHandler(request, 201));
										}
									});
								}
							};

							if(parentMessageId != null && !parentMessageId.trim().isEmpty()){
								conversationService.get(parentMessageId, user, new Handler<Either<String,JsonObject>>() {
									public void handle(Either<String, JsonObject> event) {
										if(event.isLeft()){
											badRequest(request);
											return;
										}

										parentHandler.handle(event.right().getValue());
									}
								});
							} else {
								parentHandler.handle(null);
							}

						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("draft/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageOwnerFilter.class)
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
							if(!message.containsKey("from")){
								message.put("from", user.getUserId());
							}
							neoConversationService.addDisplayNames(message, null, new Handler<JsonObject>() {
								public void handle(JsonObject message) {
									conversationService.updateDraft(messageId, message, user,
											defaultResponseHandler(request));
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

	private void saveAndSend(final String messageId, final JsonObject message, final UserInfos user,
			final String parentMessageId, final String threadId, final Handler<Either<String, JsonObject>> result){

		Handler<Either<String, JsonObject>> handler = new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					result.handle(event);
					return;
				}

				final String id = (messageId != null && !messageId.trim().isEmpty()) ?
					messageId :
					event.right().getValue().getString("id");

				conversationService.get(id, user, new Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if(event.isLeft()){
							result.handle(event);
							return;
						}

						JsonObject msg = event.right().getValue();
						JsonArray attachments = msg.getJsonArray("attachments", new fr.wseduc.webutils.collections.JsonArray());
						final AtomicLong size = new AtomicLong(0l);

						for(Object att : attachments){
							size.addAndGet(((JsonObject) att).getLong("size", 0l));
						}

						neoConversationService.findInactives(message, size.get(), new Handler<JsonObject>() {
							public void handle(JsonObject userDetails) {
								message.mergeIn(userDetails);

								conversationService.send(parentMessageId, id, message, user, new Handler<Either<String,JsonObject>>() {
									public void handle(Either<String, JsonObject> event) {
										if(event.isRight()){
											for(Object recipient : message.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray())){
												if(recipient.toString().equals(user.getUserId()))
													continue;
												updateUserQuota(recipient.toString(), size.get());
											}
										}
										result.handle(event);
									}
								});
							}
						});
					}
				});
			}
		};
		if (messageId != null && !messageId.trim().isEmpty()) {
			conversationService.updateDraft(messageId, message, user, handler);
		} else {
			conversationService.saveDraft(parentMessageId, threadId, message, user, handler);
		}
	}

	@Post("send")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(VisiblesFilter.class)
	public void send(final HttpServerRequest request) {
		final String messageId = request.params().get("id");

		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String parentMessageId = request.params().get("In-Reply-To");
					bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(final JsonObject message) {
							if(!message.containsKey("from")){
								message.put("from", user.getUserId());
							}

							final Handler<JsonObject> parentHandler = new Handler<JsonObject>() {
								public void handle(JsonObject parentMsg) {
									final String threadId;
									if( parentMsg != null){
										threadId = parentMsg.getString("thread_id");
									}else{
										threadId = null;
									}
									neoConversationService.addDisplayNames(message, parentMsg, new Handler<JsonObject>() {
										public void handle(final JsonObject message) {
											saveAndSend(messageId, message, user, parentMessageId, threadId,
													new Handler<Either<String, JsonObject>>() {
												@Override
												public void handle(Either<String, JsonObject> event) {
													if (event.isRight()) {
														JsonObject result = event.right().getValue();
														JsonObject timelineParams = new JsonObject()
															.put("subject", result.getString("subject"))
															.put("body", StringUtils.stripHtmlTag(result.getString("body")))
															.put("id", result.getString("id"))
															.put("sentIds", message.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray()));
														timelineNotification(request, timelineParams, user);
														renderJson(request, result
															.put("inactive", message.getJsonArray("inactives", new fr.wseduc.webutils.collections.JsonArray()))
															.put("undelivered", message.getJsonArray("undelivered", new fr.wseduc.webutils.collections.JsonArray()))
															.put("sent", message.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray()).size()));
													} else {
														JsonObject error = new JsonObject().put("error", event.left().getValue());
														renderJson(request, error, 400);
													}
												}
											});
										}
									});
								}
							};

							if(parentMessageId != null && !parentMessageId.trim().isEmpty()){
								conversationService.get(parentMessageId, user, new Handler<Either<String,JsonObject>>() {
									public void handle(Either<String, JsonObject> event) {
										if(event.isLeft()){
											badRequest(request);
											return;
										}

										parentHandler.handle(event.right().getValue());
									}
								});
							} else {
								parentHandler.handle(null);
							}
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
		JsonArray r = sentMessage.getJsonArray("sentIds");
		String id = sentMessage.getString("id");
		String subject = sentMessage.getString("subject", "<span translate key=\"timeline.no.subject\"></span>");
		sentMessage.remove("sentIds");
		sentMessage.remove("id");
		sentMessage.remove("subject");
		if (r == null || id == null || user == null) {
			return;
		}
		final JsonObject params = new JsonObject()
				.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("username", user.getUsername())
				.put("subject", subject)
				.put("messageUri", pathPrefix + "/conversation#/read-mail/" + id);
		params.put("resourceUri", params.getString("messageUri"));
		params.put("pushNotif", new JsonObject().put("title", "push.notif.new.message").put("body", user.getUsername()+ " : "+ sentMessage.getString("body")));
		List<String> recipients = new ArrayList<>();
		String idTmp;
		for (Object o : r) {
			if (!(o instanceof String)) continue;
			idTmp = (String) o;
			if(!user.getUserId().equals(idTmp))
				recipients.add(idTmp);
		}
		notification.notifyTimeline(request, "messagerie.send-message", user, recipients, id, params);
	}

	@Get("list/:folder")
	@SecuredAction(value = "conversation.list", type = ActionType.AUTHENTICATED)
	public void list(final HttpServerRequest request) {
		final String folder = request.params().get("folder");
		final String restrain = request.params().get("restrain");
		final String unread = request.params().get("unread");
		final String search = request.params().get("search");
		if(search != null  && search.trim().length() < 3){
			badRequest(request);
			return;
		}
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
					Boolean b = null;;
					if (unread != null && !unread.isEmpty()) {
						b = Boolean.valueOf(unread);
					}
					conversationService.list(folder, restrain, b, user, page, search, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								for (Object o : r.right().getValue()) {
									if (!(o instanceof JsonObject)) {
										continue;
									}
									translateGroupsNames((JsonObject) o, user, request);
								}
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.put("error", r.left().getValue());
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

	private void translateGroupsNames(JsonObject message, UserInfos userInfos, HttpServerRequest request) {
		final JsonArray cci = getOrElse(message.getJsonArray("cci"), new fr.wseduc.webutils.collections.JsonArray());
		final JsonArray cc = getOrElse(message.getJsonArray("cc"), new fr.wseduc.webutils.collections.JsonArray());
		final JsonArray to = getOrElse(message.getJsonArray("to"), new fr.wseduc.webutils.collections.JsonArray());
		final String from = message.getString("from");
		final Boolean notIsSender = (!userInfos.getUserId().equals(from));
		final List<String> userGroups = getOrElse(userInfos.getGroupsIds(), new ArrayList<String>());

		JsonArray d3 = new fr.wseduc.webutils.collections.JsonArray();
		for (Object o2 : getOrElse(message.getJsonArray("displayNames"), new fr.wseduc.webutils.collections.JsonArray())) {
			if (!(o2 instanceof String)) {
				continue;
			}
			String [] a = ((String) o2).split("\\$");
			if (a.length != 4) {
				continue;
			}

			if (notIsSender && cci.contains(a[0]) && !cc.contains(a[0]) && !to.contains(a[0]) && !from.equals(a[0])) continue;
			JsonArray d2 = new fr.wseduc.webutils.collections.JsonArray().add(a[0]);
			if (a[2] != null && !a[2].trim().isEmpty()) {
				final String groupDisplayName = (a[3] != null && !a[3].trim().isEmpty()) ? a[3] : null;
				d2.add(UserUtils.groupDisplayName(a[2], groupDisplayName, I18n.acceptLanguage(request)));
				//is group
				d2.add(true);
			} else {
				d2.add(a[1]);
				d2.add(false);
			}
			d3.add(d2);
		}
		message.put("displayNames", d3);
		JsonArray toName = message.getJsonArray("toName");
		if (toName != null) {
			JsonArray d2 = new fr.wseduc.webutils.collections.JsonArray();
			message.put("toName", d2);
			for (Object o : toName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, I18n.acceptLanguage(request)));
			}
		}
		JsonArray ccName = message.getJsonArray("ccName");
		if (ccName != null) {
			JsonArray d2 = new fr.wseduc.webutils.collections.JsonArray();
			message.put("ccName", d2);
			for (Object o : ccName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, I18n.acceptLanguage(request)));
			}
		}
		JsonArray cciName = message.getJsonArray("cciName");
		if (cciName != null) {
			JsonArray d2 = new fr.wseduc.webutils.collections.JsonArray();
			message.put("cciName", d2);
			for (Object o : cciName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, I18n.acceptLanguage(request)));
			}
		}

		if (notIsSender) {
			//keep cci for user recipient only
			final JsonArray newCci = new fr.wseduc.webutils.collections.JsonArray();
			if (cci.contains(userInfos.getUserId())) {
				newCci.add(userInfos.getUserId());
			} else if (!userGroups.isEmpty()) {
				for (final String groupId : userGroups) {
					if (cci.contains(groupId)) {
						newCci.add(userInfos.getUserId());
						break;
					}
				}
			}

			//add user display name for recipient
			if (!newCci.isEmpty()) {
				JsonArray d2 = new fr.wseduc.webutils.collections.JsonArray().add(userInfos.getUserId());
				d2.add(userInfos.getUsername());
				d2.add(false);
				d3.add(d2);
			}

			message.put("cci", newCci);
			message.put("cciName", new fr.wseduc.webutils.collections.JsonArray());
		}
	}

	@Get("threads/list")
	@SecuredAction(value = "conversation.threads.list")
	public void listThreads(final HttpServerRequest request){
		final String p = Utils.getOrElse(request.params().get("page"), "0", false);
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					int page;
					try {
						page = Integer.parseInt(p);
					} catch (NumberFormatException e) { page = 0; }
					conversationService.listThreads( user, page, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								for (Object o : r.right().getValue()) {
									if (!(o instanceof JsonObject)) {
										continue;
									}
									translateGroupsNames((JsonObject) o, user, request);
								}
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.put("error", r.left().getValue());
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

	@Get("thread/messages/:id")
	@SecuredAction(value = "conversation.threads.message", type = ActionType.AUTHENTICATED)
	public void listThreadMessages(final HttpServerRequest request){
		final String threadId = request.params().get("id");
		final String p = Utils.getOrElse(request.params().get("page"), "0", false);

		if (threadId == null || threadId.trim().isEmpty()) {
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
					conversationService.listThreadMessages(threadId, page, user, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								for (Object o : r.right().getValue()) {
									if (!(o instanceof JsonObject)) {
										continue;
									}
									translateGroupsNames((JsonObject) o, user, request);
								}
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.put("error", r.left().getValue());
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

	@Get("thread/previous-messages/:id")
	@SecuredAction(value = "conversation.threads.previous")
	@ResourceFilter(MessageUserFilter.class)
	public void listPreviousMessages(final HttpServerRequest request){
		final String parentId = request.params().get("id");
		if (parentId == null || parentId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		listMessages(request, parentId, true);
	}

	@Get("thread/new-messages/:id")
	@SecuredAction(value = "conversation.threads.new")
	@ResourceFilter(MessageUserFilter.class)
	public void listNewMessages(final HttpServerRequest request){
		final String messageId = request.params().get("id");
		if (messageId == null || messageId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		listMessages(request, messageId, false);
	}

	private void listMessages(final HttpServerRequest request, final String messageId, final boolean listPrevious){
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.listThreadMessagesNavigation(messageId, listPrevious, user, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								for (Object o : r.right().getValue()) {
									if (!(o instanceof JsonObject)) {
										continue;
									}
									translateGroupsNames((JsonObject) o, user, request);
								}
								renderJson(request, r.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.put("error", r.left().getValue());
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

	@Put("thread/trash")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void trashThread(final HttpServerRequest request) {

		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray threadIds = body.getJsonArray("id");
				if (threadIds == null || threadIds.isEmpty()) {
					badRequest(request);
					return;
				}

				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							conversationService.trashThread(threadIds.getList(), user, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Post("thread/toggleUnread")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void toggleUnreadThread(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray threadIds = body.getJsonArray("id");
				final Boolean unread = body.getBoolean("unread");
				if (threadIds == null || threadIds.isEmpty() || unread == null) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							conversationService.toggleUnreadThread(threadIds.getList(), unread, user, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Get("count/:folder")
	@SecuredAction(value = "conversation.count", type = ActionType.AUTHENTICATED)
	public void count(final HttpServerRequest request) {
		final String folder = request.params().get("folder");
		final String restrain = request.params().get("restrain");
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
					conversationService.count(folder, restrain, b, user, defaultResponseHandler(request));
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
							I18n.acceptLanguage(request), request.params().get("search"), defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("visibles")
	@SecuredAction(value = "conversation.visibles", type = ActionType.AUTHENTICATED)
	public void visibles(final HttpServerRequest request) {
		final String customReturn = "WHERE visibles.id IN {ids} RETURN DISTINCT visibles.id";
		final JsonObject params = new JsonObject();
		final Set<String> ids = new HashSet<>();

		RequestUtils.bodyToJson(request, message -> {
			ids.addAll(message.getJsonArray("ids", new fr.wseduc.webutils.collections.JsonArray()).getList());
			params.put("ids", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ids)));
			getUserInfos(eb, request, user -> {
				if (user != null) {
					UserUtils.findVisibles(eb, user.getUserId(), customReturn, params, true, true, false, visibles -> {
						renderJson(request, visibles);
					});
				} else {
					unauthorized(request);
				}
			});
		});
	}

	@Get("message/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
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
								translateGroupsNames(r.right().getValue(), user, request);
								renderJson(request, r.right().getValue());
								eventStore.createAndStoreEvent(ConversationEvent.GET_RESOURCE.name(), request,
										new JsonObject().put("resource", id));
							} else {
								JsonObject error = new JsonObject()
										.put("error", r.left().getValue());
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void trash(final HttpServerRequest request) {

		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray ids = body.getJsonArray("id");
				if (ids == null || ids.isEmpty()) {
					badRequest(request);
					return;
				}

				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							conversationService.trash(ids.getList(), user, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("restore")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void restore(final HttpServerRequest request) {

		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray ids = body.getJsonArray("id");
				if (ids == null || ids.isEmpty()) {
					badRequest(request);
					return;
				}

				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							conversationService.restore(ids.getList(), user, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("delete")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void delete(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonArray ids = body.getJsonArray("id");
				if (ids == null || ids.isEmpty()) {
					badRequest(request);
					return;
				}
				deleteMessages(request, ids.getList(), false);
			}
		});
	}

	private void deleteMessages(final HttpServerRequest request, final List<String> ids, final Boolean deleteAll) {
		deleteMessages(request, ids, deleteAll, null);
	}

	private void deleteMessages(final HttpServerRequest request, final List<String> ids, final Boolean deleteAll, Handler<Either<String, JsonArray>> handler) {
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					conversationService.delete(ids, deleteAll, user, new Handler<Either<String,JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								if (handler != null) {
									handler.handle(event);
								} else {
									badRequest(request, event.left().getValue());
								}
								return;
							}

							JsonArray results = event.right().getValue();
							final long freeQuota = results.getJsonArray(0).getJsonObject(0).getLong("totalquota", 0L);

							updateUserQuota(user.getUserId(), -freeQuota, new Handler<Void>() {
								public void handle(Void event) {
									if (handler != null) {
										handler.handle(new Either.Right<>(new JsonArray()));
									} else {
										ok(request);
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

	@Delete("emptyTrash")
	@SecuredAction(value="conversation.empty.trash", type = ActionType.AUTHENTICATED)
	public void emptyTrash(final HttpServerRequest request) {
		AtomicInteger count = new AtomicInteger(2);
		AtomicBoolean error = new AtomicBoolean(false);
		Handler<Either<String, JsonArray>> handler = event -> {
			if (event.isLeft()) {
				error.set(true);
			}
			if (count.decrementAndGet() == 0) {
				if (error.get()) {
					badRequest(request);
				} else {
					ok(request);
				}
			}
		};
		deleteMessages(request, null, true, handler);
		deleteFolders(request, null, true, handler);
	}

	//Mark messages as unread / read
	@Post("toggleUnread")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MultipleMessageUserFilter.class)
	public void toggleUnread(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray ids = body.getJsonArray("id");
				final Boolean unread = body.getBoolean("unread");
				if (ids == null || ids.isEmpty() || unread == null) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							conversationService.toggleUnread(ids.getList(), unread, user, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	//Get max folder depth
	@Get("max-depth")
	@SecuredAction(value="conversation.max.depth", type=ActionType.AUTHENTICATED)
	public void getMaxDepth(final HttpServerRequest request){
		renderJson(request, new JsonObject().put("max-depth", Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH)));
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(FoldersFilter.class)
	public void updateFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(FoldersMessagesFilter.class)
	public void move(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray messageIds = body.getJsonArray("id");
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
						conversationService.moveToFolder(messageIds.getList(), folderId, user, defaultResponseHandler(request));
					}
				};

				UserUtils.getUserInfos(eb, request, userInfosHandler);

			}
		});
	}

	//Move messages into a system folder
	@Put("move/root")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(FoldersFilter.class)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(FoldersFilter.class)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(FoldersFilter.class)
	public void deleteFolder(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");

		deleteFolders(request, folderId, false);
	}

	private void deleteFolders(final HttpServerRequest request, final String folderId, final Boolean deleteAll) {
		deleteFolders(request, folderId, deleteAll, null);
	}

	private void deleteFolders(final HttpServerRequest request, final String folderId, final Boolean deleteAll, final Handler<Either<String, JsonArray>> handler) {
		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				conversationService.deleteFolder(folderId, deleteAll, user, new Handler<Either<String,JsonArray>>() {
					public void handle(Either<String, JsonArray> event) {
						if(event.isLeft()){
							if (handler != null) {
								handler.handle(event);
							} else {
								badRequest(request, event.left().getValue());
							}
							return;
						}

						JsonArray results = event.right().getValue();
						final long freeQuota = results.getJsonArray(0).getJsonObject(0).getLong("totalquota", 0L);

						updateUserQuota(user.getUserId(), -freeQuota, new Handler<Void>() {
							public void handle(Void event) {
								if (handler != null) {
									handler.handle(new Either.Right<>(new JsonArray()));
								} else {
									ok(request);
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
	@Post("message/:id/attachment")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void postAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("id");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				request.pause();
				getUserQuota(user.getUserId(), new Handler<JsonObject>() {
					public void handle(JsonObject j) {

						request.resume();

						if(j == null || "error".equals(j.getString("status"))){
							badRequest(request, j == null ? "" : j.getString("message"));
							return;
						}

						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);

						ConversationController.this.storage.writeUploadFile(request, (quota - storage), new Handler<JsonObject>() {
							public void handle(final JsonObject uploaded) {
								if (!"ok".equals(uploaded.getString("status"))) {
									badRequest(request, uploaded.getString("message"));
									return;
								}

								updateUserQuota(user.getUserId(),
									uploaded.getJsonObject("metadata",
									new JsonObject()).getLong("size", 0L),
									new Handler<Void>() {
										@Override
										public void handle(Void v) {
											conversationService.addAttachment(messageId, user, uploaded, defaultResponseHandler(request));
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

	//Download an attachment
	@Get("message/:id/attachment/:attachmentId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void getAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("id");
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
							.put("filename", neoResult.getString("filename"))
							.put("content-type", neoResult.getString("contentType"));

						storage.sendFile(fileId, neoResult.getString("filename"), request, false, metadata);
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	//Download all attachments
	@Get("message/:id/allAttachments")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void getAllAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("id");

		Handler<UserInfos> userInfosHandler = new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}

				conversationService.getAllAttachments(messageId, user, new Handler<Either<String, JsonArray>>() {
					@Override
					public void handle(Either<String, JsonArray> event) {
						if(event.isRight()){
							if(event.right().getValue() == null || event.right().getValue().size() < 1){
								badRequest(request);
								return;
							}
							if(event.right().getValue().size() < 2){
								JsonObject attachment = event.right().getValue().getJsonObject(0);
								JsonObject metadata = new JsonObject()
										.put("filename", attachment.getString("filename"))
										.put("content-type", attachment.getString("contentType"));

								storage.sendFile(attachment.getString("id"), attachment.getString("filename"), request, false, metadata);
							}else{
								zipAllAttachments(request, event.right().getValue());
							}
						}else {
							badRequest(request, event.left().getValue());
						}


					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	private void zipAllAttachments(final HttpServerRequest request, JsonArray files){
		JsonObject tmp;
		final FileSystem fs = vertx.fileSystem();
		final List<String> fileIds = new ArrayList<>();
		final JsonObject aliasFileName = new JsonObject();
		final String zipDownloadName = I18n.getInstance().translate("attachments", I18n.DEFAULT_DOMAIN, I18n.acceptLanguage(request)) + ".zip";
		final String zipDirectory = exportPath + File.separator + UUID.randomUUID().toString();

		for(Object file : files){
			tmp = (JsonObject)file;
			fileIds.add(tmp.getString("id"));
			aliasFileName.put(tmp.getString("id"), StringUtils.stripAccents(tmp.getString("filename")));
		}

		fs.mkdirs(zipDirectory, new Handler<AsyncResult<Void>>() {

			private void delete(final String path){
				fs.deleteRecursive(path, true, new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.failed())
							log.error("[Conversation] Error deleting  : " + path, event.cause());
					}
				});
			}

			@Override
			public void handle(AsyncResult<Void> event) {
				if(event.succeeded()) {
					final String zipfile = zipDirectory + ".zip";

					storage.writeToFileSystem(fileIds.toArray(new String[0]), zipDirectory, aliasFileName, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if (!"ok".equals(event.getString("status"))) {
								log.error("[Conversation] Can't write to zip directory : " + event.getString("message"));
								delete(zipDirectory);
								badRequest(request);

							} else {
								Zip.getInstance().zipFolder(zipDirectory, zipfile, true, Deflater.NO_COMPRESSION, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (!"ok".equals(event.body().getString("status"))) {
											log.error("[Conversation] Zip folder " + zipDirectory + " error : " + event.body().getString("message"));
											delete(zipDirectory);
											badRequest(request);
										}else {
											final HttpServerResponse resp = request.response();
											resp.putHeader("Content-Disposition", "attachment; filename=\"" + zipDownloadName + "\"");
											resp.putHeader("Content-Type", "application/zip; name=\"\" + zipDownloadName + \"\"");
											resp.sendFile(zipfile, new Handler<AsyncResult<Void>>() {
												public void handle(AsyncResult<Void> event) {
													if(event.failed())
														log.error("Error can't send  the file: ", event.cause());
													delete(zipfile);
												}
											});
										}
									}
								});
							}
						}
					});
				}
			}
		});
	}

	//Delete an attachment
	@Delete("message/:id/attachment/:attachmentId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void deleteAttachment(final HttpServerRequest request){
		final String messageId = request.params().get("id");
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

						final JsonObject result = event.right().getValue();

						boolean deletionCheck = result.getBoolean("deletionCheck", false);
						final String fileId = result.getString("fileId");
						final long fileSize = result.getLong("fileSize");

						updateUserQuota(user.getUserId(), -fileSize, new Handler<Void>() {
							public void handle(Void v) {
								renderJson(request, result);
							}
						});

						if(deletionCheck){
							storage.removeFile(fileId, new Handler<JsonObject>() {
								@Override
								public void handle(final JsonObject result) {
									if (!"ok".equals(result.getString("status"))) {
										log.error("["+ConversationController.class.getSimpleName()+"] Error while tying to delete attachment file (_id: {"+fileId+"})");
									}
								}
							});
						}
					}
				});
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	@Put("message/:id/forward/:forwardedId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void forwardAttachments(final HttpServerRequest request){
		final String messageId = request.params().get("id");
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
								final JsonArray attachments = neoResult.getJsonArray("attachments");

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

	@Get("/print")
	@SecuredAction(value = "conversation.print", type = ActionType.AUTHENTICATED)
	public void print(final HttpServerRequest request) {
		renderView(request, null, "print.html", null);
	}

	@BusAddress("org.entcore.conversation")
	public void conversationEventBusHandler(Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "send" : send(message);
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	private void send(final Message<JsonObject> message) {
		JsonObject m = message.body().getJsonObject("message");
		if (m == null) {
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.message"));
		}
		final HttpServerRequest request = new JsonHttpServerRequest(
				message.body().getJsonObject("request", new JsonObject()));
		final UserInfos user = new UserInfos();
		user.setUserId(message.body().getString("userId"));
		user.setUsername(message.body().getString("username"));
		if(!m.containsKey("from")){
			m.put("from", user.getUserId());
		}
		neoConversationService.addDisplayNames(m, null, new Handler<JsonObject>() {
			public void handle(final JsonObject m) {
				saveAndSend(null, m, user, null, null,
					new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								JsonObject result = event.right().getValue();
								JsonObject timelineParams = new JsonObject()
									.put("subject", result.getString("subject"))
									.put("id", result.getString("id"))
									.put("sentIds", m.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray()));
								timelineNotification(request, timelineParams, user);
								JsonObject s = new JsonObject().put("status", "ok")
										.put("result", new fr.wseduc.webutils.collections.JsonArray().add(new JsonObject()));
								message.reply(s);
							} else {
								JsonObject error = new JsonObject()
										.put("error", event.left().getValue());
								message.reply(error);
							}
						}
					});
			}
		});

	}

	private void getUserQuota(String userId, final Handler<JsonObject> handler){
		JsonObject message = new JsonObject();
		message.put("action", "getUserQuota");
		message.put("userId", userId);

		eb.send(QUOTA_BUS_ADDRESS, message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				handler.handle(reply.body());
			}
		}));
	}

	private void updateUserQuota(final String userId, long size){
		updateUserQuota(userId, size, null);
	}

	private void updateUserQuota(final String userId, long size, final Handler<Void> continuation){
		JsonObject message = new JsonObject();
		message.put("action", "updateUserQuota");
		message.put("userId", userId);
		message.put("size", size);
		message.put("threshold", threshold);

		eb.send(QUOTA_BUS_ADDRESS, message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				JsonObject obj = reply.body();
				UserUtils.addSessionAttribute(eb, userId, "storage", obj.getLong("storage"), null);
				if (obj.getBoolean("notify", false)) {
					notifyEmptySpaceIsSmall(userId);
				}

				if(continuation != null)
					continuation.handle(null);
			}
		}));
	}

	private void notifyEmptySpaceIsSmall(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()),
				"messagerie.storage", null, recipients, null, new JsonObject());
	}

}
