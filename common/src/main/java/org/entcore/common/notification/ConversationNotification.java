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

package org.entcore.common.notification;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ConversationNotification {

	private final I18n i18n = I18n.getInstance();
	private final Renders render;
	private final Neo neo;
	private final String host;
	private final EventBus eb;
	private static final Logger log = LoggerFactory.getLogger(TimelineHelper.class);
	private static final String CONVERSATION_ADDRESS = "org.entcore.conversation";

	public ConversationNotification(Vertx vertx, EventBus eb, JsonObject config) {
		this.render = new Renders(vertx, config);
		this.neo = new Neo(vertx, eb, log);
		this.eb = eb;
		this.host = config.getString("host", "http://localhost:8009");
	}

	public void notify(final HttpServerRequest request, final String from, final JsonArray to, final JsonArray cc,
			final String subject, String template, JsonObject params,
			final Handler<Either<String, JsonObject>> result) {
		render.processTemplate(request, template, params, new Handler<String>() {
			@Override
			public void handle(String message) {
				if (message != null) {
					ConversationNotification.this.notify(request, from, to, cc,
							i18n.translate(subject, Renders.getHost(request), I18n.acceptLanguage(request)),
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
			cc = new fr.wseduc.webutils.collections.JsonArray();
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
		String language = I18n.acceptLanguage(request);
		if (language == null || language.trim().isEmpty()) {
			language = "fr";
		} else {
			String[] langs = language.split(",");
			language = langs[0];
		}
		String displayName = i18n.translate("no-reply", Renders.getHost(request), language);
		final JsonObject m = new JsonObject()
				.put("message", new JsonObject()
					.put("to", to)
					.put("cc", cc)
					.put("subject", subject)
					.put("body", message)
				)
				.put("action", "send")
				.put("userId", "no-reply-" + language)
				.put("username", displayName)
				.put("request", new JsonObject()
					.put("path", request.path())
					.put("headers", new JsonObject()
							.put("Accept-Language", I18n.acceptLanguage(request)))
				);
		String query =
				"MATCH (u:User { id : {noReplyId}}) " +
				"WITH count(*) as exists " +
				"WHERE exists = 0 " +
				"CREATE (u:User:Visible {id : {noReplyId}, displayName : {noReplyName}})";
		JsonObject params = new JsonObject()
				.put("noReplyName", displayName)
				.put("noReplyId", "no-reply-" + language).put("dest", dest);
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
					eb.send(CONVERSATION_ADDRESS, m, handlerToAsyncHandler(Neo4jResult.validUniqueResultHandler(result)));
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
