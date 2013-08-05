package edu.one.core.communication.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.communication.profils.ProfilFactory;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import edu.one.core.infra.security.SecuredAction;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;

public class CommunicationController extends Controller {

	private final Neo neo;
	private final ProfilFactory pf;
	private static final List<String> profils = Collections.unmodifiableList(
			Arrays.asList("ADMINISTRATEUR", "DIRECTEUR", "PERSONNEL_EN", "ENSEIGNANT",
					"AGENT_CT", "ELEVE", "PARENT"));

	public CommunicationController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		neo = new Neo(vertx.eventBus(), log);
		pf = new ProfilFactory();
	}

	public void view(HttpServerRequest request) {
		renderView(request);
	}

	public void setGroupProfilsCommunication(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> profils = request.formAttributes().getAll("profil");
				String groupId = request.formAttributes().get("groupId");
				if (groupId != null && !groupId.trim().isEmpty() && profils != null) {
					Map<String, Object> params = new HashMap<>();
					params.put("id", groupId);
					neo.send(
						"START n=node:node_auto_index(id={id}) " +
						"RETURN n.id as id, n.name as name",
						params, new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
								JsonObject group = res.body().getObject("result").getObject("0");
								if (group != null) {
									JsonArray queries = new JsonArray();
									for (String profil : profils) {
										try {
											queries.addObject(pf.getProfil(profil)
													.queryAddComGroupProfil(group));
										} catch (IllegalArgumentException e) {
											log.error(e.getMessage(), e);
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
//					neo.send(
//						"START n=node:node_auto_index(type='GROUPE')" +
//						"MATCH n<-[:APPARTIENT]-m " +
//						"WHERE n.ENTGroupeNom = {profil} " +
//						"CREATE UNIQUE m-[:COMMUNIQUE]-n"
//						, params, request.response());
				} else {
					badRequest(request);
				}
			}
		});
	}

	public void listProfils(HttpServerRequest request) {
		renderJson(request, new JsonArray(profils.toArray()));
	}

	public void listVisiblesGroupsProfil(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				visibleUsers(user.getUserId(), allGroupsProfilsClasse(),
						new Handler<JsonArray>() {

					@Override
					public void handle(JsonArray event) {
						renderJson(request, event);
					}
				});
			}
		});
	}

	private JsonArray allGroupsProfilsClasse() {
		JsonArray gpc = new JsonArray();
		for (String profil : profils) {
			gpc.addString("GROUPE_CLASSE_" + profil);
		}
		return gpc;
	}

	public void visibleUsers(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			List<String> expectedTypes = request.params().getAll("expectedType");
			visibleUsers(userId, new JsonArray(expectedTypes.toArray()), new Handler<JsonArray>() {

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
			JsonArray expectedTypes = message.body().getArray("expectedTypes");
			visibleUsers(userId, expectedTypes, new Handler<JsonArray>() {

				@Override
				public void handle(JsonArray res) {
					message.reply(res);
				}
			});
		} else {
			message.reply(new JsonArray());
		}
	}

	private void visibleUsers(String userId, JsonArray expectedTypes, final Handler<JsonArray> handler) {
		StringBuilder query = new StringBuilder()
			.append("START n = node:node_auto_index(id={userId}) ")
			.append("MATCH n-[:COMMUNIQUE*]->m ");
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("WHERE has(m.type) AND m.type IN ")
			.append(expectedTypes.encode().replaceAll("\"", "'"))
			.append(" ");
		}
		query.append("RETURN distinct m.id as id, m.name as name, m.type as type");
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		neo.send(query.toString(), params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = new JsonArray();
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject j = res.body().getObject("result");
					for (String idx : j.getFieldNames()) {
						r.addObject(j.getObject(idx));
					}
				}
				handler.handle(r);
			}
		});
	}
}
