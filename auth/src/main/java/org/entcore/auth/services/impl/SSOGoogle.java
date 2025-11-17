package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.opensaml.saml2.core.Assertion;

public class SSOGoogle extends AbstractSSOProvider {
	private static final Logger log = LoggerFactory.getLogger(SSOGoogle.class);
	private static final String EMAIL = "email";
	private static final String USERID = "userId";

	@Override
	public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId,
						 Handler<Either<String, JsonArray>> handler) {
		getEmail(userId)
				.compose(this::fillResult)
				.onSuccess(result -> handler.handle(new Either.Right<>(result)))
				.onFailure(err -> {
					log.error("[Auth@SSOGoogle::generate] Failed to generate response for Google: " + err.getMessage());
					handler.handle(new Either.Left<>(err.getMessage()));
				});
	}

	private Future<String> getEmail(String userId) {
		Promise<String> promise = Promise.promise();

		String query = "MATCH (u:User {id:{userId}})\n"
				+ "OPTIONAL MATCH (u)-[:IN]->(:Group)-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(:Action)<-[:PROVIDE]-(a:Application)\n"
				+ "WITH u, COLLECT(a) AS applications\n"
				+ "RETURN u.email AS email";

		Neo4j.getInstance()
				.execute(query, new JsonObject().put(USERID, userId), Neo4jResult.validUniqueResultHandler(event -> {
					if (event.isLeft()) {
						String err = event.left().getValue();
						log.error("[Auth@SSOGoogle::getEmail] Neo4j error for user " + userId + " : " + err);
						promise.fail(err);
						return;
					}

					JsonObject row = event.right().getValue();
					String email = row != null ? row.getString(EMAIL) : null;
					if (email != null) {
						email = email.trim();
					}

					if (email == null || email.isEmpty()) {
						String msg = "User email not found";
						log.warn("[Auth@SSOGoogle::getEmail] " + msg + " for user: " + userId);
						promise.fail(msg);
						return;
					}

					promise.complete(email);
				}));

		return promise.future();
	}

	private Future<JsonArray> fillResult(String email) {
		Promise<JsonArray> promise = Promise.promise();
		try {
			JsonArray result = new JsonArray().add(new JsonObject().put(EMAIL, email));
			promise.complete(result);
		} catch (Exception e) {
			log.error("[Auth@SSOGoogle::fillResult] Failed to build result: " + e.getMessage());
			promise.fail(e.getMessage());
		}
		return promise.future();
	}

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		String errorMessage = "execute function not available on SSO Google implementation";
		log.error("[Auth@SSOGoogle::execute] " + errorMessage);
		handler.handle(new Either.Left<>(errorMessage));
	}
}
