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
import org.entcore.common.cache.Cache;
import org.entcore.common.cache.CacheOperation;
import org.entcore.common.cache.CacheScope;
import org.entcore.common.events.EventHelper;
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
import static org.entcore.common.utils.StringUtils.isEmpty;

public class ApiController extends BaseController {
	public static final String RESOURCE_NAME = "message";

	private final static String QUOTA_BUS_ADDRESS = "org.entcore.workspace.quota";

	private Storage storage;
	private int threshold;

	private ConversationService conversationService;
//	private Neo4jConversationService userService;
	private TimelineHelper notification;
	private EventHelper eventHelper;
	private enum ConversationEvent {GET_RESOURCE, ACCESS }
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
	// 	this.userService = userService;
	// 	return this;
	// }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		notification = new TimelineHelper(vertx, eb, config);
		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Conversation.class.getSimpleName());
		this.eventHelper =  new EventHelper(eventStore);
		this.threshold = config.getInteger("alertStorage", 80);
	}

	/**
	 * Utility method to read a query param and convert it to an Integer.
	 */
	private Integer parseQueryParam(final HttpServerRequest request, String param, final Integer defaultValue) {
		final String paramValue = getOrElse(request.params().get(param), ""+defaultValue, false);
		try {
			return Integer.valueOf(paramValue);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Utility method to read a query param and convert it to a Boolean.
	 */
	private Boolean parseQueryParam(final HttpServerRequest request, String param, final Boolean defaultValue) {
		return Boolean.valueOf( getOrElse(request.params().get(param), ""+defaultValue, false) );
	}


	@Get("api/folders/:folderId/messages")
	@SecuredAction(value = "conversation.list", type = ActionType.AUTHENTICATED)
	public void ListFolderMessages(final HttpServerRequest request) {
		final String folderId = request.params().get("folderId");
		final Integer page = parseQueryParam(request, "page", Integer.valueOf(0));
		final Integer page_size = parseQueryParam(request, "page_size", (Integer) null);
		final Boolean unread = parseQueryParam(request, "unread", Boolean.valueOf(false));
		final String search = request.params().get("search");

		if (isEmpty(folderId)) {
			badRequest(request);
			return;
		}
		if(search != null  && search.trim().length() < 3){
			badRequest(request);
			return;
		}

		getUserInfos(eb, request, user -> {
			if (user != null) {
				conversationService.list(folderId, "true", unread, user, page, search, either -> {
					if (either.isRight()) {
						for (Object o : either.right().getValue()) {
							if (!(o instanceof JsonObject)) {
								continue;
							}
							translateGroupsNames((JsonObject) o, user, request);
						}
						renderJson(request, either.right().getValue());
					} else {
						JsonObject error = new JsonObject()
								.put("error", either.left().getValue());
						renderJson(request, error, 400);
					}
				});
			} else {
				unauthorized(request);
			}
		});
	}

	private void translateGroupsNames(JsonObject message, UserInfos userInfos, HttpServerRequest request) {
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
			if (!isEmpty(a[2])) {
				final String groupDisplayName = isEmpty(a[3]) ? null : a[3];
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
			JsonArray d2 = new JsonArray();
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
			JsonArray d2 = new JsonArray();
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
			JsonArray d2 = new JsonArray();
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

	@Get("api/messages/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(MessageUserFilter.class)
	public void GetFullMessage(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (isEmpty(id)) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, user -> {
			if (user != null) {
				conversationService.get(id, user, either -> {
					if (either.isRight()) {
						translateGroupsNames(either.right().getValue(), user, request);
						renderJson(request, either.right().getValue());
						// eventStore.createAndStoreEvent(ConversationEvent.GET_RESOURCE.name(), request,
						// 		new JsonObject().put("resource", id));
					} else {
						JsonObject error = new JsonObject()
								.put("error", either.left().getValue());
						renderJson(request, error, 400);
					}
				});
			} else {
				unauthorized(request);
			}
		});
	}

	//List folders at a given depth, or trashed folders at depth 1 only.
	@Get("api/folders")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listFolders(final HttpServerRequest request){
		final String parentId = request.params().get("parentId");
		final String listTrash = request.params().get("trash");

		Handler<UserInfos> userInfosHandler = user -> {
			if(user == null){
				unauthorized(request);
				return;
			}
			if(listTrash != null){
				conversationService.listTrashedFolders(user, arrayResponseHandler(request));
			} else {
				conversationService.listFolders(parentId, user, arrayResponseHandler(request));
			}
		};

		UserUtils.getUserInfos(eb, request, userInfosHandler);
	}

	@Get("api/userfolders/list")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listUserFolders(final HttpServerRequest request){
		final String parentId = request.params().get("parentId");
		final String unread = request.params().get("unread");
		final Boolean b = unread != null && !unread.isEmpty()? Boolean.valueOf(unread) : null;
		UserUtils.getUserInfos(eb, request, user -> {
			if(user == null){
				unauthorized(request);
				return;
			}
			conversationService.listUserFolders(Optional.ofNullable(parentId), user, b, arrayResponseHandler(request));
		});
	}

/*
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
*/
}
