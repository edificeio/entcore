/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.CookieHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;


public abstract class GenericEventStore implements EventStore {

	protected String module;
	protected EventBus eventBus;
	protected JsonArray userBlacklist;
	protected static final Logger logger = LoggerFactory.getLogger(GenericEventStore.class);

	@Override
	public void createAndStoreEvent(String eventType, UserInfos user) {
		createAndStoreEvent(eventType, user, null);
	}

	@Override
	public void createAndStoreEvent(final String eventType, final HttpServerRequest request,
			final JsonObject customAttributes) {
		UserUtils.getUserInfos(eventBus, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				execute(user, eventType, request, customAttributes);
			}
		});
	}

	@Override
	public void createAndStoreEvent(String eventType, UserInfos user, JsonObject customAttributes) {
		execute(user, eventType, null, customAttributes);
	}

	@Override
	public void createAndStoreEvent(final String eventType, final HttpServerRequest request) {
		createAndStoreEvent(eventType, request, null);
	}

	@Override
	public void createAndStoreEvent(final String eventType, final String login) {
		String query =
				"MATCH (n:User {login : {login}}) " +
				"OPTIONAL MATCH n-[:IN]->(gp:ProfileGroup) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[:IN]->()-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN distinct n.id as userId,  p.name as type, COLLECT(distinct gp.id) as profilGroupsIds, " +
				"COLLECT(distinct c.id) as classes, COLLECT(distinct s.id) as structures";
		Neo4j.getInstance().execute(query, new JsonObject().putString("login", login),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res.size() == 1) {
					execute(UserUtils.sessionToUserInfos(
							res.<JsonObject>get(0)), eventType, null, null);
				} else {
					logger.error("Error : user " + login + " not found.");
				}

			}
		});
	}

	private void execute(UserInfos user, String eventType, HttpServerRequest request,
			JsonObject customAttributes) {
		if (user == null || !userBlacklist.contains(user.getUserId())) {
			storeEvent(generateEvent(eventType, user, request, customAttributes), new Handler<Either<String, Void>>() {
				@Override
				public void handle(Either<String, Void> event) {
					if (event.isLeft()) {
						logger.error("Error adding event : " + event.left().getValue());
					}
				}
			});
		}
	}

	private JsonObject generateEvent(String eventType, UserInfos user, HttpServerRequest request,
			JsonObject customAttributes) {
		JsonObject event = new JsonObject();
		if (customAttributes != null && customAttributes.size() > 0) {
			event.mergeIn(customAttributes);
		}
		event.putString("event-type", eventType)
				.putString("module", module)
				.putNumber("date", System.currentTimeMillis());
		if (user != null) {
			event.putString("userId", user.getUserId())
					.putString("profil", user.getType());
			if (user.getStructures() != null) {
				event.putArray("structures", new JsonArray(user.getStructures().toArray()));
			}
			if (user.getClasses() != null) {
				event.putArray("classes", new JsonArray(user.getClasses().toArray()));
			}
			if (user.getGroupsIds() != null) {
				event.putArray("groups", new JsonArray(user.getGroupsIds().toArray()));
			}
		}
		if (request != null) {
			event.putString("referer", request.headers().get("Referer"));
			event.putString("sessionId", CookieHelper.getInstance().getSigned("oneSessionId", request));
		}
		return event;
	}

	protected abstract void storeEvent(JsonObject event, Handler<Either<String, Void>> handler);

	private void initBlacklist() {
		eventBus.send("event.blacklist", new JsonObject(), new Handler<Message<JsonArray>>() {
			@Override
			public void handle(Message<JsonArray> message) {
				userBlacklist = message.body();
			}
		});
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
		this.initBlacklist();
	}

	public void setModule(String module) {
		this.module = module;
	}

}
