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
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.position.UserPositionSource;
import org.entcore.common.user.position.UserPositionService;

public class UserPositionController extends BaseController {

	private final UserPositionService userPositionService;

	public UserPositionController(UserPositionService userPositionService) {
		this.userPositionService = userPositionService;
	}

	@Get("/positions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void getPositions(HttpServerRequest request) {
		final String content = request.getParam("content");
		final String structureId = request.getParam("structureId");
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			userPositionService.getUserPositions(content, structureId, adminInfos)
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
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			final String positionId = request.getParam("positionId");
			userPositionService.getUserPosition(positionId, adminInfos)
					.onSuccess(userPosition -> Renders.render(request, userPosition))
					.onFailure(th -> {
						Renders.log.warn("Could not find user position with id: " + positionId, th);
						Renders.notFound(request);
					});
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
				userPositionService.createUserPosition(positionName, structureId, UserPositionSource.MANUAL, adminInfos)
						.onSuccess(userPosition -> Renders.render(request, userPosition, 201))
						.onFailure(th -> {
							final String message;
							final JsonObject body = new JsonObject();
							final int code;
							final String errorMessage = th.getMessage();
							if(errorMessage.startsWith("position.already.exists:")) {
								code = 409;
								message = th.getMessage();
								body.put("existingPositionId", errorMessage.split(":")[1]);
							} else if(errorMessage.equals("cannot.create.position.on.this.structure")) {
								code = 401;
								message = "Missing the rights to create a position on this structure";
							} else {
								code = 500;
								message = "unknown.error";
							}
							Renders.log.warn("An error occurred while creating position : " + positionName + " in structure " + structureId, th);
							Renders.renderError(request, body, code, message);
						});
			});
		});
	}

	@Put("/positions/:positionId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void updatePosition(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			final String positionId = request.getParam("positionId");
			RequestUtils.bodyToJson(request, jsonBody -> {
				final String name = jsonBody.getString("name");
				userPositionService.renameUserPosition(name, positionId, adminInfos)
						.onSuccess(userPosition -> Renders.render(request, userPosition))
						.onFailure(th -> {
							log.warn("An error occurred while renaming user position with id : " + positionId + " and name : " + name, th);
							final String message;
							final int code;
							if("position.not.accessible".equals(th.getMessage())) {
								message = th.getMessage();
								code = 403;
							} else if("position.name.already.used".equals(th.getMessage())) {
								message = th.getMessage();
								code = 409;
							} else {
								message = "unknown.error";
								code = 500;
							}
							Renders.renderError(request, new JsonObject(), code, message);
						});
			});
		});

	}

	@Delete("/positions/:positionId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void deletePosition(HttpServerRequest request) {
		UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(adminInfos -> {
			final String positionId = request.getParam("positionId");
			userPositionService.deleteUserPosition(positionId, adminInfos)
					.onSuccess(event -> Renders.ok(request))
					.onFailure(th -> {
						Renders.log.warn("An error occurred while fetching user position with id : " + positionId, th);
						Renders.renderError(request);
					});
		});
	}

}
