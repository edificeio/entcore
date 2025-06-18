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
package org.entcore.conversation.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

/**
 * Utility class for handling messages, particularly for decoding display names stored in the database,
 * extracting users and groups from messages, and formatting recipients.
 */
public class MessageUtil {
    /*
     * Constants representing various message fields.
     */
    final static public String RECIPIENT_ID = "id";
    final static public String RECIPIENT_NAME = "displayName";
    final static public String MSG_FROM = "from";
    final static public String MSG_FROM_NAME = "fromName";
    final static public String MSG_TO = "to";
    final static public String MSG_CC = "cc";
    final static public String MSG_CCI = "cci";
    final static public String FROM_DELETED_ID = "FROM_DELETED_ID";

    /**
     * Extracts users and groups from a message loaded from the database and populates the user and group indices.
     * Also hides CCI recipients in the message who should not be visible.
     * @param message the message read from the database
     * @param userInfos the user information
     * @param lang the language
     * @param userIndex Map of ID <-> {"id": user ID, "displayName": username}
     * @param groupIndex Map of ID <-> {"id": group ID, "displayName": groupname}
     */
    static public void computeUsersAndGroupsDisplayNames(
            JsonObject message, UserInfos userInfos, String lang, final JsonObject userIndex, final JsonObject groupIndex) {
        final String userId = userInfos.getUserId();
		final Boolean notIsSender = (!userId.equals(message.getString(MSG_FROM)));
		final List<String> userGroups = getOrElse(userInfos.getGroupsIds(), new ArrayList<>());

        if(StringUtils.isEmpty(message.getString(MSG_FROM))) {
            userIndex.put(
                    FROM_DELETED_ID,
                    JsonObject.of(RECIPIENT_ID, FROM_DELETED_ID, RECIPIENT_NAME, message.getString(MSG_FROM_NAME))
            );
            message.put(MSG_FROM, FROM_DELETED_ID);
        }

        // Add connected user to index
        userIndex.put(
            userId, 
            JsonObject.of(RECIPIENT_ID, userId, RECIPIENT_NAME, userInfos.getUsername())
        );

		getOrElse((JsonArray) message.remove("displayNames"), new JsonArray())
		.stream()
        .filter(encodedDisplayName -> (encodedDisplayName instanceof String))
        .map(encodedDisplayName -> DecodedDisplayName.decode((String)encodedDisplayName, lang))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(decoded -> {
            final JsonObject index = decoded.ofGroup() ? groupIndex : userIndex;
            JsonObject newEntry = index.getJsonObject(decoded.getId());
            if( newEntry == null ) {
                index.put(
                    decoded.getId(), 
                    JsonObject.of(RECIPIENT_ID, decoded.getId(), RECIPIENT_NAME, decoded.getDisplayName())
                );
            }
        });

        /* NOTE JCBE 2024-12-16 : 
         * this code mimics (and simplifies) the implementation of ConversationController.translateGroupsNames()
         * Excepting the management of cci and cciName, i do not understand its utility.
         * Maybe pre-formating deleted users/groups displayNames ?
         * Anyway, it should not cause any harm, so let's keep it there.
         */
        Stream.of("toName", "ccName", "cciName").forEach(field -> {
            if(notIsSender && "cciName".equals(field)) {
                // keep cci for user recipient only
                final JsonArray newCci = new JsonArray();
                final JsonArray cci = getOrElse(message.getJsonArray("cci"), new JsonArray());
                if (cci.contains(userId)) {
                    newCci.add(userId);
                } else if (!userGroups.isEmpty()) {
                    for (final String groupId : userGroups) {
                        if (cci.contains(groupId)) {
                            newCci.add(userId);
                            break;
                        }
                    }
                }
                message.put(MSG_CCI, newCci);
                message.put("cciName", new JsonArray());
            } else {
                JsonArray array = message.getJsonArray(field);
                for (int i=0; array!=null && i<array.size(); i++) {
                    Object o = array.getValue(i);
                    if (!(o instanceof String)) {
                        continue;
                    }
                    array.set(i, UserUtils.groupDisplayName((String) o, null, lang));
                }
            }
        });
    }

    /**
     * Replaces recipients in a message (in DB format) with those found in an index.
     * @param message the message read from the database
     * @param userIndex the user index
     * @param groupIndex the group index
     */
    static public void formatRecipients(JsonObject message, final JsonObject userIndex, final JsonObject groupIndex) {
        final String from = message.getString(MSG_FROM);
        boolean isDeleted = message.getString(MSG_FROM).equals(FROM_DELETED_ID);
        message.put(MSG_FROM, userIndex.getJsonObject(from));
        if(isDeleted) {
            JsonObject fromUser = userIndex.getJsonObject(from);
            fromUser.put(RECIPIENT_ID, "");
            message.put(MSG_FROM, fromUser);
        }

        Stream.of(MSG_TO, MSG_CC, MSG_CCI).forEach(key -> {
            JsonArray recipients = (JsonArray) message.remove(key);
            final JsonArray users = new JsonArray();
            final JsonArray groups = new JsonArray();
            message.put(key, new JsonObject().put("users", users).put("groups", groups));

            if( !(recipients instanceof JsonArray) ) return;
            for(int i=0; i<recipients.size(); i++) {
                final String id = recipients.getString(i);
                JsonObject recipient;
                if((recipient = userIndex.getJsonObject(id))!=null) {
                    users.add(recipient);
                } else if((recipient = groupIndex.getJsonObject(id))!=null) {
                    groups.add(recipient);
                }
            }
        });

        // Final clean up
        Stream.of("fromName", "toName", "ccName", "cciName", "displayNames", "text_searchable").forEach(key -> {
            message.remove(key);
        });
    }

    /**
     * Asynchronous utility method to get additional information about users and groups.
     * @param eb the event bus
     * @param userInfos the user information
     * @param userIndex the user index
     * @param groupIndex the group index
     * @return a Future representing the completion of the operation
     */
    static public Future<Void> loadUsersAndGroupsDetails(final EventBus eb, final UserInfos userInfos, final JsonObject userIndex, final JsonObject groupIndex) {
		// Gather additional users and groups information.
		return Future.join(
			loadUsersDetails(eb, userInfos.getUserId(), userIndex),
			loadGroupsDetails(eb, userInfos.getUserId(), groupIndex)
		)
		// Compose final response
		.map( infos -> {
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
            return null; // Avoid a warning
		});
    }

    /**
     * Loads additional details about users.
     * @param eb the event bus
     * @param userId the user ID
     * @param userIndex the user index
     * @return a Future representing the completion of the operation
     */
    static private Future<JsonArray> loadUsersDetails(final EventBus eb, final String userId, final JsonObject userIndex) {
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

    /**
     * Loads additional details about groups.
     * @param eb the event bus
     * @param userId the user ID
     * @param groupIndex the group index
     * @return a Future representing the completion of the operation
     */
    static private Future<JsonArray> loadGroupsDetails(final EventBus eb, final String userId, final JsonObject groupIndex) {
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
}
