package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.auth.services.CarbonioPreauthService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class CarbonioPreauthController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(CarbonioPreauthController.class);

	CarbonioPreauthService carbonioPreauthService;
	String carbonioBaseUrl;
	String carbonioRedirectUrl;
	String carbonioDomainKey;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		carbonioBaseUrl = config.getString("carbonio-base-url");
		carbonioRedirectUrl = config.getString("carbonio-redirect-url");
		carbonioDomainKey = config.getString("carbonio-domain-key");

		carbonioPreauthService = new CarbonioPreauthService(carbonioRedirectUrl, carbonioDomainKey);
	}

	@Get("/carbonio/preauth")
	@SecuredAction("carbonio.preauth")
	public void preauth(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos == null) {
				log.warn("Carbonio preauth failed: user not found in session");
				unauthorized(request, "User not found");
			} else {
				final String userAlias = getUserAlias(userInfos);

				Either<String, String> result = carbonioPreauthService.generatePreauthUrl(userAlias);
				if (result.isLeft()) {
					log.error("Failed to generate Carbonio preauth URL for user " + userAlias + ": " + result.left()
							.getValue());
					badRequest(request, result.left().getValue());
					return;
				}

				String carbonioPreAuthUrl = result.right().getValue();
				log.info("Carbonio preauth redirect for user: " + userInfos.getUserId());
				request.response().setStatusCode(302);
				request.response().putHeader("Location", carbonioPreAuthUrl);
				request.response().end();
			}
		});
	}

	private String getUserAlias(UserInfos userInfos) {
		final String domain = carbonioBaseUrl
				.replaceFirst("https?://", "")
				.replaceFirst("/.*$", "");

		return String.format("%s@%s", userInfos.getUserId(), domain);
	}
}
