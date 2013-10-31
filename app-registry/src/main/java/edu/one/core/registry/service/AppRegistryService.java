package edu.one.core.registry.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import com.google.common.base.Joiner;

import edu.one.core.infra.Controller;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.Server;
import edu.one.core.infra.Utils;
import edu.one.core.common.user.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.SecuredAction;

public class AppRegistryService extends Controller {

	private final Neo neo;

	public AppRegistryService(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		neo = new Neo(Server.getEventBus(vertx), log);
	}

	@SecuredAction("app-registry.view")
	public void view(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				listSchools(user, new Handler<JsonArray>() {

					@Override
					public void handle(JsonArray event) {
						renderView(request, new JsonObject().putArray("schools", event));
					}
				});
			}
		});
	}

	@SecuredAction("app-registry.list.applications")
	public void listApplications(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","APPLICATION");
		neo.send(
			"START n=node:node_auto_index(type={type}) " +
			"RETURN n.id as id, n.name as name",
			params,
			request.response()
		);
	}

	@SecuredAction("app-registry.list.actions")
	public void listApplicationActions(HttpServerRequest request) {
		String name = request.params().get("name");
		if (name != null && !name.trim().isEmpty()) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("name", name);
			neo.send(
					"START n=node:node_auto_index(name={name}) " +
					"MATCH n-[:PROVIDE]->a " +
					"RETURN a.name as name, a.displayName as displayName, a.type as type",
				params,
				request.response()
			);
		} else {
			request.response().setStatusCode(400).end();
		}
	}

	@SecuredAction("app-registry.list.applications.actions")
	public void listApplicationsWithActions(HttpServerRequest request) {
		String actionType = request.params().get("actionType");
		String query =
				"START n=node:node_auto_index(type={type}) " +
				"MATCH n-[r?:PROVIDE]->a ";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","APPLICATION");
		if (actionType != null &&
				("WORKFLOW".equals(actionType) || "RESOURCE".equals(actionType))) {
			query += "WHERE r IS NULL OR a.type = {actionType} ";
			params.put("actionType", "SECURED_ACTION_" + actionType);
		}
		query += "RETURN n.id as id, n.name as name, "
				+ "COLLECT([a.name, a.displayName, a.type]) as actions";
		neo.send(query, params, request.response());
	}

	@SecuredAction("app-registry.create.role")
	public void createRole(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String actions = request.formAttributes().get("actions");
				final String roleName = request.formAttributes().get("role");
				if (actions != null && roleName != null &&
						!actions.trim().isEmpty() && !roleName.trim().isEmpty()) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("roleName", roleName);
					params.put("id", UUID.randomUUID().toString());
					neo.send(
							"START n=node:node_auto_index(name={roleName}) "+
							"WITH count(*) AS exists " +
							"WHERE exists=0 " +
							"CREATE (m {id:{id}, " +
							"type:'ROLE', " +
							"name:{roleName}" +
							"}) " +
							"RETURN m.id as id",
							params,
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if ("ok".equals(event.body().getString("status"))) {
										Map<String, Object> params2 = new HashMap<String, Object>();
										params2.put("roleName", roleName);
										neo.send(
											"START n=node:node_auto_index('name:(\"" +
											actions.replaceAll(",", "\" \"") + "\")'), " +
											"m=node:node_auto_index(name={roleName}) " +
											"CREATE UNIQUE m-[:AUTHORIZE]->n",
											params2, request.response()
										);
									} else {
										badRequest(request);
									}
								}
							}
						);
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction("app-registry.link.Group")
	public void linkGroup(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				List <String> roleIds = request.formAttributes().getAll("roleIds");
				String groupId = request.formAttributes().get("groupId");
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("groupId", groupId);
				if (roleIds != null && groupId != null && !groupId.trim().isEmpty()) {
					String deleteQuery =
							"START m=node:node_auto_index(id={groupId}) "
							+ "MATCH m-[r:AUTHORIZED]-() "
							+ "DELETE r";
					if (roleIds.isEmpty()) {
						neo.send(deleteQuery, params, request.response());
					} else {
						JsonArray queries = new JsonArray();
						queries.addObject(new JsonObject()
							.putString("query", deleteQuery)
							.putObject("params", new JsonObject(params)));
						String createQuery =
								"START n=node:node_auto_index('id:(" + Joiner.on(' ').join(roleIds) + ")'), "
								+ "m=node:node_auto_index(id={groupId}) "
								+ "CREATE UNIQUE m-[ra:AUTHORIZED]->n";
						queries.addObject(new JsonObject()
						.putString("query", createQuery)
						.putObject("params", new JsonObject(params)));
						neo.sendBatch(queries, request.response());
					}
					// TODO refactor with following commented code
//					String query;
//					if (roleIds.isEmpty()) {
//						query = "START m=node:node_auto_index(id={groupId}) "
//								+ "MATCH m-[r:AUTHORIZED]-() "
//								+ "DELETE r";
//					} else {
//						query = "START n=node:node_auto_index('id:(" + Joiner.on(' ').join(roleIds) + ")'), "
//								+ "m=node:node_auto_index(id={groupId}) "
//								+ "MATCH m-[r:AUTHORIZED]-() "
//								+ "CREATE m-[:AUTHORIZED]->n "
//								+ "DELETE r ";
//					}
//					neo.send(query, params, request.response());
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction("app-registry.list.roles")
	public void listRoles(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","ROLE");
		neo.send(
			"START n=node:node_auto_index(type={type}) " +
			"RETURN n.id as id, n.name as name",
			params,
			request.response()
		);
	}

	@SecuredAction("app-registry.list.roles.actions")
	public void listRolesWithActions(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","ROLE");
		neo.send(
			"START n=node:node_auto_index(type={type}) " +
			"MATCH n-[r?:AUTHORIZE]->a " +
			"RETURN n.id as id, n.name as name, COLLECT([a.name, a.displayName, a.type]) as actions",
			params,
			request.response()
		);
	}

	@SecuredAction("app-registry.list.groups")
	public void listGroups(final HttpServerRequest request) {
		eb.send("directory", new JsonObject().putString("action", "groups"),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					renderJson(request, res.body());
				} else {
					renderError(request, res.body());
				}
			}
		});
	}

	@SecuredAction("app-registry.list.schools")
	public void listSchools(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					listSchools(user, new Handler<JsonArray>() {

						@Override
						public void handle(JsonArray event) {
							renderJson(request, event);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void listSchools(UserInfos user, final Handler<JsonArray> handler) {
		if (user != null) {
			eb.send("wse.communication.schools", new JsonObject()
			.putString("userId", user.getUserId())
			.putString("userType", user.getType()), new Handler<Message<JsonArray>>() {

				@Override
				public void handle(Message<JsonArray> event) {
					handler.handle(event.body());
				}
			});
		} else {
			handler.handle(new JsonArray());
		}
	}

	@SecuredAction("app-registry.list.groups.roles")
	public void listGroupsWithRoles(final HttpServerRequest request) {
		String schoolId = request.params().get("schoolId");
		String query = "START n=node:node_auto_index({type})";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","type:GROUP_*");
		if (schoolId != null && !schoolId.trim().isEmpty()) {
			params.put("schoolId", schoolId);
			query += ", m=node:node_auto_index(id={schoolId}) " +
					 "MATCH m<-[:DEPENDS*1..2]-n-[r?:AUTHORIZED]->a ";
		} else {
			query += " MATCH n-[r?:AUTHORIZED]->a ";
		}
		query += "RETURN distinct n.id as id, n.name as name, COLLECT(a.id) as roles";
		neo.send(query, params, request.response());
	}

	@SecuredAction("app-registry.application")
	public void application(HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("id", id);
			neo.send(
				"START n=node:node_auto_index(id={id}) " +
				"RETURN n.id as id, n.name as name, " +
				"n.grantType? as grantType, n.secret? as secret, n.address? as address, " +
				"n.icon? as icon, n.target? as target",
				params,
				request.response()
			);
		} else {
			badRequest(request);
		}
	}

	@SecuredAction("app-registry.application.conf")
	public void applicationConf(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				String applicationId = request.formAttributes().get("applicationId");
				String grantType = request.formAttributes().get("grantType");
				String secret = request.formAttributes().get("secret");
				String address = request.formAttributes().get("address");
				String icon = request.formAttributes().get("icon");
				String target = request.formAttributes().get("target");
				if (applicationId != null && !applicationId.trim().isEmpty()) {
					String query =
							"START n=node:node_auto_index(id={applicationId}) " +
							"SET n.grantType = {grantType}, n.secret = {secret}, " +
							"n.address = {address} , n.icon = {icon} , n.target = {target}";
					Map<String, Object> params = new HashMap<>();
					params.put("applicationId", applicationId);
					params.put("grantType", Utils.getOrElse(grantType, ""));
					params.put("secret", Utils.getOrElse(secret, ""));
					params.put("address", Utils.getOrElse(address, ""));
					params.put("icon", Utils.getOrElse(icon, ""));
					params.put("target", Utils.getOrElse(target, ""));
					neo.send(query, params, request.response());
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction("app-registry.create.external.app")
	public void createExternalApp(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				String name = request.formAttributes().get("name");
				String grantType = request.formAttributes().get("grantType");
				String secret = request.formAttributes().get("secret");
				if (name != null && !name.trim().isEmpty() &&
						grantType != null && !grantType.trim().isEmpty()) {
					String address = request.formAttributes().get("address");
					String icon = request.formAttributes().get("icon");
					String target = request.formAttributes().get("target");
					String query = "CREATE (c { id: {id}, type: 'APPLICATION', name: {name}, " +
							"grantType: {grantType}, address: {address} , icon: {icon} , target: {target} ";
					if (secret != null && !secret.trim().isEmpty()) {
						query += ", secret: {secret} })";
					} else {
						query += "})";
					}
					String appId = UUID.randomUUID().toString();
					JsonArray queries = new JsonArray();
					JsonObject params = new JsonObject();
					params.putString("id", appId);
					params.putString("name", name);
					params.putString("grantType", grantType);
					params.putString("secret", secret);
					params.putString("address", Utils.getOrElse(address, ""));
					params.putString("icon", Utils.getOrElse(icon, ""));
					params.putString("target", Utils.getOrElse(target, ""));
					queries.addObject(Neo.toJsonObject(query, params));
					if (address != null && !address.trim().isEmpty()) {
						String query2 =
								"START n=node:node_auto_index(id={id}) " +
								"CREATE UNIQUE n-[r:PROVIDE]->(a {type: {type}, " +
								"name:{name}, displayName:{displayName}}) " +
								"RETURN a.name as name";
						queries.addObject(Neo.toJsonObject(query2, new JsonObject()
						.putString("id", appId)
						.putString("type", "SECURED_ACTION_WORKFLOW")
						.putString("name", name + "|address")
						.putString("displayName", name + ".address")));
					}
					neo.sendBatch(queries, request.response());
				} else {
					badRequest(request);
				}
			}
		});
	}

	public void collectApps(final Message<JsonObject> message) {
		final JsonObject app = message.body().getObject("application");
		final String application = app.getString("name");
		final JsonArray securedActions = message.body().getArray("actions");
		if (application != null && securedActions != null && !application.trim().isEmpty()) {
			neo.send(
				"START n=node:node_auto_index(name='" + application + "') "+
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m {id:'" + UUID.randomUUID().toString() + "', " +
				"type:'APPLICATION', " +
				"address:'" + app.getString("address", "") + "', " +
				"icon:'" + app.getString("icon", "") + "', " +
				"name:'" + application +
				"'}) " +
				"RETURN m.id as id",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							JsonArray queries = new JsonArray();
							for (Object o: securedActions) {
								JsonObject json = (JsonObject) o;
								JsonObject q = new JsonObject();
								q.putString("query",
									"START n=node:node_auto_index(name={application}) " +
									"CREATE UNIQUE n-[r:PROVIDE]->(a {type: {type}, " +
									"name:{name}, displayName:{displayName}}) " +
									"RETURN a.name as name"
								);
								q.putObject("params", json
									.putString("application", application)
									.putString("type", "SECURED_ACTION_" +
											json.getString("type", "WORKFLOW")));
								queries.addObject(q);
							}
							neo.sendBatch(queries, new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> res) {
									log.info(res.body());
									message.reply(res.body());
								}
							});
						}
					}
				}
			);
		}
	}

}
