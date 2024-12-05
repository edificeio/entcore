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
import java.util.stream.Stream;

import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import static org.entcore.common.utils.StringUtils.isEmpty;

import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Message {
    final static public String RECIPIENT_ID     = "id";
    final static public String RECIPIENT_NAME   = "displayName";
    final static public String MSG_FROM         = "from";
    final static public String MSG_TO           = "to";
    final static public String MSG_CC           = "cc";
    final static public String MSG_CCI          = "cci";

    /**
     * Extract users and group informations from a message loaded from DB.
     * @param message message read from DB
     * @param userInfos
     * @param lang
     * @param userIndex Map of ID <-> {"id": user ID, "displayName": username}
     * @param groupIndex Map of ID <-> {"id": group ID, "displayName": groupname}
     */
    static public void extractUsersAndGroups(
            JsonObject message, UserInfos userInfos, String lang, final JsonObject userIndex, final JsonObject groupIndex
        ) {
        final String userId = userInfos.getUserId();
		final Boolean notIsSender = (!userId.equals(message.getString(MSG_FROM)));
		final List<String> userGroups = getOrElse(userInfos.getGroupsIds(), new ArrayList<String>());

        // Add connected user to index
        userIndex.put(
            userId, 
            new JsonObject().put(RECIPIENT_ID, userId).put(RECIPIENT_NAME, userInfos.getUsername())
        );

		JsonArray displayNames = getOrElse((JsonArray) message.remove("displayNames"), new JsonArray());
		for (Object o2 : displayNames) {
			if (!(o2 instanceof String)) {
				continue;
			}
			String[] a = ((String) o2).split("\\$");
			if (a.length != 4) {
				continue;
			}

            final boolean isGroup = !isEmpty(a[2]);
            final JsonObject correctIndex = isGroup ? groupIndex : userIndex;
            JsonObject newEntry = correctIndex.getJsonObject(a[0]);
            if( newEntry != null ) continue;

            newEntry = new JsonObject().put(RECIPIENT_ID, a[0]);

			if (isGroup) {
				final String groupDisplayName = isEmpty(a[3]) ? null : a[3];
				newEntry.put(RECIPIENT_NAME, UserUtils.groupDisplayName(a[2], groupDisplayName, lang));
			} else {
				newEntry.put(RECIPIENT_NAME, a[1]);
			}
            correctIndex.put(a[0], newEntry);
		}

        // Pre-format deleted users/groups names ?
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
     * Replace recipients in a message (in DB format) with those found in an index.
     * @param message message read from DB
     * @param userIndex Map of ID <-> {"id": user ID, "displayName": username, "profile": profile}
     * @param groupIndex Map of ID <-> {"id": group ID, "displayName": groupname, "nbUsers": nb users, "type": type, "subType": subtype}
     */
    static public void formatRecipients(JsonObject message, final JsonObject userIndex, final JsonObject groupIndex) {
        final String from = message.getString(MSG_FROM);
        message.put(MSG_FROM, userIndex.getJsonObject(from));

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
        Stream.of("fromName", "toName", "ccName", "cciName", "displayNames").forEach(key -> {
            message.remove(key);
        });
    }
}
