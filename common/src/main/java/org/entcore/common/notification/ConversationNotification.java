/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.notification;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

public class ConversationNotification {

	private final I18n i18n = I18n.getInstance();
	private final Renders render;
	private final Neo neo;
	private final String host;
	private final EventBus eb;
	private static final Logger log = LoggerFactory.getLogger(TimelineHelper.class);
	private static final String CONVERSATION_ADDRESS = "org.entcore.conversation";

	public ConversationNotification(Vertx vertx, EventBus eb, Container container) {
		this.render = new Renders(vertx, container);
		this.neo = new Neo(eb, log);
		this.eb = eb;
		this.host = container.config().getString("host", "http://localhost:8009");
	}

	public void notify(final HttpServerRequest request, final String from, final JsonArray to, final JsonArray cc,
			final String subject, String template, JsonObject params,
			final Handler<Either<String, JsonObject>> result) {
		render.processTemplate(request, template, params, new Handler<String>() {
			@Override
			public void handle(String message) {
				if (message != null) {
					ConversationNotification.this.notify(request, from, to, cc,
							i18n.translate(subject, request.headers().get("Accept-Language")),
							message, result);
				} else {
					log.error("Unable to send conversation notification.");
					result.handle(new Either.Left<String, JsonObject>("Unable to send conversation notification."));
				}
			}
		});
	}

	public void notify(final HttpServerRequest request, String from, JsonArray to, JsonArray cc, String subject,
		String message, final Handler<Either<String, JsonObject>> result) {
		if (cc == null) {
			cc = new JsonArray();
		}
		if (subject == null || message == null || to == null || from == null) {
			result.handle(new Either.Left<String, JsonObject>("Conversation notification : invalid parameters"));
			log.warn("Conversation notification : invalid parameters");
			return;
		}
		JsonArray dest = to.copy();
		for (Object o : cc) {
			dest.add(o);
		}
		String language = request.headers().get("Accept-Language");
		if (language == null || language.trim().isEmpty()) {
			language = "fr";
		}
		String displayName = i18n.translate("no-reply", language);
		final JsonObject m = new JsonObject()
				.putObject("message", new JsonObject()
					.putArray("to", to)
					.putArray("cc", cc)
					.putString("subject", subject)
					.putString("body", message)
				)
				.putString("action", "send")
				.putString("userId", "no-reply-" + language)
				.putString("username", displayName)
				.putObject("request", new JsonObject()
					.putString("path", request.path())
					.putObject("headers", new JsonObject()
							.putString("Accept-Language",request.headers().get("Accept-Language")))
				);
		String query =
				"MATCH (u:User { id : {noReplyId}}) " +
				"WITH count(*) as exists " +
				"WHERE exists = 0 " +
				"CREATE (u:User:Visible {id : {noReplyId}, displayName : {noReplyName}}) " +
				"WITH u " +
				"CREATE UNIQUE u-[:HAS_CONVERSATION]->(c:Conversation { userId : {noReplyId}, active : {true} }), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fi:ConversationFolder:ConversationSystemFolder { name : {inbox}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fo:ConversationFolder:ConversationSystemFolder { name : {outbox}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fd:ConversationFolder:ConversationSystemFolder { name : {draft}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(ft:ConversationFolder:ConversationSystemFolder { name : {trash}}) ";
		JsonObject params = new JsonObject().putString("inbox", "INBOX").putString("outbox", "OUTBOX")
				.putString("draft", "DRAFT").putString("trash", "TRASH").putBoolean("true", true)
				.putString("noReplyName", displayName)
				.putString("noReplyId", "no-reply-" + language).putArray("dest", dest);
		StatementsBuilder sb = new StatementsBuilder()
				.add(query, params);
		sb.add(
				"MATCH (dest:User), (u:User {id : {noReplyId}}) " +
				"WHERE dest.id IN {dest} " +
				"CREATE UNIQUE u-[:COMMUNIQUE_DIRECT]->dest ",
				params
		);
		neo.executeTransaction(sb.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if ("ok".equals(message.body().getString("status"))) {
					eb.send(CONVERSATION_ADDRESS, m, Neo4jResult.validUniqueResultHandler(result));
				} else {
					result.handle(new Either.Left<String, JsonObject>(message.body().getString("message")));
				}
			}
		});
	}

	public String getHost() {
		return host;
	}

}
