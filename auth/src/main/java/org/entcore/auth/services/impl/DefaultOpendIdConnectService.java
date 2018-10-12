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

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.JWT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class DefaultOpendIdConnectService implements OpenIdConnectService, OpenIdConnectServiceProvider {

	private final String iss;
	private final JWT jwt;
	private boolean setFederated = true;

	public DefaultOpendIdConnectService(String iss, Vertx vertx, String keysPath) {
		this.iss = iss;
		this.jwt = isNotEmpty(keysPath) ? new JWT(vertx, keysPath) : null;
	}

	public DefaultOpendIdConnectService(String iss) {
		this(iss, null, null);
	}

	@Override
	public void generateIdToken(String userId, final String clientId, final Handler<AsyncResult<String>> handler) {
		final  String query = "MATCH (u:User {id: {id}}) return u.externalId as sub, u.email as  email, u.displayName as name";
		Neo4j.getInstance().execute(query, new JsonObject().put("id", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					generatePayload(res.getJsonObject(0), clientId, handler);
				} else {
					handler.handle(new DefaultAsyncResult<String>(new RuntimeException("invalid.userId")));
				}
			}
		});
	}

	private void generatePayload(JsonObject payload, String clientId, Handler<AsyncResult<String>> handler) {
		if (payload != null) {
			final long iat = System.currentTimeMillis() / 1000;
			payload.put("iss", getIss())
					.put("aud", clientId)
					.put("iat", iat)
					.put("exp", iat + EXPIRATION_TIME);
			try {
				handler.handle(new DefaultAsyncResult<>(jwt.encodeAndSign(payload)));
			} catch (Exception e) {
				handler.handle(new DefaultAsyncResult<String>(e));
			}
		} else {
			handler.handle(new DefaultAsyncResult<String>(new RuntimeException("undefined.payload")));
		}
	}

	@Override
	public String getIss() {
		return iss;
	}

	@Override
	public void executeFederate(JsonObject payload, Handler<Either<String, Object>> handler) {
		if (iss.equals(payload.getString("iss")) && payload.getLong("exp", 0l) > (System.currentTimeMillis() / 1000)) {
			AbstractSSOProvider.executeFederateQuery(
					"MATCH (u:User { externalId : {sub}}) ", payload, null, setFederated, Neo4j.getInstance(), handler);
		} else {
			handler.handle(new Either.Left<String, Object>("invalid.openid.payload"));
		}
	}

	@Override
	public void mappingUser(String login, String password, JsonObject payload, Handler<Either<String, Object>> handler) {
		handler.handle(new Either.Left<String, Object>("unsupported"));
	}

	public void setSetFederated(boolean setFederated) {
		this.setFederated = setFederated;
	}

}
