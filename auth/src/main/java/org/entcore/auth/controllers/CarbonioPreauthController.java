package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.auth.services.CarbonioPreauthService;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;
import java.util.Optional;

public class CarbonioPreauthController extends BaseController {
	CarbonioPreauthService carbonioPreauthService;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		Optional<String> carbonioBaseUrl = Optional.ofNullable(config.getString("carbonioBaseUrl"));
		Optional<String> carbonioDomainKey = Optional.ofNullable(config.getString("carbonioDomainKey"));

		if (!carbonioBaseUrl.isPresent() || !carbonioDomainKey.isPresent()) {
			throw new IllegalArgumentException("Both carbonio-base-url and carbonio-domain-key must be configured");
		}

		carbonioPreauthService = new CarbonioPreauthService(carbonioBaseUrl.get(), carbonioDomainKey.get());
	}

	@Get("/carbonio/preauth")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void preauth(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos == null) {
				unauthorized(request, "User not found");
			} else {
				String email = userInfos.getEmail();

				Either<String, String> result = carbonioPreauthService.generatePreauthUrl(email);

				if (result.isLeft()) {
					badRequest(request, result.left().getValue());
					return;
				}

				String carbonioPreAuthUrl = result.right().getValue();
				request.response().setStatusCode(302);
				request.response().putHeader("Location", carbonioPreAuthUrl);
				request.response().end();
			}
		});
	}
}
