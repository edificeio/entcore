/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.controllers;

import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Either;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.impl.DefaultSchoolService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class StructureController extends Controller {

	private final SchoolService structureService;

	public StructureController(Vertx vertx, Container container, RouteMatcher rm,
						   Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		Neo neo = new Neo(eb,log);
		structureService = new DefaultSchoolService(neo, eb);
	}

	@SecuredAction("structure.link.user")
	public void linkUser(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String userId = request.params().get("userId");
		structureService.link(structureId, userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					if (r.right().getValue() != null && r.right().getValue().size() > 0) {
						JsonArray a = new JsonArray().addString(userId);
						ApplicationUtils.sendModifiedUserGroup(eb, a, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								JsonObject j = new JsonObject()
										.putString("action", "setDefaultCommunicationRules")
										.putString("schoolId", structureId);
								eb.send("wse.communication", j);
							}
						});
						renderJson(request, r.right().getValue(), 200);
					} else {
						notFound(request);
					}
				} else {
					renderJson(request, new JsonObject().putString("error", r.left().getValue()), 400);
				}
			}
		});
	}

	@SecuredAction("structure.unlink.user")
	public void unlinkUser(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String structureId = request.params().get("structureId");
		structureService.unlink(structureId, userId, notEmptyResponseHandler(request));
	}

}

