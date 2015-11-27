package org.entcore.registry.services.impl;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;

import java.util.List;
import java.util.UUID;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.registry.services.WidgetService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class DefaultWidgetService implements WidgetService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void createWidget(String applicationId, JsonObject widget, final Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, widget))
			return;
		if(widget.getString("name", "").trim().isEmpty())
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));

		String linkApp = " ";
		if(applicationId != null && !applicationId.trim().isEmpty()){
			linkApp = "WITH w CREATE UNIQUE (w)<-[:HAS_WIDGET]-(:Application {id: {applicationId}}) ";
		}
		String query =
			"MATCH (w:Widget) " +
			"WHERE w.name = {widgetName} " +
			"WITH count(*) AS exists " +
			"WHERE exists=0 " +
			"CREATE (w:Widget {props}) " + linkApp +
			"RETURN w.id as id";
		JsonObject params = new JsonObject()
			.putObject("props", widget.putString("id", UUID.randomUUID().toString()))
			.putString("widgetName", widget.getString("name"));
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void listWidgets(final Handler<Either<String, JsonArray>> handler){
		String query =
			"MATCH (w:Widget) OPTIONAL MATCH (w)<-[:HAS_WIDGET]-(a:Application) "+
			"RETURN collect({id: w.id, name: w.name, js: w.js, path: w.path, application: {id: a.id, name: a.name}}) as widgets";
		neo.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void getWidgetInfos(String widgetId, String structureId, final Handler<Either<String, JsonObject>> handler){
		if(widgetId == null || widgetId.trim().isEmpty() || structureId == null || structureId.trim().isEmpty())
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		String query =
				"MATCH (w:Widget) " +
				"WHERE w.id = {widgetId} " +
				"OPTIONAL MATCH (w)<-[:HAS_WIDGET]-(a:Application) " +
				"OPTIONAL MATCH (w)<-[:AUTHORIZED]-(g:Group)-[:DEPENDS]->()-[:BELONGS*0..1]->(s:Structure {id: {structureId}}) " +
				"WITH w, a, COLLECT(g) as groups " +
				"RETURN w, a, groups ";
		JsonObject params = new JsonObject()
				.putString("widgetId", widgetId)
				.putString("structureId", structureId);

		neo.execute(query, params, Neo4jResult.fullNodeMergeHandler("w", handler, "a"));
	}

	@Override
	public void deleteWidget(String widgetId, final Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (w:Widget {id: {widgetId}})-[any]-() "+
			"DELETE any, w";
		JsonObject params = new JsonObject().putString("widgetId", widgetId);

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

	@Override
	public void linkWidget(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
		if(widgetId == null || widgetId.trim().isEmpty() || groupIds == null || groupIds.isEmpty()){
			handler.handle(new Either.Left<String, JsonObject>("invalid.parameters"));
		}

		String query =
			"MATCH (w:Widget {id: {widgetId}}), (g:Group) " +
			"WHERE g.id IN {groupIds} AND NOT(g-[:AUTHORIZED]->w) " +
			"AND (NOT(w<-[:HAS_WIDGET]-(Application)) OR w<-[:HAS_WIDGET]-(:Application)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(:Role)<-[:AUTHORIZED]-g) " +
			"CREATE UNIQUE g-[:AUTHORIZED]->w";
		JsonObject params = new JsonObject()
				.putString("widgetId", widgetId)
				.putArray("groupIds", new JsonArray(groupIds.toArray()));

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
			"AND (NOT(w<-[:HAS_WIDGET]-(Application)) OR NOT(w<-[:HAS_WIDGET]-(:Application)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(:Role)<-[:AUTHORIZED]-g)) " +
			"DELETE rel";
		JsonObject params = new JsonObject()
				.putString("widgetId", widgetId)
				.putArray("groupIds", new JsonArray(groupIds.toArray()));

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
			"SET rel.mandatory = true";
		JsonObject params = new JsonObject()
				.putString("widgetId", widgetId)
				.putArray("groupIds", new JsonArray(groupIds.toArray()));

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
				.putString("widgetId", widgetId)
				.putArray("groupIds", new JsonArray(groupIds.toArray()));

		neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
	}

}
