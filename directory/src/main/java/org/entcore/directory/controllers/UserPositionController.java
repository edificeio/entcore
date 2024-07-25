package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.pojo.UserPositionSource;
import org.entcore.directory.services.UserPositionService;

import java.util.Optional;
import java.util.Set;

public class UserPositionController extends BaseController {

	private final UserPositionService userPositionService;

	public UserPositionController(UserPositionService userPositionService) {
		this.userPositionService = userPositionService;
	}

	@Get("/positions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void getPositions(HttpServerRequest request) {
		final Optional<String> prefix = Optional.ofNullable(request.getParam("prefix"));
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			userPositionService.getUserPositions(prefix.orElse(""), adminInfos)
					.onSuccess(userPositions -> Renders.render(request, userPositions))
					.onFailure(th -> {
						Renders.log.warn("An error occurred while fetching user positions", th);
						Renders.renderError(request);
					});
		});

	}

	@Get("/positions/:positionId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void getPosition(HttpServerRequest request) {
		final String positionId = request.getParam("positionId");
		userPositionService.getUserPosition(positionId)
				.onSuccess(userPosition -> Renders.render(request, userPosition))
				.onFailure(th -> {
					Renders.log.warn("Could not find user position with id: " + positionId, th);
					Renders.notFound(request);
				});
	}

	@Post("/positions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void createPosition(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			RequestUtils.bodyToJson(request, jsonBody -> {
				final String positionName = jsonBody.getString("name");
				final String structureId = jsonBody.getString("structureId");
				final String userId = jsonBody.getString("userId");
				userPositionService.createUserPosition(positionName, structureId, userId, UserPositionSource.MANUAL, adminInfos)
						.onSuccess(userPosition -> Renders.render(request, userPosition, 201))
						.onFailure(th -> {
							Renders.log.warn("An error occurred while creating position : " + positionName + " for user " + userId + " in structure " + structureId, th);
							Renders.renderError(request);
						});
			});
		});
	}

	@Put("/positions/:positionId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void updatePosition(HttpServerRequest request) {
		final String positionId = request.getParam("positionId");
		RequestUtils.bodyToJson(request, jsonBody -> {
			final String name = jsonBody.getString("name");
			userPositionService.renameUserPosition(name, positionId)
					.onSuccess(userPosition -> Renders.render(request, userPosition))
					.onFailure(th -> {
						Renders.log.warn("An error occurred while renaming user position with id : " + positionId + " and name : " + name, th);
						Renders.renderError(request);
					});
		});
	}

	@Delete("/positions/:positionId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void deletePosition(HttpServerRequest request) {
		final String positionId = request.getParam("positionId");
		userPositionService.deleteUserPosition(positionId)
				.onSuccess(event -> Renders.ok(request))
				.onFailure(th -> {
					Renders.log.warn("An error occurred while fetching user position with id : " + positionId, th);
					Renders.renderError(request);
				});
	}

}
