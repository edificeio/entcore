package edu.one.core.communication.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import com.google.common.base.Joiner;

import edu.one.core.communication.profils.GroupProfil;
import edu.one.core.communication.profils.ProfilFactory;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import edu.one.core.infra.Server;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.SecuredAction;

public class CommunicationController extends Controller {

	private final Neo neo;
	private final ProfilFactory pf;
	private static final List<String> profils = Collections.unmodifiableList(
			Arrays.asList("ADMINISTRATEUR", "ELEVE", "PERSRELELEVE", "PERSEDUCNAT", "ENSEIGNANT"));

	public CommunicationController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		neo = new Neo(Server.getEventBus(vertx), log);
		pf = new ProfilFactory();
	}

	@SecuredAction("communication.view")
	public void view(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					listVisiblesSchools(user.getUserId(), user.getType(), new Handler<JsonArray>() {

						@Override
						public void handle(JsonArray event) {
							renderView(request, new JsonObject().putArray("schools", event));
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("communication.conf.profils.matrix")
	public void setGroupsProfilsMatrix(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> groupsProfils = request.formAttributes().getAll("groupProfil");
				if (profils != null) {
					Set<String> gpId = new HashSet<>(Arrays.asList(Joiner.on("_").join(groupsProfils).split("_")));
					neo.send(
						"START n=node:node_auto_index('id:" + Joiner.on(" OR id:").join(gpId) + "') " +
						"RETURN n.id as id, n.name as name, n.type as type",
						null, new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
								HashMap<String, GroupProfil> gps = new HashMap<>();
								JsonObject result = res.body().getObject("result");
								if (result != null) {
									for (String attr : result.getFieldNames()) {
										JsonObject json = result.getObject(attr);
										try {
											gps.put(json.getString("id"), pf.getGroupProfil(json));
										} catch (IllegalArgumentException e) {
											log.error(e.getMessage(), e);
										}
									}
									JsonArray queries = new JsonArray();
									for (String gp : groupsProfils) {
										String[] groupsId = gp.split("_");
										if (groupsId.length != 2) continue;
										GroupProfil gp1 = gps.get(groupsId[0]);
										GroupProfil gp2 = gps.get(groupsId[1]);
										if (gp1 != null && gp2 != null) {
											JsonElement q = gp1.queryAddCommunicationLink(gp2);
											if (q != null) {
												if (q.isArray()) {
													for (Object o: q.asArray()) {
														queries.add(o);
													}
												} else {
													queries.add(q);
												}
											}
										}
									}
									neo.sendBatch(queries, request.response());
								} else {
									renderJson(request, res.body(), 404);
								}
							} else {
								renderError(request, res.body());
							}
						}
					});
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction("communication.conf.pe.com")
	public void setParentEnfantCommunication(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> groups = request.formAttributes().getAll("groupId");
				if (groups != null) {
					JsonArray queries = GroupProfil.queryParentEnfantCommunication(groups);
					neo.sendBatch(queries, request.response());
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction("communication.list.profils")
	public void listProfils(HttpServerRequest request) {
		renderJson(request, new JsonArray(profils.toArray()));
	}

	@SecuredAction("communication.list.group.profil")
	public void listVisiblesGroupsProfil(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String schoolId = request.params().get("schoolId");
					if ("SUPERADMIN".equals(user.getType())) {
						eb.send("directory", new JsonObject().putString("action", "groups")
								.putString("schoolId", schoolId),
								new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								if ("ok".equals(res.body().getString("status"))) {
									renderJson(request,
											resultToJsonArray(res.body().getObject("result")));
								} else {
									renderError(request, res.body());
								}
							}
						});
					} else {
						visibleUsers(user.getUserId(), schoolId, allGroupsProfilsClasse(),
								new Handler<JsonArray>() {

							@Override
							public void handle(JsonArray event) {
								renderJson(request, event);
							}
						});
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("communication.list.classes.student")
	public void listVisiblesClassesEnfants(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String schoolId = request.params().get("schoolId");
					if ("SUPERADMIN".equals(user.getType())) {
						eb.send("directory", new JsonObject().putString("action", "groups")
								.putArray("types", new JsonArray().addString("GROUP_CLASSE_ELEVE"))
								.putString("schoolId", schoolId),
								new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								if ("ok".equals(res.body().getString("status"))) {
									renderJson(request,
											resultToJsonArray(res.body().getObject("result")));
								} else {
									renderError(request, res.body());
								}
							}
						});
					} else {
						visibleUsers(user.getUserId(), schoolId, new JsonArray().add("GROUP_CLASSE_ELEVE"),
								new Handler<JsonArray>() {
							@Override
							public void handle(JsonArray event) {
								renderJson(request, event);
							}
						});
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("communication.list.schools")
	public void listVisiblesSchools(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					listVisiblesSchools(user.getUserId(), user.getType(), new Handler<JsonArray>() {

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

	public void listVisiblesSchools(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		String userType = message.body().getString("userType");
		if (userId != null && !userId.trim().isEmpty()) {
			listVisiblesSchools(userId, userType, new Handler<JsonArray>() {

				@Override
				public void handle(JsonArray event) {
					message.reply(event);
				}
			});
		} else {
			message.reply(new JsonArray());
		}
	}

	private void listVisiblesSchools(String userId, String userType,
			final Handler<JsonArray> handler) {
		String query = "START n=node:node_auto_index(type='ETABEDUCNAT') ";
		Map<String, Object> params = new HashMap<>();
		if (!"SUPERADMIN".equals(userType)) {
			query += ", m=node:node_auto_index(id={userId}) " +
					 "MATCH m-[:COMMUNIQUE]->g-[:DEPENDS*1..2]->n ";
			params.put("userId", userId);
		}
		query += "RETURN n.id as id, n.ENTStructureNomCourant as name, n.type as type";
		neo.send(query, params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> m) {
				if ("ok".equals(m.body().getString("status"))) {
					handler.handle(resultToJsonArray(m.body().getObject("result")));
				} else {
					handler.handle(new JsonArray());
				}
			}
		});
	}

	private JsonArray allGroupsProfilsClasse() {
		JsonArray gpc = new JsonArray();
		for (String profil : profils) {
			gpc.addString("GROUP_CLASSE_" + profil);
		}
		return gpc;
	}

	@SecuredAction("communication.visible.user")
	public void visibleUsers(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String schoolId = request.params().get("schoolId");
			List<String> expectedTypes = request.params().getAll("expectedType");
			visibleUsers(userId, schoolId, new JsonArray(expectedTypes.toArray()), new Handler<JsonArray>() {

				@Override
				public void handle(JsonArray res) {
					renderJson(request, res);
				}
			});
		} else {
			renderJson(request, new JsonArray());
		}
	}

	public void visibleUsers(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String action = message.body().getString("action", "");
			String schoolId = message.body().getString("schoolId");
			JsonArray expectedTypes = message.body().getArray("expectedTypes");
			Handler<JsonArray> responseHandler = new Handler<JsonArray>() {

				@Override
				public void handle(JsonArray res) {
					message.reply(res);
				}
			};
			switch (action) {
			case "visibleUsers":
				String customReturn = message.body().getString("customReturn");
				JsonObject ap = message.body().getObject("additionnalParams");
				Map<String, Object> additionnalParams = null;
				if (ap != null) {
					additionnalParams = ap.toMap();
				}
				visibleUsers(userId, schoolId, expectedTypes, customReturn,
						additionnalParams, responseHandler);
				break;
			case "usersCanSeeMe":
				usersCanSeeMe(userId, responseHandler);
				break;
			case "visibleProfilsGroups":
				visibleProfilsGroups(userId, responseHandler);
				break;
			case "usersInProfilGroup":
				usersInProfilGroup(userId, responseHandler);
				break;
			default:
				message.reply(new JsonArray());
				break;
			}
		} else {
			message.reply(new JsonArray());
		}
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes,
			final Handler<JsonArray> handler) {
		visibleUsers(userId, schoolId, expectedTypes, null, null, handler);
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes,
			String customReturn, Map<String, Object> additionnalParams, final Handler<JsonArray> handler) {
		StringBuilder query = new StringBuilder()
			.append("START n = node:node_auto_index(id={userId})");
		Map<String, Object> params = new HashMap<>();
		if (schoolId != null && !schoolId.trim().isEmpty()) {
			query.append(", s=node:node_auto_index(id={schoolId}) "
					+ "MATCH n-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->s "); //TODO manage leaf
			params.put("schoolId", schoolId);
		} else {
			query.append(" MATCH n-[:COMMUNIQUE*1..2]->l-[?:COMMUNIQUE]->m<-[?:COMMUNIQUE_DIRECT]-n ");
		}
		query.append("WHERE has(m.id) AND m.id <> {userId} ");
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND has(m.type) AND m.type IN ")
			.append(expectedTypes.encode().replaceAll("\"", "'"))
			.append(" ");
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH m as visibles ");
			query.append(customReturn);
		} else {
			query.append("RETURN distinct m.id as id, m.name? as name, "
				+ "m.ENTPersonLogin? as login, m.ENTPersonNomAffichage? as username, m.type as type, "
				+ "m.ENTPersonNom? as lastName, m.ENTPersonPrenom? as firstName "
				+ "ORDER BY name, username ");
		}
		params.put("userId", userId);
		if (additionnalParams != null) {
			params.putAll(additionnalParams);
		}
		neo.send(query.toString(), params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = new JsonArray();
				if ("ok".equals(res.body().getString("status"))) {
					r = resultToJsonArray(res.body().getObject("result"));
				}
				handler.handle(r);
			}
		});
	}

	private void usersCanSeeMe(String userId, final Handler<JsonArray> handler) {
		String query =
				"START n=node:node_auto_index(id={userId}) " +
				"MATCH n<-[:COMMUNIQUE*1..2]-l<-[?:COMMUNIQUE]-m-[?:COMMUNIQUE_DIRECT]->n " +
				"WHERE has(m.id) AND m.id <> {userId} AND has(m.ENTPersonLogin) " +
				"RETURN distinct m.id as id, m.ENTPersonLogin as login, " +
				"m.ENTPersonNomAffichage? as username, m.type as type " +
				"ORDER BY username ";
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		neo.send(query.toString(), params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = new JsonArray();
				if ("ok".equals(res.body().getString("status"))) {
					r = resultToJsonArray(res.body().getObject("result"));
				}
				handler.handle(r);
			}
		});
	}

	private void visibleProfilsGroups(String userId, final Handler<JsonArray> handler) {
		String query =
				"START n = node:node_auto_index(id={userId}) " +
				"MATCH n-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-gp " +
				"WHERE has(gp.type) AND gp.type IN ['GROUP_CLASSE_PERSRELELEVE','GROUP_CLASSE_ELEVE'," +
						"'GROUP_CLASSE_PERSEDUCNAT','GROUP_CLASSE_ENSEIGNANT','GROUP_ETABEDUCNAT_PERSRELELEVE'," +
						"'GROUP_ETABEDUCNAT_ELEVE','GROUP_ETABEDUCNAT_PERSEDUCNAT','GROUP_ETABEDUCNAT_ENSEIGNANT'," +
						"'GROUP_ETABEDUCNAT_DIRECTEUR'] " +
				"RETURN distinct gp.id as id, gp.name as name, gp.type as type " +
				"ORDER BY type DESC, name ";
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		neo.send(query.toString(), params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = new JsonArray();
				if ("ok".equals(res.body().getString("status"))) {
					r = resultToJsonArray(res.body().getObject("result"));
				}
				handler.handle(r);
			}
		});
	}

	private void usersInProfilGroup(String profilGroupId, final Handler<JsonArray> handler) {
		String query =
				"START n = node:node_auto_index({group}) " +
				"MATCH n<-[:APPARTIENT]-u " +
				"WHERE has(u.type) AND u.type IN ['PERSRELELEVE','ELEVE','PERSEDUCNAT','ENSEIGNANT'] " +
				"RETURN distinct u.id as id, u.ENTPersonLogin? as login," +
						" u.ENTPersonNomAffichage? as username, u.type as type " +
				"ORDER BY username ";
		Map<String, Object> params = new HashMap<>();
		params.put("group", "id:" + profilGroupId + " AND type:GROUP_*");
		neo.send(query.toString(), params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = new JsonArray();
				if ("ok".equals(res.body().getString("status"))) {
					r = resultToJsonArray(res.body().getObject("result"));
				}
				handler.handle(r);
			}
		});
	}

	private JsonArray resultToJsonArray(JsonObject j) {
		JsonArray r = new JsonArray();
		for (String idx : j.getFieldNames()) {
			r.addObject(j.getObject(idx));
		}
		return r;
	}
}
