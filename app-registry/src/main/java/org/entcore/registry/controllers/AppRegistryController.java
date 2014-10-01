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

package org.entcore.registry.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.registry.filters.ApplicationFilter;
import org.entcore.registry.filters.LinkRoleGroupFilter;
import org.entcore.registry.filters.RoleFilter;
import org.entcore.registry.services.AppRegistryService;
import org.entcore.registry.services.impl.DefaultAppRegistryService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.appregistry.AppRegistryEvents.APP_REGISTRY_PUBLISH_ADDRESS;
import static org.entcore.common.appregistry.AppRegistryEvents.PROFILE_GROUP_ACTIONS_UPDATED;
import static org.entcore.common.bus.BusResponseHandler.busArrayHandler;
import static org.entcore.common.bus.BusResponseHandler.busResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class AppRegistryController extends BaseController {

	private final AppRegistryService appRegistryService = new DefaultAppRegistryService();

	@Get("/admin-console")
	@SecuredAction("app-registry.adminConsole")
	public void staticAdmin(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/app-preview")
	@SecuredAction("app-registry.adminConsole")
	public void appPreview(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/applications")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listApplications(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		appRegistryService.listApplications(structureId, arrayResponseHandler(request));
	}

	@Get("/application/:name")
	@SecuredAction("app-registry.list.actions")
	public void listApplicationActions(HttpServerRequest request) {
		String name = request.params().get("name");
		if (name != null && !name.trim().isEmpty()) {
			appRegistryService.listActions(name, arrayResponseHandler(request));
		} else {
			badRequest(request, "invalid.application.name");
		}
	}

	@Get("/applications/actions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listApplicationsWithActions(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		String actionType = request.params().get("actionType");
		appRegistryService.listApplicationsWithActions(structureId, actionType, arrayResponseHandler(request));
	}

	@Post("/role")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createRole(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String roleName = body.getString("role");
				final JsonArray actions = body.getArray("actions");
				if (actions != null && roleName != null &&
						actions.size() > 0 && !roleName.trim().isEmpty()) {
					final JsonObject role = new JsonObject().putString("name", roleName);
					String structureId = request.params().get("structureId");
					appRegistryService.createRole(structureId, role, actions, notEmptyResponseHandler(request, 201, 409));
				} else {
					badRequest(request, "invalid.parameters");
				}
			}
		});
	}

	@Put("/role/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(RoleFilter.class)
	public void updateRole(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String roleId = request.params().get("id");
				if (roleId != null && !roleId.trim().isEmpty()) {
					final String roleName = body.getString("role");
					final JsonArray actions = body.getArray("actions", new JsonArray());
					final JsonObject role = new JsonObject();
					if (roleName != null && !roleName.trim().isEmpty()) {
						role.putString("name", roleName);
					}
					appRegistryService.updateRole(roleId, role, actions, notEmptyResponseHandler(request));
				} else {
					badRequest(request, "invalid.id");
				}
			}
		});
	}

	@Delete("/role/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(RoleFilter.class)
	public void deleteRole(final HttpServerRequest request) {
		String roleId = request.params().get("id");
		if (roleId != null && !roleId.trim().isEmpty()) {
			appRegistryService.deleteRole(roleId, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.id");
		}
	}

	@Post("/authorize/group")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(LinkRoleGroupFilter.class)
	public void linkGroup(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray roleIds = body.getArray("roleIds");
				final String groupId = body.getString("groupId");
				if (roleIds != null && groupId != null && !groupId.trim().isEmpty()) {
					appRegistryService.linkRolesToGroup(groupId, roleIds, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								updatedProfileGroupActions(groupId);
								renderJson(request, event.right().getValue());
							} else {
								leftToResponse(request, event.left());
							}
						}
					});
				} else {
					badRequest(request, "invalid.parameters");
				}
			}
		});
	}

	@Get("/roles")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listRoles(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		appRegistryService.listRoles(structureId, arrayResponseHandler(request));
	}

	@Get("/roles/actions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listRolesWithActions(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		appRegistryService.listRolesWithActions(structureId, arrayResponseHandler(request));
	}

	@Get("/groups/roles")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listGroupsWithRoles(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		appRegistryService.listGroupsWithRoles(structureId, true, arrayResponseHandler(request));
	}

	@Get("/application/conf/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void application(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			appRegistryService.getApplication(id, notEmptyResponseHandler(request));
		} else {
			badRequest(request, "invalid.application.id");
		}
	}

	@Put("/application/conf/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void applicationConf(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "updateApplication", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String applicationId = request.params().get("id");
				if (applicationId != null && !applicationId.trim().isEmpty()) {
					appRegistryService.updateApplication(applicationId, body, notEmptyResponseHandler(request));
				} else {
					badRequest(request, "invalid.application.id");
				}
			}
		});
	}

	@Delete("/application/conf/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void deleteApplication(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			appRegistryService.deleteApplication(id, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.application.id");
		}
	}

	@Post("/application/external")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createExternalApp(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createApplication", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String structureId = request.params().get("structureId");
				appRegistryService.createApplication(structureId, body, null, notEmptyResponseHandler(request, 201, 409));
			}
		});
	}

	@BusAddress("wse.app.registry")
	public void collectApps(final Message<JsonObject> message) {
		final JsonObject app = message.body().getObject("application");
		final String application = app.getString("name");
		final JsonArray securedActions = message.body().getArray("actions");
		if (application != null && securedActions != null && !application.trim().isEmpty()) {
			appRegistryService.createApplication(null, app, securedActions, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> event) {
					JsonObject j = new JsonObject();
					if (event.isRight()) {
						j.putString("status", "ok");
					} else {
						j.putString("status", "error").putString("message", event.left().getValue());
					}
					message.reply(j);
				}
			});
		} else {
			message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.parameters"));
		}
	}

	@Put("/application")
	public void recordApplication(final HttpServerRequest request) {
		if (("localhost:"+ container.config().getInteger("port", 8012))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject jo) {
					eb.send(container.config().getString("address"), jo, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> reply) {
							renderJson(request, reply.body());
						}
					});
				}
			});
		} else {
			forbidden(request, "invalid.host");
		}
	}

	@BusAddress("wse.app.registry.applications")
	public void applications(final Message<JsonObject> message) {
		String application = message.body().getString("application");
		if (application != null && !application.trim().isEmpty()) {
			String action = message.body().getString("action", "");
			Handler<Either<String, JsonArray>> responseHandler = new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> res) {
					if (res.isRight()) {
						message.reply(res.right().getValue());
					} else {
						message.reply(new JsonArray());
					}
				}
			};
			switch (action) {
				case "allowedUsers":
					appRegistryService.applicationAllowedUsers(application,
							message.body().getArray("users"),
							message.body().getArray("groups"),
							responseHandler);
					break;
				case "allowedProfileGroups":
					appRegistryService.applicationAllowedProfileGroups(application, responseHandler);
					break;
				default:
					message.reply(new JsonArray());
					break;
			}
		} else {
			message.reply(new JsonArray());
		}
	}

	@BusAddress("wse.app.registry.bus")
	public void registryEventBusHandler(final Message<JsonObject> message) {
		final String structureId = message.body().getString("structureId");
		switch (message.body().getString("action", "")) {
			case "setDefaultClassRoles" :
				appRegistryService.setDefaultClassRoles(message.body().getString("classId"),
						new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> r) {
						if (r.isRight()) {
							message.reply(r.right().getValue());
						} else {
							message.reply(new JsonObject().putString("status", "error")
									.putString("message", "invalid.classId"));
						}
					}
				});
				break;
			case "create-external-application" :
				appRegistryService.createApplication(structureId,
						message.body().getObject("application"), null, busResponseHandler(message));
				break;
			case "create-role" :
				final JsonObject role = message.body().getObject("role");
				final JsonArray actions = message.body().getArray("actions");
				appRegistryService.createRole(structureId, role, actions, busResponseHandler(message));
				break;
			case "link-role-group" :
				final String groupId = message.body().getString("groupId");
				final JsonArray roleIds = message.body().getArray("roleIds");
				appRegistryService.linkRolesToGroup(groupId, roleIds, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							updatedProfileGroupActions(groupId);
							message.reply(new JsonObject().putString("status", "ok")
									.putObject("result", event.right().getValue()));
						} else {
							JsonObject error = new JsonObject()
									.putString("status", "error")
									.putString("message", event.left().getValue());
							message.reply(error);
						}
					}
				});
				break;
			case "list-groups-with-roles" :
				boolean classGroups = message.body().getBoolean("classGroups", false);
				appRegistryService.listGroupsWithRoles(structureId, classGroups, busArrayHandler(message));
				break;
			case "list-roles" :
				appRegistryService.listRoles(structureId, busArrayHandler(message));
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	private void updatedProfileGroupActions(String groupId) {
		JsonObject message = new JsonObject().putString("type", PROFILE_GROUP_ACTIONS_UPDATED);
		if (groupId != null && !groupId.trim().isEmpty()) {
			message.putArray("groups", new JsonArray().add(groupId));
		}
		eb.publish(APP_REGISTRY_PUBLISH_ADDRESS, message);
	}

}
