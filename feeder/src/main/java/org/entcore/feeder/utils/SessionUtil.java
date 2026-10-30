/*
 * Copyright © "Open Digital Education", 2026
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

 */

package org.entcore.feeder.utils;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;

public final class SessionUtil {

	private static final Logger log = LoggerFactory.getLogger(SessionUtil.class);

	private SessionUtil() {
	}

	/**
	 * Drops the web and mobile sessions and the OAuth2 tokens of the given users, so that their connected
	 * clients are logged out. Users are processed one after the other : a pre-delete batch can hold thousands
	 * of users and revoking them all at once would flood the session module and MongoDB.
	 * @param eb Event bus used to reach the session module
	 * @param userIds Ids of the users whose sessions must be revoked
	 */
	public static void revokeUsers(EventBus eb, JsonArray userIds) {
		revokeUser(eb, userIds, 0);
	}

	private static void revokeUser(EventBus eb, JsonArray userIds, int index) {
		if (index >= userIds.size()) {
			return;
		}
		final String userId = userIds.getString(index);
		UserUtils.deletePermanentSession(eb, userId, null, null, deleted -> {
			if (!Boolean.TRUE.equals(deleted)) {
				log.error("Error delete permanent sessions and OAuth2 tokens with userId : " + userId);
			}
			UserUtils.deleteCacheSession(eb, userId, null, dropped -> {
				if (dropped == null) {
					log.error("Error delete cache sessions with userId : " + userId);
				}
				revokeUser(eb, userIds, index + 1);
			});
		});
	}

}
