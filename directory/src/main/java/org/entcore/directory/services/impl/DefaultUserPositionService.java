package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.directory.pojo.UserPosition;
import org.entcore.directory.pojo.UserPositionSource;
import org.entcore.directory.services.UserPositionService;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DefaultUserPositionService implements UserPositionService {

	private final Neo4j neo4jClient = Neo4j.getInstance();

	@Override
	public Future<Set<UserPosition>> getUserPositions(Set<String> structureIds, String prefix) {
		Promise<Set<UserPosition>> promise = Promise.promise();
		final String prefixRegex = prefix + ".*";
		final JsonArray structureIdsArray = new JsonArray();
		structureIds.forEach(structureIdsArray::add);
		final String query = "" +
				"MATCH (p:UserPosition)-[:IN]->(s:Structure) " +
				"WHERE p.name =~ {prefixRegex} " +
				"AND s.id IN {structureIds} " +
				"RETURN p.id as id, p.name as name, p.source as source ";
		final JsonObject params = new JsonObject()
				.put("prefixRegex", prefixRegex)
				.put("structureIds", structureIdsArray);
		neo4jClient.execute(query, params, Neo4jResult.validResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
			} else {
				Set<UserPosition> userPositions = new HashSet<>();
				JsonArray results = event.right().getValue();
				results.forEach(result -> {
					JsonObject jsonResult = (JsonObject) result;
					userPositions.add(new UserPosition(jsonResult.getString("id"), jsonResult.getString("name"), UserPositionSource.valueOf(jsonResult.getString("source"))));
				});
				promise.complete(userPositions);
			}
		}));
		return promise.future();
	}

	@Override
	public Future<UserPosition> getUserPosition(String userPositionId) {
		Promise<UserPosition> promise = Promise.promise();
		final String query = "" +
				"MATCH (p:UserPosition {id: {userPositionId}}) " +
				"RETURN DISTINCT p.id as id, p.name as name, p.source as source ";
		neo4jClient.execute(query, new JsonObject().put("userPositionId", userPositionId), Neo4jResult.validUniqueResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
			} else {
				JsonObject result = event.right().getValue();
				if (result.isEmpty()) {
					promise.fail("No user position found.");
				}
				promise.complete(new UserPosition(result.getString("id"), result.getString("name"), UserPositionSource.valueOf(result.getString("source"))));
			}
		}));
		return promise.future();
	}

	@Override
	public Future<UserPosition> createUserPosition(String positionName, String structureId, String userId, UserPositionSource source) {
		Promise<UserPosition> promise = Promise.promise();
		final String positionId = UUID.randomUUID().toString();
		final JsonObject userPositionProps = new JsonObject()
				.put("id", positionId)
				.put("name", positionName)
				.put("source", source.toString());
		final String query = "" +
				"MATCH (u:User {id: {userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure {id: {structureId}}) " +
				"CREATE (u)-[:HAS_POSITION]->(p:UserPosition {userPositionProps})-[:IN]->(s) " +
				"RETURN p.id as id, p.name as name, p.source as source ";
		final JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("structureId", structureId)
				.put("userPositionProps", userPositionProps);
		neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
			} else {
				JsonObject result = event.right().getValue();
				promise.complete(new UserPosition(result.getString("id"), result.getString("name"), UserPositionSource.valueOf(result.getString("source"))));
			}
		}));
		return promise.future();
	}

	@Override
	public Future<UserPosition> renameUserPosition(String positionName, String positionId) {
		Promise<UserPosition> promise = Promise.promise();
		final String query = "" +
				"MATCH (u:UserPosition {id: {positionId}}) " +
				"SET u.name = {positionName} " +
				"RETURN DISTINCT u.id as id, u.name as name, u.source as source ";
		final JsonObject params = new JsonObject()
				.put("positionId", positionId)
				.put("positionName", positionName);
		neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
			} else {
				JsonObject result = event.right().getValue();
				promise.complete(new UserPosition(result.getString("id"), result.getString("name"), UserPositionSource.valueOf(result.getString("source"))));
			}
		}));
		return promise.future();
	}

	@Override
	public Future<Void> deleteUserPosition(String positionId) {
		Promise<Void> promise = Promise.promise();
		final String query = "" +
				"MATCH (:User)-[h:HAS_POSITION]->(p:UserPosition {id: {positionId}})-[i:IN]->(:Structure) " +
				"DELETE h,p,i ";
		neo4jClient.execute(query, new JsonObject().put("positionId", positionId), Neo4jResult.validResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
			} else {
				promise.complete();
			}
		}));
		return promise.future();
	}
}
