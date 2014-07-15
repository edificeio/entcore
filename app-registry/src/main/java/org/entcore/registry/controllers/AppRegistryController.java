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
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
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
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class AppRegistryController extends BaseController {

	private final AppRegistryService appRegistryService = new DefaultAppRegistryService();

	@Get("/admin")
	@SecuredAction("app-registry.view")
	public void view(final HttpServerRequest request) {
//		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
//
//			@Override
//			public void handle(UserInfos user) {
//				listStructures(user, new Handler<JsonArray>() {
//
//					@Override
//					public void handle(JsonArray event) {
						renderView(request, new JsonObject());//.putArray("schools", event));
//					}
//				});
//			}
//		});
	}

	@Get("/static-admin")
	@SecuredAction("app-registry.staticAdmin")
	public void staticAdmin(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/app-preview")
	@SecuredAction("app-registry.staticAdmin")
	public void appPreview(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/applications")
	@SecuredAction("app-registry.list.applications")
	public void listApplications(HttpServerRequest request) {
		appRegistryService.listApplications(arrayResponseHandler(request));
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
	@SecuredAction("app-registry.list.applications.actions")
	public void listApplicationsWithActions(HttpServerRequest request) {
		String actionType = request.params().get("actionType");
		appRegistryService.listApplicationsWithActions(actionType, arrayResponseHandler(request));
	}

	@Post("/role")
	@SecuredAction("app-registry.create.role")
	public void createRole(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String roleName = body.getString("role");
				final JsonArray actions = body.getArray("actions");
				if (actions != null && roleName != null &&
						actions.size() > 0 && !roleName.trim().isEmpty()) {
					final JsonObject role = new JsonObject().putString("name", roleName);
					appRegistryService.createRole(role, actions, notEmptyResponseHandler(request, 201, 409));
				} else {
					badRequest(request, "invalid.parameters");
				}
			}
		});
	}

	@Put("/role/:id")
	@SecuredAction("app-registry.update.role")
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
	@SecuredAction("app-registry.create.role")
	public void deleteRole(final HttpServerRequest request) {
		String roleId = request.params().get("id");
		if (roleId != null && !roleId.trim().isEmpty()) {
			appRegistryService.deleteRole(roleId, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.id");
		}
	}

	@Post("/authorize/group")
	@SecuredAction("app-registry.link.Group")
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
	@SecuredAction("app-registry.list.roles")
	public void listRoles(HttpServerRequest request) {
		appRegistryService.listRoles(arrayResponseHandler(request));
	}

	@Get("/roles/actions")
	@SecuredAction("app-registry.list.roles.actions")
	public void listRolesWithActions(HttpServerRequest request) {
		appRegistryService.listRolesWithActions(arrayResponseHandler(request));
	}

	@Get("/groups/roles")
	@SecuredAction("app-registry.list.groups.roles")
	public void listGroupsWithRoles(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		appRegistryService.listGroupsWithRoles(structureId, arrayResponseHandler(request));
	}

	@Get("/application/conf/:id")
	@SecuredAction("app-registry.application")
	public void application(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			appRegistryService.getApplication(id, notEmptyResponseHandler(request));
		} else {
			badRequest(request, "invalid.application.id");
		}
	}

	@Put("/application/conf/:id")
	@SecuredAction("app-registry.application.conf")
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
	@SecuredAction("app-registry.application")
	public void deleteApplication(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			appRegistryService.deleteApplication(id, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.application.id");
		}
	}

	@Post("/application/external")
	@SecuredAction("app-registry.create.external.app")
	public void createExternalApp(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createApplication", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				appRegistryService.createApplication(body, null, notEmptyResponseHandler(request, 201, 409));
			}
		});
	}

	@BusAddress("wse.app.registry")
	public void collectApps(final Message<JsonObject> message) {
		final JsonObject app = message.body().getObject("application");
		final String application = app.getString("name");
		final JsonArray securedActions = message.body().getArray("actions");
		if (application != null && securedActions != null && !application.trim().isEmpty()) {
			appRegistryService.createApplication(app, securedActions, new Handler<Either<String, JsonObject>>() {
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
