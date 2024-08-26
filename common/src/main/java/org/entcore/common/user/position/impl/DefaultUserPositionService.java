package org.entcore.common.user.position.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jQueryAndParams;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.position.UserPosition;
import org.entcore.common.user.position.UserPositionService;
import org.entcore.common.user.position.UserPositionSource;
import org.entcore.common.utils.StringUtils;

import java.util.*;

public class DefaultUserPositionService implements UserPositionService {

	public static final String ADMIN_WITHOUT_STRUCTURE = "Admin not linked to any structure.";

	private final Logger logger = LoggerFactory.getLogger(DefaultUserPositionService.class);

	private final Neo4j neo4jClient = Neo4j.getInstance();

	@Override
	public Future<Set<UserPosition>> getUserPositions(String prefix, String structureId, UserInfos adminInfos) {
		return getAdminUserPositions(null, prefix, structureId, adminInfos);
	}

	@Override
	public Future<Set<UserPosition>> getUserPositionsInStructure(String prefix, String structureId, UserInfos user) {
		return getUserPositions(null, prefix, structureId, user);
	}

	@Override
	public Future<UserPosition> getUserPosition(String userPositionId, UserInfos adminInfos) {
		Promise<UserPosition> promise = Promise.promise();
		getAdminUserPositions(userPositionId, null, null, adminInfos)
				.onSuccess(userPositions -> {
					if (userPositions.isEmpty()) {
						final String error = "No user position found.";
						logger.warn(error);
						promise.fail(error);
					} else {
						promise.complete(userPositions.stream().findFirst().get());
					}
				})
				.onFailure(promise::fail);
		return promise.future();
	}

