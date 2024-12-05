/* Copyright © "Edifice", 2024
 *
 * This program is published by "Edifice".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Edifice" with a reference to the website: https://edifice.io/.
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

import fr.wseduc.rs.Get;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;

import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.filters.*;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.Neo4jConversationService;
import org.entcore.conversation.util.Message;

import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.vertx.java.core.http.RouteMatcher;

import java.util.*;
import java.util.stream.Collector;

import static fr.wseduc.webutils.Utils.getOrElse;

import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getAuthenticatedUserInfos;
import static org.entcore.common.utils.StringUtils.isEmpty;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import io.vertx.core.Promise;
import io.vertx.core.Future;

public class ApiController extends BaseController {
	public static final String RESOURCE_NAME = "message";

	private final static String QUOTA_BUS_ADDRESS = "org.entcore.workspace.quota";

	private Storage storage;
	private int threshold;

	private ConversationService conversationService;
	// private Neo4jConversationService userService;
	private TimelineHelper notification;
	private EventHelper eventHelper;

	private enum ConversationEvent {
		GET_RESOURCE, ACCESS
	}

	private final String exportPath;

	public ApiController(Storage storage, String exportPath) {
		this.storage = storage;
		this.exportPath = exportPath;
	}

	public ApiController setConversationService(final ConversationService conversationService) {
		this.conversationService = conversationService;
		return this;
	}
	// public ApiController setUserService(final UserService userService) {
	// this.userService = userService;
	// return this;
	// }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		// TODO clean up unused thingies
		notification = new TimelineHelper(vertx, eb, config);
		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Conversation.class.getSimpleName());
		this.eventHelper = new EventHelper(eventStore);
		this.threshold = config.getInteger("alertStorage", 80);
	}

	@Get("api/folders/:folderId/messages")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SystemOrUserFolderFilter.class)
	public void ListFolderMessages(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");
		final Integer page = parseQueryParam(request, "page", 0);
		final Integer page_size = parseQueryParam(request, "page_size", ConversationService.LIST_LIMIT);
		final Boolean unread = parseQueryParam(request, "unread", false);
		final String search = request.params().get("search");

		if (isEmpty(folderId)) {
			badRequest(request);
			return;
		}
		if(search != null  && search.trim().length() < 3){
			badRequest(request);
			return;
		}

		final String acceptedLanguage = I18n.acceptLanguage(request);

		getAuthenticatedUserInfos(eb, request)
		.compose( user -> listAndFormat(folderId, unread, user, page, page_size, search, acceptedLanguage) )
		.onSuccess( messages -> {
			renderJson(request, messages);
		})
		.onFailure( throwable -> {
			JsonObject error = new JsonObject().put("error", throwable.getMessage());
			renderJson(request, error, 400);
		});
	}

	@Get("api/messages/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void GetFullMessage(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (isEmpty(id)) {
			badRequest(request);
			return;
		}
		getAuthenticatedUserInfos(eb, request)
		.onSuccess( user -> {
			conversationService.get(id, user, either -> {
				if (either.isRight()) {
					translateGroupsNames(either.right().getValue(), user,  I18n.acceptLanguage(request));
					renderJson(request, either.right().getValue());
					// eventStore.createAndStoreEvent(ConversationEvent.GET_RESOURCE.name(),
					// request,
					// new JsonObject().put("resource", id));
				} else {
					JsonObject error = new JsonObject()
							.put("error", either.left().getValue());
					renderJson(request, error, 400);
				}
			});
		});
	}

	// List folders at a given depth, or trashed folders at depth 1 only.
	@Get("api/folders")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listFolders(final HttpServerRequest request) {
		final String parentId = request.params().get("parentId");
		final String listTrash = request.params().get("trash");

		getAuthenticatedUserInfos(eb, request)
		.onSuccess(user -> {
			if (listTrash != null) {
				conversationService.listTrashedFolders(user, arrayResponseHandler(request));
			} else {
				conversationService.listFolders(parentId, user, arrayResponseHandler(request));
			}
		});
	}

	@Get("api/userfolders/list")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listUserFolders(final HttpServerRequest request) {
		final String parentId = request.params().get("parentId");
		final String unread = request.params().get("unread");
		final Boolean b = unread != null && !unread.isEmpty() ? Boolean.valueOf(unread) : null;
		getAuthenticatedUserInfos(eb, request)
		.onSuccess( user -> {
			conversationService.listUserFolders(Optional.ofNullable(parentId), user, b, arrayResponseHandler(request));
		});
	}

	/** Utility adapter */
	private Future<JsonArray> listAndFormat(String folderId, Boolean unread, UserInfos userInfos, int page, int page_size, String search, String lang) {
		final Promise<JsonArray> promise = Promise.promise();
		final JsonObject userIndex = new JsonObject();
		final JsonObject groupIndex = new JsonObject();
		conversationService.list(folderId, unread, userInfos, page, page_size, search, either -> {
			if (either.isRight()) {
				final JsonArray messages = either.right().getValue();
				for (Object message : messages) {
					if (!(message instanceof JsonObject)) {
						continue;
					}
					// Extract users and groups.
					Message.extractUsersAndGroups((JsonObject) message, userInfos, lang, userIndex, groupIndex);
				}

				// Gather additional users and groups information.
				Future.all(
					loadUsersDetails(userInfos.getUserId(), userIndex),
					loadGroupsDetails(userInfos.getUserId(), groupIndex)
				)
				// Compose final response
				.onSuccess(infos -> {
					JsonArray usersInfo = infos.resultAt(0);
					usersInfo.stream().forEach(ui -> {
						if(!(ui instanceof JsonObject)) return;
						final JsonObject info = (JsonObject) ui;
						final JsonObject user = userIndex.getJsonObject(info.getString("id"));
						if(user!=null) {
							user.put("profile", info.getString("type"));
						}
					});
					JsonArray groupsInfo = infos.resultAt(1);
					groupsInfo.stream().forEach(gi -> {
						if(!(gi instanceof JsonObject)) return;
						final JsonObject info = (JsonObject) gi;
						final JsonObject group = groupIndex.getJsonObject(info.getString("id"));
						if(group!=null ) {
							group.put("size", info.getInteger("nbUsers"));
							group.put("type", info.getString("type"));
							group.put("subType", info.getString("subType"));
						}
					});

					for (Object m : messages) {
						if (!(m instanceof JsonObject)) {
							continue;
						}
						Message.formatRecipients((JsonObject) m, userIndex, groupIndex);
					}
					promise.complete(messages);
				})
				.onFailure( throwable -> {
					promise.fail(throwable.getMessage());
				});
			} else {
				promise.fail(either.left().getValue());
			}
		});
		return promise.future();
	}

	private Future<JsonArray> loadUsersDetails(final String userId, final JsonObject userIndex) {
		Promise<JsonArray> promise = Promise.promise();
		JsonObject action = new JsonObject()
		.put("action", "list-users")
		.put("userIds", userIndex.stream().map(entry->entry.getKey())
				.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add)))
		.put("itself", Boolean.TRUE)
		.put("excludeUserId", userId);
        eb.request("directory", action, handlerToAsyncHandler(event -> {
            JsonArray res = event.body().getJsonArray("result", new JsonArray());
            if ("ok".equals(event.body().getString("status")) && res != null) {
                promise.complete(res);
            } else {
                promise.fail("User not found");
            }
        }));
		return promise.future();
	}

	private Future<JsonArray> loadGroupsDetails(final String userId, final JsonObject groupIndex) {
		Promise<JsonArray> promise = Promise.promise();
		JsonObject action = new JsonObject()
		.put("action", "getGroupsInfos")
		.put("userId", userId)
		.put("groupIds", groupIndex.stream().map(entry->entry.getKey())
				.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add)));
		eb.request("directory", action, handlerToAsyncHandler(event -> {
            JsonArray res = event.body().getJsonArray("result", new JsonArray());
            if ("ok".equals(event.body().getString("status")) && res != null) {
                promise.complete(res);
            } else {
                promise.fail("Groups not found");
            }
        }));
		return promise.future();
	}

	/** Utility method to read a query param and convert it to an Integer. */
	private Integer parseQueryParam(final HttpServerRequest request, String param, final Integer defaultValue) {
		final String paramValue = getOrElse(request.params().get(param), "" + defaultValue, false);
		try {
			return Integer.valueOf(paramValue);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/** Utility method to read a query param and convert it to a Boolean. */
	private Boolean parseQueryParam(final HttpServerRequest request, String param, final Boolean defaultValue) {
		return Boolean.valueOf(getOrElse(request.params().get(param), "" + defaultValue, false));
	}

	private void translateGroupsNames(JsonObject message, UserInfos userInfos, String lang) {
		final JsonArray cci = getOrElse(message.getJsonArray("cci"), new JsonArray());
		final JsonArray cc = getOrElse(message.getJsonArray("cc"), new JsonArray());
		final JsonArray to = getOrElse(message.getJsonArray("to"), new JsonArray());
		final String from = message.getString("from");
		final Boolean notIsSender = (!userInfos.getUserId().equals(from));
		final List<String> userGroups = getOrElse(userInfos.getGroupsIds(), new ArrayList<String>());

		JsonArray d3 = new JsonArray();
		for (Object o2 : getOrElse(message.getJsonArray("displayNames"), new JsonArray())) {
			if (!(o2 instanceof String)) {
				continue;
			}
			String [] a = ((String) o2).split("\\$");
			if (a.length != 4) {
				continue;
			}

			if (notIsSender && cci.contains(a[0]) && !cc.contains(a[0]) && !to.contains(a[0]) && !from.equals(a[0])) continue;
			JsonArray d2 = new JsonArray().add(a[0]);
			if (a[2] != null && !a[2].trim().isEmpty()) {
				final String groupDisplayName = (a[3] != null && !a[3].trim().isEmpty()) ? a[3] : null;
				d2.add(UserUtils.groupDisplayName(a[2], groupDisplayName, lang));
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
			JsonArray d2 = new JsonArray();
			message.put("toName", d2);
			for (Object o : toName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, lang));
			}
		}
		JsonArray ccName = message.getJsonArray("ccName");
		if (ccName != null) {
			JsonArray d2 = new JsonArray();
			message.put("ccName", d2);
			for (Object o : ccName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, lang));
			}
		}
		JsonArray cciName = message.getJsonArray("cciName");
		if (cciName != null) {
			JsonArray d2 = new JsonArray();
			message.put("cciName", d2);
			for (Object o : cciName) {
				if (!(o instanceof String)) {
					continue;
				}
				d2.add(UserUtils.groupDisplayName((String) o, null, lang));
			}
		}

		if (notIsSender) {
			//keep cci for user recipient only
			final JsonArray newCci = new JsonArray();
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
				JsonArray d2 = new JsonArray().add(userInfos.getUserId());
				d2.add(userInfos.getUsername());
				d2.add(false);
				d3.add(d2);
			}

			message.put("cci", newCci);
			message.put("cciName", new JsonArray());
		}
	}

}
