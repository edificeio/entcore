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

import static org.entcore.common.user.UserUtils.findVisibles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;

public class VisiblesFilter implements ResourcesProvider{

	private Neo4j neo;
	private Sql sql;

	public VisiblesFilter() {
		neo = Neo4j.getInstance();
		sql = Sql.getInstance();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void authorize(HttpServerRequest request, Binding binding,
			final UserInfos user, final Handler<Boolean> handler) {

		final String parentMessageId = request.params().get("In-Reply-To");
		final Set<String> ids = new HashSet<>();
		final String customReturn = "WHERE visibles.id IN {ids} RETURN DISTINCT visibles.id";
		final JsonObject params = new JsonObject();

		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			public void handle(final JsonObject message) {
				ids.addAll(message.getJsonArray("to", new fr.wseduc.webutils.collections.JsonArray()).getList());
				ids.addAll(message.getJsonArray("cc", new fr.wseduc.webutils.collections.JsonArray()).getList());

				final Handler<Void> checkHandler = new Handler<Void>() {
					public void handle(Void v) {
						params.put("ids", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ids)));
						findVisibles(neo.getEventBus(), user.getUserId(), customReturn, params, true, true, false, new Handler<JsonArray>() {
							public void handle(JsonArray visibles) {
								handler.handle(visibles.size() == ids.size());
							}
						});
					}
				};

				if(parentMessageId == null || parentMessageId.trim().isEmpty()){
					checkHandler.handle(null);
					return;
				}

				sql.prepared(
					"SELECT m.*  " +
					"FROM conversation.messages m " +
					"WHERE m.id = ?",
					new fr.wseduc.webutils.collections.JsonArray().add(parentMessageId),
					SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> parentMsgEvent) {
							if(parentMsgEvent.isLeft()){
								handler.handle(false);
								return;
							}

							JsonObject parentMsg = parentMsgEvent.right().getValue();
							ids.remove(parentMsg.getString("from"));
							ids.removeAll(parentMsg.getJsonArray("to", new fr.wseduc.webutils.collections.JsonArray()).getList());
							ids.removeAll(parentMsg.getJsonArray("cc", new fr.wseduc.webutils.collections.JsonArray()).getList());

							checkHandler.handle(null);
						}
					}, "cc", "to"));
			}
		});

	}

}
