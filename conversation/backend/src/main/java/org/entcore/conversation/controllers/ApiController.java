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

import java.util.Optional;

import org.entcore.common.http.filter.ResourceFilter;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import org.entcore.common.user.UserInfos;
import static org.entcore.common.user.UserUtils.getAuthenticatedUserInfos;
import static org.entcore.common.utils.StringUtils.isEmpty;
import org.entcore.conversation.filters.MessageUserFilter;
import org.entcore.conversation.filters.SystemOrUserFolderFilter;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.util.MessageUtil;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import static fr.wseduc.webutils.Utils.getOrElse;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApiController extends BaseController {
	public static final String RESOURCE_NAME = "message";

	private ConversationService conversationService;

	private enum ConversationEvent {
		GET_RESOURCE, ACCESS
	}

	public ApiController() {
	}

	public ApiController setConversationService(final ConversationService conversationService) {
		this.conversationService = conversationService;
		return this;
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
			log.error("Unable to list and format messages from folder id="+folderId, throwable);
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
		final String acceptedLanguage = I18n.acceptLanguage(request);

		getAuthenticatedUserInfos(eb, request)
		.compose( user -> getAndFormat(id, user, acceptedLanguage) )
		.onSuccess( message -> {
			renderJson(request, message);
		})
		.onFailure( throwable -> {
			log.error("Unable to get and format message id="+id, throwable);
			JsonObject error = new JsonObject().put("error", throwable.getMessage());
			renderJson(request, error, 400);
		});
	}

	// List user's folders with a given depth.
	@Get("api/folders")
	@SecuredAction(value = "conversation.folder.list", type = ActionType.AUTHENTICATED)
	public void listFolders(final HttpServerRequest request) {
		Integer depth = parseQueryParam(request, "depth", 1);
		getAuthenticatedUserInfos(eb, request)
		.onSuccess(user -> {
			conversationService.getFolderTree(user, depth, Optional.empty(), arrayResponseHandler(request));
		});
	}

	/** Utility adapter */
	private Future<JsonObject> getAndFormat(String id, UserInfos userInfos, String lang) {
		final Promise<JsonObject> promise = Promise.promise();
		final JsonObject userIndex = new JsonObject();
		final JsonObject groupIndex = new JsonObject();
		conversationService.get(id, userInfos, either -> {
			if (either.isRight()) {
				final JsonObject message = either.right().getValue();
				// Extract distinct users and groups.
				MessageUtil.computeUsersAndGroupsDisplayNames(message, userInfos, lang, userIndex, groupIndex);

				MessageUtil.loadUsersAndGroupsDetails(eb, userInfos, userIndex, groupIndex)
				.onSuccess( unused -> {
					MessageUtil.formatRecipients(message, userIndex, groupIndex);
					promise.complete(message);
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
					// Extract distinct users and groups.
					MessageUtil.computeUsersAndGroupsDisplayNames((JsonObject) message, userInfos, lang, userIndex, groupIndex);
				}

				MessageUtil.loadUsersAndGroupsDetails(eb, userInfos, userIndex, groupIndex)
				.onSuccess( unused -> {
					for (Object m : messages) {
						if (!(m instanceof JsonObject)) {
							continue;
						}
						MessageUtil.formatRecipients((JsonObject) m, userIndex, groupIndex);
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

}
