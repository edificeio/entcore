package org.entcore.communication.controllers;

import java.util.*;

import org.entcore.common.neo4j.StatementsBuilder;
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

import fr.wseduc.webutils.collections.Joiner;

import org.entcore.communication.profils.GroupProfil;
import org.entcore.communication.profils.ProfilFactory;
import fr.wseduc.webutils.Controller;
import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.Server;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.SecuredAction;

public class CommunicationController extends Controller {

	private final Neo neo;
	private final ProfilFactory pf;
	private static final List<String> profils = Collections.unmodifiableList(
			Arrays.asList("SuperAdmin", "Student", "Relative", "Principal", "Teacher"));

	public CommunicationController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
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
						"MATCH (n:ProfileGroup) " +
						"WHERE n.id IN ['" + Joiner.on("','").join(gpId) + "'] " +
						"RETURN n.id as id, n.name as name, " +
						"HEAD(filter(x IN labels(n) WHERE x <> 'Visible' AND x <> 'ProfileGroup' " +
						"AND x <> 'ClassProfileGroup' AND x <> 'SchoolProfileGroup')) as type",
						 new Handler<Message<JsonObject>>() {

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
					if ("SuperAdmin".equals(user.getType())) {
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
					if ("SuperAdmin".equals(user.getType())) {
						eb.send("directory", new JsonObject().putString("action", "groups")
								.putArray("types", new JsonArray().addString("ClassStudentGroup"))
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
						visibleUsers(user.getUserId(), schoolId, new JsonArray().add("ClassStudentGroup"),
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
		String query;
		Map<String, Object> params = new HashMap<>();
		if (!"SuperAdmin".equals(userType)) {
			query = "MATCH (m:User)-[:COMMUNIQUE]->g-[:DEPENDS*1..2]->(n:School) " +
					"WHERE m.id = {userId} ";
			params.put("userId", userId);
		} else {
			query = "MATCH (n:School) ";
		}
		query += "RETURN n.id as id, n.name as name, HEAD(labels(n)) as type";
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
		gpc.addString("ClassProfileGroup");
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
				boolean itSelf = message.body().getBoolean("itself", false);
				boolean myGroup = message.body().getBoolean("mygroup", false);
				Map<String, Object> additionnalParams = null;
				if (ap != null) {
					additionnalParams = ap.toMap();
				}
				visibleUsers(userId, schoolId, expectedTypes, itSelf, myGroup, customReturn,
						additionnalParams, responseHandler);
				break;
			case "usersCanSeeMe":
				usersCanSeeMe(userId, responseHandler);
				break;
			case "visibleProfilsGroups":
				visibleProfilsGroups(userId, responseHandler);
				break;
			case "usersInProfilGroup":
				boolean itSelf2 = message.body().getBoolean("itself", false);
				String excludeUserId = message.body().getString("excludeUserId");
				usersInProfilGroup(userId, itSelf2, excludeUserId, responseHandler);
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
		visibleUsers(userId, schoolId, expectedTypes, false, null, null, handler);
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes, boolean itSelf,
			String customReturn, Map<String, Object> additionnalParams, final Handler<JsonArray> handler) {
		visibleUsers(userId, schoolId, expectedTypes, itSelf, false, customReturn, additionnalParams, handler);
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
			String customReturn, Map<String, Object> additionnalParams, final Handler<JsonArray> handler) {
		StringBuilder query = new StringBuilder();
		Map<String, Object> params = new HashMap<>();
		String condition = itSelf ? "" : "AND m.id <> {userId} ";
		if (schoolId != null && !schoolId.trim().isEmpty()) {
			query.append("MATCH (n:User)-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->(s:School {id:{schoolId}})"); //TODO manage leaf
			params.put("schoolId", schoolId);
		} else {
			String l = (myGroup) ? "" : " AND length(p) >= 2";
			query.append(" MATCH p=(n:User)-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]->t-[:COMMUNIQUE*0..1]->ipg" +
					"-[:COMMUNIQUE*0..1]->g<-[:DEPENDS*0..1]-m ");
			condition += "AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
					"XOR (type(r) = 'COMMUNIQUE'"+ l +
					" AND (length(p) < 3 OR (ipg:ProfileGroup AND length(p) = 3)))) ";
		}
		query.append("WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ").append(condition);
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND (");
			StringBuilder types = new StringBuilder();
			for (Object o: expectedTypes) {
				if (!(o instanceof String)) continue;
				String t = (String) o;
				types.append(" OR m:").append(t);
			}
			query.append(types.substring(4)).append(") ");
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH DISTINCT m as visibles ");
			query.append(customReturn);
		} else {
			query.append("RETURN distinct m.id as id, m.name as name, "
				+ "m.login as login, m.displayName as username, "
				+ "HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User' AND x <> 'ProfileGroup' "
				+ "AND x <> 'ClassProfileGroup' AND x <> 'SchoolProfileGroup')) as type, "
				+ "m.lastName as lastName, m.firstName as firstName "
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
				"MATCH p=(n:User)<-[:COMMUNIQUE*0..2]-t<-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]-(m:User) " +
				"WHERE n.id = {userId} AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
				"XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) AND m.id <> {userId} " +
				"RETURN distinct m.id as id, m.login as login, " +
				"m.displayName as username, HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User')) as type " +
				"ORDER BY username ";
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		neo.send(query, params, new Handler<Message<JsonObject>>() {

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
				"MATCH (n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(gp:ProfileGroup) " +
				"WHERE n.id = {userId} " +
				"RETURN distinct gp.id as id, gp.name as name, " +
				"HEAD(filter(x IN labels(gp) WHERE x <> 'Visible' AND x <> 'ProfileGroup' " +
				"AND x <> 'ClassProfileGroup' AND x <> 'SchoolProfileGroup')) as type " +
				"ORDER BY type DESC, name ";
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		neo.send(query, params, new Handler<Message<JsonObject>>() {

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

	private void usersInProfilGroup(String profilGroupId, boolean itSelf, String userId,
			final Handler<JsonArray> handler) {
		String condition = (itSelf || userId == null) ? "" : "AND u.id <> {userId} ";
		String query =
				"MATCH (n:ProfileGroup)<-[:APPARTIENT]-(u:User) " +
				"WHERE n.id = {groupId} " + condition +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, HEAD(filter(x IN labels(u) WHERE x <> 'Visible' AND x <> 'User')) as type " +
				"ORDER BY username ";
		Map<String, Object> params = new HashMap<>();
		params.put("groupId", profilGroupId);
		if (!itSelf && userId != null) {
			params.put("userId", userId);
		}
		neo.send(query, params, new Handler<Message<JsonObject>>() {
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

	@SecuredAction("communication.default.rules")
	public void defaultCommunicationRules(final HttpServerRequest request) {
		String schoolId = request.params().get("schoolId");
		if (schoolId == null || schoolId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		setDefaultCommunicationRules(schoolId, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				if ("ok".equals(r.body().getString("status"))) {
					renderJson(request, r.body());
				} else {
					renderError(request, r.body());
				}
			}
		});
	}

	private void setDefaultCommunicationRules(
			String schoolId, Handler<Message<JsonObject>> handler) {
		StatementsBuilder b = new StatementsBuilder();
		final JsonObject params = new JsonObject().putString("schoolId", schoolId);
		JsonArray r = container.config().getArray("defaultCommunicationRules");
		if (r == null || r.size() == 0 || schoolId == null || schoolId.trim().isEmpty()) {
			handler.handle(null);
			return;
		}
		for (Object o: r) {
			if (!(o instanceof String) || ((String) o).trim().isEmpty()) continue;
			if (((String) o).contains("EN_RELATION_AVEC")) {
				b.add("MATCH " + o + " CREATE UNIQUE start-[:COMMUNIQUE_DIRECT]->end", params);
					//"(s:School)<-[:APPARTIENT*2]-(n:Student)-[:EN_RELATION_AVEC]->(m:Relative)"
			} else {
				if (((String) o).contains("Class")) {
					b.add(
						"MATCH (s:School)<-[:APPARTIENT]-(c:Class), " + o + " " +
						//"(n:ClassStudentGroup)-[:DEPENDS]->c<-[:DEPENDS]-(m:ClassTeacherGroup)"
						"WHERE s.id = {schoolId} " +
						"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
					);
				} else {
					b.add(
						"MATCH (s:School), " + o + " " +
						 //"(n:SchoolTeacherGroup)-[:DEPENDS]->s<-[:DEPENDS]-(m:SchoolStudentGroup)"
						"WHERE s.id = {schoolId} " +
						"CREATE UNIQUE start-[:COMMUNIQUE]->end", params
					);
				}
			}
		}
		neo.executeTransaction(b.build(), null, true, handler);
	}

	public void communicationEventBusHandler(final Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "setDefaultCommunicationRules" :
				setDefaultCommunicationRules(
						message.body().getString("schoolId"), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						if (r != null) {
							message.reply(r.body());
						} else {
							message.reply(new JsonObject().putString("status", "error")
									.putString("message", "invalid.schoolId"));
						}
					}
				});
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	@SecuredAction("communication.remove.rules")
	public void removeCommunicationRules(HttpServerRequest request) {
		String schoolId = request.params().get("schoolId");
		if (schoolId == null || schoolId.trim().isEmpty()) {
			renderError(request);
			return;
		}
		String query =
				"MATCH (s:School)<-[:DEPENDS*1..2]-(g:ProfileGroup)-[r:COMMUNIQUE]-() " +
				"WHERE s.id = {schoolId} " +
				"OPTIONAl MATCH s<-[:APPARTIENT]-(c:Class)<-[:APPARTIENT]-(u:User)-[r1:COMMUNIQUE_DIRECT]-() " +
				"DELETE r, r1";
		neo.execute(query, new JsonObject().putString("schoolId", schoolId), request.response());
	}

}
