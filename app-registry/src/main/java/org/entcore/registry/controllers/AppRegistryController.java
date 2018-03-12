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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.BaseController;

import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.filters.ApplicationFilter;
import org.entcore.registry.filters.LinkRoleGroupFilter;
import org.entcore.registry.filters.RoleFilter;
import org.entcore.registry.filters.RoleGroupFilter;
import org.entcore.registry.filters.SuperAdminFilter;
import org.entcore.registry.services.AppRegistryService;
import org.entcore.registry.services.impl.DefaultAppRegistryService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.appregistry.AppRegistryEvents.APP_REGISTRY_PUBLISH_ADDRESS;
import static org.entcore.common.appregistry.AppRegistryEvents.PROFILE_GROUP_ACTIONS_UPDATED;
import static org.entcore.common.bus.BusResponseHandler.busArrayHandler;
import static org.entcore.common.bus.BusResponseHandler.busResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import java.net.URL;

public class AppRegistryController extends BaseController {

	private final AppRegistryService appRegistryService = new DefaultAppRegistryService();

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void staticAdmin(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/app-preview")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
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
	
	@Get("structure/:structureId/application/:appId/groups/roles")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	public void listApplicationRolesWithGroups(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		String appId = request.params().get("appId");
		appRegistryService.listApplicationRolesWithGroups(structureId, appId, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> r) {
				if (r.isRight()) {
					JsonArray list = r.right().getValue();
					for (Object res : list) {
						UserUtils.translateGroupsNames(((JsonObject)res).getJsonArray("groups"), I18n.acceptLanguage(request));
					}
					renderJson(request, list);
				} else {
					leftToResponse(request, r.left());
				}
			}
		});
	}

	@Post("/role")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createRole(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String roleName = body.getString("role");
				final JsonArray actions = body.getJsonArray("actions");
				if (actions != null && roleName != null &&
						actions.size() > 0 && !roleName.trim().isEmpty()) {
					final JsonObject role = new JsonObject().put("name", roleName);
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
					final JsonArray actions = body.getJsonArray("actions", new JsonArray());
					final JsonObject role = new JsonObject();
					if (roleName != null && !roleName.trim().isEmpty()) {
						role.put("name", roleName);
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
				final JsonArray roleIds = body.getJsonArray("roleIds");
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

	@Put("/authorize/group/:groupId/role/:roleId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(RoleGroupFilter.class)
	public void addGroupLink(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		final String roleId = request.params().get("roleId");
		appRegistryService.addGroupLink(groupId, roleId, defaultResponseHandler(request));
	}

	@Delete("/authorize/group/:groupId/role/:roleId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(RoleGroupFilter.class)
	public void removeGroupLink(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		final String roleId = request.params().get("roleId");
		appRegistryService.deleteGroupLink(groupId, roleId, defaultResponseHandler(request, 204));
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
		appRegistryService.listGroupsWithRoles(structureId, true, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> r) {
				if (r.isRight()) {
					JsonArray res = r.right().getValue();
					UserUtils.translateGroupsNames(res, I18n.acceptLanguage(request));
					renderJson(request, res);
				} else {
					leftToResponse(request, r.left());
				}
			}
		});
	}

	@Post("/application")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void createApplication(final HttpServerRequest request){
		bodyToJson(request, pathPrefix + "createApplication", new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				String structureId = request.params().get("structureId");
				final String casType = body.getString("casType", "");
				final String address = body.getString("address", "");
				final boolean updateCas = !StringUtils.isEmpty(casType);
				final URL addressURL = DefaultAppRegistryService.checkCasUrl(address);

				// don't check url for standard app or oauth connector
				if (!updateCas || addressURL != null) {
					appRegistryService.createApplication(structureId, body, null, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isLeft()) {
								JsonObject error = new JsonObject()
										.put("error", event.left().getValue());
								Renders.renderJson(request, error, 400);
								return;
							}

							if (event.right().getValue() != null && event.right().getValue().size() > 0) {
								sendPatternToCasConfiguration(updateCas, body, addressURL, casType);
								Renders.renderJson(request, event.right().getValue(), 201);
							} else {
								JsonObject error = new JsonObject()
										.put("error", "appregistry.failed.app");
								Renders.renderJson(request, error, 400);
							}
						}
					});
				} else {
					badRequest(request, "appregistry.failed.app.url");
				}
			}
		});
	}

	private void sendPatternToCasConfiguration(boolean updateCas, JsonObject body, URL addressURL, String casType) {
		if (updateCas && addressURL != null) {
            String pattern = body.getString("pattern", "");
            if (pattern.isEmpty()) {
                pattern = "^\\Q" + addressURL.getProtocol() + "://" + addressURL.getHost() + (addressURL.getPort() > 0 ? ":" + addressURL.getPort() : "") + "\\E.*";
            }
            Server.getEventBus(vertx).publish("cas.configuration", new JsonObject()
                    .put("action", "add-patterns")
                    .put("service", casType)
                    .put("patterns", new JsonArray().add(pattern)));
        }
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
			public void handle(final JsonObject body) {
				String applicationId = request.params().get("id");
				final String casType = body.getString("casType","");
				final String address = body.getString("address", "");
				final boolean updateCas = !StringUtils.isEmpty(casType);

				if (applicationId != null && !applicationId.trim().isEmpty()) {
					final URL addressURL = DefaultAppRegistryService.checkCasUrl(address);

					// don't check url for standard app or oauth connector
					if (!updateCas ||  addressURL != null) {
						appRegistryService.updateApplication(applicationId, body, new Handler<Either<String, JsonObject>>() {
							public void handle(Either<String, JsonObject> event) {
								if (event.isLeft()) {
									JsonObject error = new JsonObject()
											.put("error", event.left().getValue());
									Renders.renderJson(request, error, 400);
									return;
								}

								sendPatternToCasConfiguration(updateCas, body, addressURL, casType);
								Renders.renderJson(request, event.right().getValue());
							}
						});
					} else {
						badRequest(request, "appregistry.failed.app.url");
					}
				} else {
					badRequest(request, "appregistry.failed.app");
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

	@Get("/cas-types")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void listCasTypes(final HttpServerRequest request) {
		Server.getEventBus(vertx).send("cas.configuration", new JsonObject().put("action", "list-services"),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					renderJson(request, event.body().getJsonArray("result"));
				} else {
					log.error(event.body().getString("message"));
				}
			}
		}));
	}

	@BusAddress("wse.app.registry")
	public void collectApps(final Message<JsonObject> message) {
		final JsonObject app = message.body().getJsonObject("application");
		final String application = app.getString("name");
		final JsonArray securedActions = message.body().getJsonArray("actions");
		if (application != null && securedActions != null && !application.trim().isEmpty()) {
			appRegistryService.createApplication(null, app, securedActions, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> event) {
					JsonObject j = new JsonObject();
					if (event.isRight()) {
						j.put("status", "ok");
					} else {
						j.put("status", "error").put("message", event.left().getValue());
					}
					message.reply(j);
				}
			});
		} else {
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.parameters"));
		}
	}

	@Put("/application")
	public void recordApplication(final HttpServerRequest request) {
		if (("localhost:"+ config.getInteger("port", 8012))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject jo) {
					eb.send(config.getString("address", "wse.app.registry"), jo, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> reply) {
							renderJson(request, reply.body());
						}
					}));
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
							message.body().getJsonArray("users"),
							message.body().getJsonArray("groups"),
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
							message.reply(new JsonObject().put("status", "error")
									.put("message", "invalid.classId"));
						}
					}
				});
				break;
			case "create-external-application" :
				appRegistryService.createApplication(structureId,
						message.body().getJsonObject("application"), null, busResponseHandler(message));
				break;
			case "create-role" :
				final JsonObject role = message.body().getJsonObject("role");
				final JsonArray actions = message.body().getJsonArray("actions");
				appRegistryService.createRole(structureId, role, actions, busResponseHandler(message));
				break;
			case "link-role-group" :
				final String groupId = message.body().getString("groupId");
				final JsonArray roleIds = message.body().getJsonArray("roleIds");
				appRegistryService.linkRolesToGroup(groupId, roleIds, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							updatedProfileGroupActions(groupId);
							message.reply(new JsonObject().put("status", "ok")
									.put("result", event.right().getValue()));
						} else {
							JsonObject error = new JsonObject()
									.put("status", "error")
									.put("message", event.left().getValue());
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
			case "list-cas-connectors" :
				appRegistryService.listCasConnectors(busArrayHandler(message));
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	private void updatedProfileGroupActions(String groupId) {
		JsonObject message = new JsonObject().put("type", PROFILE_GROUP_ACTIONS_UPDATED);
		if (groupId != null && !groupId.trim().isEmpty()) {
			message.put("groups", new JsonArray().add(groupId));
		}
		eb.publish(APP_REGISTRY_PUBLISH_ADDRESS, message);
	}

}