	private Future<Set<UserPosition>> getAdminUserPositions(String positionId, String prefix, String structureId, UserInfos adminInfos) {
		Promise<Set<UserPosition>> promise = Promise.promise();
		fetchAdminStructures(adminInfos).onSuccess(structureIds -> {
			if (structureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else {
				final JsonArray adminStructureIds = new JsonArray();
				structureIds.forEach(adminStructureIds::add);
				final JsonObject params = new JsonObject()
						.put("adminStructureIds", adminStructureIds);

				final StringBuilder query = new StringBuilder();
				query.append("MATCH (p:UserPosition)-[:IN]->(s:Structure) ")
						.append("WHERE s.id IN {adminStructureIds} ");
				//filters user positions whose id match the specified user position id
				if (!StringUtils.isEmpty(positionId)) {
					query.append("AND p.id = {positionId} ");
					params.put("positionId", positionId);
				}
				// filters user positions whose name don't match the prefix
				if (prefix != null) {
					query.append("AND p.name =~ {prefixRegex} ");
					params.put("prefixRegex", prefix + ".*");
				}
				// filters user positions related to a specific structure
				if (!StringUtils.isEmpty(structureId)) {
					query.append("AND s.id = {structureId} ");
					params.put("structureId", structureId);
				}
				query.append("RETURN p.id as id, p.name as name, p.source as source, s.id as structureId ");
				neo4jClient.execute(query.toString(), params, Neo4jResult.validResultHandler(event -> {
					if (event.isLeft()) {
						logger.warn("Failed fetching user positions : " + event.left().getValue());
						promise.fail(event.left().getValue());
					} else {
						Set<UserPosition> userPositions = new HashSet<>();
						JsonArray results = event.right().getValue();
						results.forEach(result -> {
							JsonObject jsonResult = (JsonObject) result;
							userPositions.add(createUserPositionFromResult(jsonResult));
						});
						promise.complete(userPositions);
					}
				}));
			}
		}).onFailure(promise::fail);
		
		return promise.future();
	}

	private Future<Set<UserPosition>> getUserPositions(String positionId, String prefix, String structureId, UserInfos userInfos) {
		Promise<Set<UserPosition>> promise = Promise.promise();
		final JsonObject params = new JsonObject()
			.put("userStructureIds", new JsonArray(
				userInfos.getStructures() == null ? Collections.emptyList() : userInfos.getStructures()
			));

		final StringBuilder query = new StringBuilder();
		query.append("MATCH (p:UserPosition)-[:IN]->(s:Structure) ")
				.append("WHERE s.id IN {userStructureIds} ");
		//filters user positions whose id match the specified user position id
		if (!StringUtils.isEmpty(positionId)) {
			query.append("AND p.id = {positionId} ");
			params.put("positionId", positionId);
		}
		// filters user positions whose name don't match the prefix
		if (prefix != null) {
			query.append("AND p.name =~ {prefixRegex} ");
			params.put("prefixRegex", prefix + ".*");
		}
		// filters user positions related to a specific structure
		if (!StringUtils.isEmpty(structureId)) {
			query.append("AND s.id = {structureId} ");
			params.put("structureId", structureId);
		}
		query.append("RETURN p.id as id, p.name as name, p.source as source, s.id as structureId ");
		neo4jClient.execute(query.toString(), params, Neo4jResult.validResultHandler(event -> {
			if (event.isLeft()) {
				logger.warn("Failed fetching user positions : " + event.left().getValue());
				promise.fail(event.left().getValue());
			} else {
				Set<UserPosition> userPositions = new HashSet<>();
				JsonArray results = event.right().getValue();
				results.forEach(result -> {
					JsonObject jsonResult = (JsonObject) result;
					userPositions.add(createUserPositionFromResult(jsonResult));
				});
				promise.complete(userPositions);
			}
		}));
	
		return promise.future();
	}

	@Override
	public Future<UserPosition> createUserPosition(String positionName, String structureId, UserPositionSource source, UserInfos adminInfos) {
		Promise<UserPosition> promise = Promise.promise();
		fetchAdminStructures(adminInfos).onSuccess(adminStructureIds -> {
			if (adminStructureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else {
				getPositionByNameInStructure(positionName, structureId, adminStructureIds).onSuccess(userPosition -> {
					if (userPosition.isPresent()) {
						promise.complete(userPosition.get());
					} else {
						final JsonArray adminStructureArray = new JsonArray();
						adminStructureIds.forEach(adminStructureArray::add);
						final String positionId = UUID.randomUUID().toString();
						final JsonObject userPositionProps = new JsonObject()
								.put("id", positionId)
								.put("name", positionName)
								.put("source", source.toString());
						final JsonObject params = new JsonObject()
								.put("structureId", structureId)
								.put("adminStructureIds", adminStructureArray)
								.put("userPositionProps", userPositionProps);
						final String query = "" +
								"MATCH (s:Structure {id:{structureId}}) " +
								"WHERE s.id IN {adminStructureIds} " +
								"CREATE UNIQUE (p:UserPosition {userPositionProps})-[:IN]->(s) " +
								"RETURN p.id as id, p.name as name, p.source as source, s.id as structureId ";
						neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
							if (event.isLeft()) {
								logger.warn("Failed creating user position : " + event.left().getValue());
								promise.fail(event.left().getValue());
							} else {
								JsonObject result = event.right().getValue();
								promise.complete(createUserPositionFromResult(result));
							}
						}));
					}
				}).onFailure(promise::fail);
			}
		}).onFailure(promise::fail);
		return promise.future();
	}

	@Override
	public Future<UserPosition> renameUserPosition(String positionName, String positionId, UserInfos adminInfos) {
		Promise<UserPosition> promise = Promise.promise();
		fetchAdminStructures(adminInfos).onSuccess(structureIds -> {
			if (structureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else {
				final JsonArray adminStructureIds = new JsonArray();
				structureIds.forEach(adminStructureIds::add);
				final JsonObject params = new JsonObject()
						.put("positionId", positionId)
						.put("positionName", positionName)
						.put("adminStructureIds", adminStructureIds);

				final String query = "" +
						"MATCH (p:UserPosition {id: {positionId}})-[:IN]->(s:Structure) " +
						"WHERE s.id IN {adminStructureIds} " +
						"SET p.name = {positionName} " +
						"RETURN p.id as id, p.name as name, p.source as source, s.id as structureId ";
				neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
					if (event.isLeft()) {
						logger.warn("Failed renaming user position : " + event.left().getValue());
						promise.fail(event.left().getValue());
					} else {
						JsonObject result = event.right().getValue();
						promise.complete(createUserPositionFromResult(result));
					}
				}));
			}
		}).onFailure(promise::fail);
		return promise.future();
	}

	@Override
	public Future<Void> deleteUserPosition(String positionId, UserInfos adminInfos) {
		Promise<Void> promise = Promise.promise();
		fetchAdminStructures(adminInfos).onSuccess(structureIds -> {
			if (structureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else {
				final JsonArray adminStructureIds = new JsonArray();
				structureIds.forEach(adminStructureIds::add);
				final JsonObject params = new JsonObject()
						.put("positionId", positionId)
						.put("adminStructureIds", adminStructureIds);
				final String query = "" +
						"MATCH (p:UserPosition {id:{positionId}})-[:IN]->(s:Structure) " +
						"WHERE s.id IN {adminStructureIds} " +
						"DETACH DELETE p ";
				neo4jClient.execute(query, params, Neo4jResult.validResultHandler(event -> {
					if (event.isLeft()) {
						logger.warn("Failed deleting user position : " + event.left().getValue());
						promise.fail(event.left().getValue());
					} else {
						promise.complete();
					}
				}));
			}}).onFailure(promise::fail);
		return promise.future();
	}

	/**
	 * Build a neo4j query and the associated params that link the positions identified with the position ids to the user
	 * Note : if one of the specified position doesn't exist in the user's structures, he will not be linked to it.
	 * @param positionIds the ids of the positions to be linked to the user
	 * @param userId the id of the user
	 * @return the neo4j query and the associated params
	 */
	public static Neo4jQueryAndParams getUserPositionSettingQueryAndParam(Set<String> positionIds, String userId) {
		final JsonArray positionIdsArray = new JsonArray();
		positionIds.forEach(positionIdsArray::add);
		final JsonObject params = new JsonObject()
				.put("positionIds", positionIdsArray)
				.put("userId", userId);
		final String query = "" +
				"MATCH (u:User {id:{userId}}) " +
			  "OPTIONAL MATCH (u)-[h:HAS_POSITION]->(p:UserPosition) " +
				"WHERE p is not null and NOT p.id IN {positionIds} " +
				"DELETE h " +
				"WITH u " +
				"MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure)<-[:IN]-(p:UserPosition) " +
				"WHERE p.id IN {positionIds} " +
				"MERGE (u)-[:HAS_POSITION]->(p) ";
		return new Neo4jQueryAndParams(query, params);
	}

	private Future<Optional<UserPosition>> getPositionByNameInStructure(String userPositionName, String structureId, Set<String> adminStructureIds) {
		Promise<Optional<UserPosition>> promise = Promise.promise();
		final JsonArray adminStructureArray = new JsonArray();
		adminStructureIds.forEach(adminStructureArray::add);
		JsonObject params = new JsonObject()
				.put("adminStructureArray", adminStructureArray)
				.put("structureId", structureId)
				.put("userPositionName", userPositionName);
		final String query = "" +
				"MATCH (p:UserPosition)-[:IN]-(s:Structure) " +
				"WHERE s.id IN {adminStructureArray} " +
				"AND s.id = {structureId} " +
				"AND p.name = {userPositionName} " +
				"RETURN p.id as id, p.name as name, p.source as source, s.id as structureId ";
		neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
			if (event.isLeft()) {
				logger.warn("Failed retrieving positions by name in structure : " + event.left().getValue());
				promise.fail(event.left().getValue());
			} else {
				JsonObject result = event.right().getValue();
				if (result.isEmpty()) {
					promise.complete(Optional.empty());
				} else {
					promise.complete(Optional.of(createUserPositionFromResult(result)));
				}
			}
		}));
		return promise.future();
	}

	private Future<Set<String>> fetchAdminStructures(UserInfos adminInfos) {
		Promise<Set<String>> promise = Promise.promise();
		final StringBuilder query = new StringBuilder();
		final JsonObject params = new JsonObject();
		if (adminInfos.isADMC()) {
			query.append("MATCH (s:Structure) ");
		} else if (adminInfos.isADML()) {
			query.append("MATCH (:User {id:{adminId}})-[:IN]->(:FunctionGroup {filter:\"AdminLocal\"})-[:DEPENDS]->(s:Structure) ");
			params.put("adminId", adminInfos.getUserId());
		} else {
			promise.complete(new HashSet<>());
			return promise.future();
		}
		query.append("RETURN s.id as id");
		neo4jClient.execute(query.toString(), params, Neo4jResult.validResultHandler(event -> {
			if (event.isLeft()) {
				logger.warn("Failed fetching structures of admin : " + event.left().getValue());
				promise.fail(event.left().getValue());
			} else {
			JsonArray results = event.right().getValue();
			Set<String> structureIds = new HashSet<>();
			results.forEach(result -> {
				JsonObject jsonResult = (JsonObject) result;
				structureIds.add(jsonResult.getString("id"));
			});
			promise.complete(structureIds);
			}
		}));
		return promise.future();
	}

	private static UserPosition createUserPositionFromResult(JsonObject jsonResult) {
		return new UserPosition(jsonResult.getString("id"),
				jsonResult.getString("name"),
				UserPositionSource.valueOf(jsonResult.getString("source")),
				jsonResult.getString("structureId"));
	}
}
