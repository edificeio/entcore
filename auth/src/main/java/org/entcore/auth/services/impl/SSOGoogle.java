package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.opensaml.saml2.core.Assertion;

public class SSOGoogle extends AbstractSSOProvider {
	private static final Logger log = LoggerFactory.getLogger(SSOGoogle.class);
	private static final String EMAIL = "email";

	@Override
	public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId,
						 Handler<Either<String, JsonArray>> handler) {
		final String email = getNameId(userId, host);

		if (email == null) {
			String msg = "No email domain configured for host: " + host;
			log.error("[Auth@SSOGoogle::generate] " + msg);
			handler.handle(new Either.Left<>(msg));
			return;
		}

		log.info("[Auth@SSOGoogle::email] " + email);
		handler.handle(new Either.Right<>(new JsonArray().add(new JsonObject().put(EMAIL, email))));
	}

	@Override
	public String getNameId(String userId, String host) {
		final String emailDomain = Vertx.currentContext().config()
				.getJsonObject("google-email-domain-by-host", new JsonObject())
				.getString(host);

		if (emailDomain == null || emailDomain.isEmpty()) {
			return null;
		}

		return userId + "@" + emailDomain;
	}

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		String errorMessage = "execute function not available on SSO Google implementation";
		log.error("[Auth@SSOGoogle::execute] " + errorMessage);
		handler.handle(new Either.Left<>(errorMessage));
	}
}
