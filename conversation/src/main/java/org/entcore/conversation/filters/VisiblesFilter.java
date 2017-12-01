/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
				ids.addAll(message.getJsonArray("to", new JsonArray()).getList());
				ids.addAll(message.getJsonArray("cc", new JsonArray()).getList());

				final Handler<Void> checkHandler = new Handler<Void>() {
					public void handle(Void v) {
						params.put("ids", new JsonArray(new ArrayList<>(ids)));
						findVisibles(neo.getEventBus(), user.getUserId(), customReturn, params, true, true, true, new Handler<JsonArray>() {
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
					new JsonArray().add(parentMessageId),
					SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> parentMsgEvent) {
							if(parentMsgEvent.isLeft()){
								handler.handle(false);
								return;
							}

							JsonObject parentMsg = parentMsgEvent.right().getValue();
							ids.remove(parentMsg.getString("from"));
							ids.removeAll(parentMsg.getJsonArray("to", new JsonArray()).getList());
							ids.removeAll(parentMsg.getJsonArray("cc", new JsonArray()).getList());

							checkHandler.handle(null);
						}
					}, "cc", "to"));
			}
		});

	}

}
