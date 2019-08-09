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

package org.entcore.directory.services.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.services.UserBookService;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UserbookRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(UserbookRepositoryEvents.class);
	private final UserBookService userBookService;

	public UserbookRepositoryEvents(UserBookService userBookService) {
		super();
		this.userBookService = userBookService;
	}

	@Override
	public void mergeUsers(String keepedUserId, String deletedUserId) {
		userBookService.cleanAvatarCache(Arrays.asList(deletedUserId), res -> {

		});
	}

	@Override
	public void exportResources(String exportId, String userId, JsonArray groups, String exportPath,
			String locale, String host, Handler<Boolean> handler) {

	}

	@Override
	public void deleteGroups(JsonArray groups) {

	}

	@Override
	public void deleteUsers(JsonArray users) {
		if (users == null) return;

		if (users.size() > 1) {
			String query =
					"MATCH (u:UserBook)-[r]-(n) " +
					"WHERE (n:Hobby OR n:UserBook) AND NOT(u<--(:User)) " +
					"DETACH DELETE u, r, n";
			StatementsBuilder b = new StatementsBuilder().add(query);
			query = "MATCH (p:UserAppConf) " +
					"WHERE NOT(p<--(:User)) " +
					"DETACH DELETE p";
			b.add(query);
			b.add("MATCH (sb:ShareBookmark) WHERE NOT(sb<--(:User)) DELETE sb");
			Neo4j.getInstance().executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if (!"ok".equals(event.body().getString("status"))) {
						log.error("Error deleting userbook data : " + event.body().encode());
					}
				}
			});
		}
		List<String> userIds = users.stream().filter(u -> u instanceof JsonObject)
				.map(u -> ((JsonObject) u).getString("id")).collect(Collectors.toList());
		userBookService.cleanAvatarCache(userIds, res -> {
			if (!res) {
				log.error("Error cleaning avatars for ids : " + StringUtils.join(userIds, " "));
			}
		});
	}

}
