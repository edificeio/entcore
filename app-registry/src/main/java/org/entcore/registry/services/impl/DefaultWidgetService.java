/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.registry.services.impl;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;

import java.util.List;
import java.util.UUID;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.registry.services.WidgetService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class DefaultWidgetService implements WidgetService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void createWidget(String applicationName, JsonObject widget, final Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, widget))
			return;
		if(widget.getString("name", "").trim().isEmpty())
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));

		JsonObject params = new JsonObject()
			.put("props", widget.put("id", UUID.randomUUID().toString()))
			.put("widgetName", widget.getString("name"));
		String linkApp = " ";

		if(applicationName != null && !applicationName.trim().isEmpty()){
			linkApp = "WITH w MATCH (a:Application {name: {applicationName}}) CREATE UNIQUE w<-[:HAS_WIDGET]-a ";
			params.put("applicationName", applicationName);
		}

		String query =
			"MATCH (w:Widget) " +
			"WHERE w.name = {widgetName} " +
			"WITH count(*) AS exists " +
			"WHERE exists=0 " +
			"CREATE (w:Widget {props}) " + linkApp +
			"RETURN w.id as id";

		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void listWidgets(final Handler<Either<String, JsonObject>> handler){
		String query =
			"MATCH (w:Widget) OPTIONAL MATCH (w)<-[:HAS_WIDGET]-(a:Application) "+
			"WITH w, a, length(a-[:PROVIDE]->(:WorkflowAction)) > 0 as workflowLinked " +
			"RETURN collect({id: w.id, name: w.name, js: w.js, path: w.path, i18n: w.i18n, " +
			"locked: w.locked, application: {id: a.id, name: a.name, address: a.address, strongLink: workflowLinked}}) as widgets";
		neo.execute(query, new JsonObject(), Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void getWidgetInfos(String widgetId, String structureId, final Handler<Either<String, JsonObject>> handler){
		if(widgetId == null || widgetId.trim().isEmpty() || structureId == null || structureId.trim().isEmpty())
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		String query =
			"MATCH (w:Widget) " +
			"WHERE w.id = {widgetId} " +
			"OPTIONAL MATCH (w)<-[:HAS_WIDGET]-(a:Application) " +
			"OPTIONAL MATCH (w)<-[rel:AUTHORIZED]-(g:Group)-[:DEPENDS]->()-[:BELONGS*0..1]->(s:Structure {id: {structureId}}) " +
			"WITH w, a, COLLECT({id: g.id, mandatory: coalesce(rel.mandatory, false)}) as groups " +
			"RETURN w, a, groups ";
		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("structureId", structureId);

		neo.execute(query, params, Neo4jResult.fullNodeMergeHandler("w", handler, "a"));
	}

	@Override
	public void deleteWidget(String widgetId, final Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (w:Widget {id: {widgetId}}) "+
			"OPTIONAL MATCH w-[any]-() " +
			"DELETE any, w";
		JsonObject params = new JsonObject().put("widgetId", widgetId);

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void toggleLock(String widgetId, final Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
				"MATCH (w:Widget {id: {widgetId}}) "+
				"SET w.locked = NOT coalesce(w.locked, false) " +
				"RETURN w.locked as locked";
			JsonObject params = new JsonObject().put("widgetId", widgetId);

			neo.execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void linkWidget(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty() || groupIds == null || groupIds.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (w:Widget {id: {widgetId}}), (g:Group) " +
			"WHERE g.id IN {groupIds} AND NOT(g-[:AUTHORIZED]->w) " +
			"AND COALESCE(w.locked ,false) = false " +
			"CREATE UNIQUE g-[:AUTHORIZED]->w";
		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("groupIds", new JsonArray(groupIds));

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void unlinkWidget(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty() || groupIds == null || groupIds.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (g:Group)-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE g.id IN {groupIds} " +
			"AND COALESCE(w.locked ,false) = false " +
			"DELETE rel";
		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("groupIds", new JsonArray(groupIds));

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void setMandatory(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty() || groupIds == null || groupIds.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (g:Group)-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE g.id IN {groupIds} " +
			"AND COALESCE(w.locked ,false) = false " +
			"SET rel.mandatory = true";
		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("groupIds", new JsonArray(groupIds));

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void removeMandatory(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty() || groupIds == null || groupIds.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (g:Group)-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE g.id IN {groupIds} " +
			"REMOVE rel.mandatory";
		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("groupIds", new JsonArray(groupIds));

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void massAuthorize(String widgetId, String structureId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		if(structureId == null || structureId .trim().isEmpty() ||
				widgetId == null || widgetId.trim().isEmpty() || profiles == null || profiles.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (w:Widget {id: {widgetId}}), " +
			"(parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
			"WHERE p.name IN {profiles} AND NOT(g-[:AUTHORIZED]->w) " +
			"CREATE UNIQUE g-[:AUTHORIZED]->w";

		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("structureId", structureId)
				.put("profiles", new JsonArray(profiles));

		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void massUnauthorize(String widgetId, String structureId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		if(structureId == null || structureId .trim().isEmpty() ||
				widgetId == null || widgetId.trim().isEmpty() || profiles == null || profiles.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
			"g-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE p.name IN {profiles} " +
			"DELETE rel";

		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("structureId", structureId)
				.put("profiles", new JsonArray(profiles));

			neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void massSetMandatory(String widgetId, String structureId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		if(structureId == null || structureId .trim().isEmpty() ||
				widgetId == null || widgetId.trim().isEmpty() || profiles == null || profiles.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
			"g-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE p.name IN {profiles} " +
			"SET rel.mandatory = true";

		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("structureId", structureId)
				.put("profiles", new JsonArray(profiles));

		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void massRemoveMandatory(String widgetId, String structureId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		if(structureId == null || structureId .trim().isEmpty() ||
				widgetId == null || widgetId.trim().isEmpty() || profiles == null || profiles.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
			"g-[rel:AUTHORIZED]->(w:Widget {id: {widgetId}}) " +
			"WHERE p.name IN {profiles} " +
			"AND COALESCE(w.locked ,false) = false " +
			"REMOVE rel.mandatory";

		JsonObject params = new JsonObject()
				.put("widgetId", widgetId)
				.put("structureId", structureId)
				.put("profiles", new JsonArray(profiles));

		neo.execute(query, params, validEmptyHandler(handler));
	}

}
