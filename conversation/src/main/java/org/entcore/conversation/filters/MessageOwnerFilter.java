/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.conversation.filters;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;
import org.entcore.conversation.service.impl.ReactivePGClient;
import org.entcore.conversation.service.impl.ReactiveSql;

public class MessageOwnerFilter implements ResourcesProvider {

	private final ReactivePGClient sql;

	public MessageOwnerFilter(){
		final Vertx vertx = Vertx.currentContext().owner();
		final JsonObject config = vertx.getOrCreateContext().config();
		this.sql = new ReactivePGClient(vertx, config);
	}

	public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {

		String messageId = request.params().get("id");

		if(messageId == null || messageId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"SELECT count(distinct m) AS number FROM conversation.messages m " +
			"JOIN conversation.usermessages um ON m.id = um.message_id " +
			"WHERE um.user_id = $1 AND um.message_id = $2 AND m.from = $3";

		final Tuple values = Tuple.tuple()
			.addString(user.getUserId())
			.addString(messageId)
			.addString(user.getUserId());

		request.pause();

		sql.withReadOnlyTransaction(connection ->
			sql.prepared(query, values, connection)
			.onComplete(r -> ReactiveSql.validUniqueResult(r, event -> {
					request.resume();
					if(event.isLeft()){
						handler.handle(false);
						return;
					}

					int count = event.right().getValue().getInteger("number", 0);
					handler.handle(count == 1);
				})
			)
		);
	}

}
