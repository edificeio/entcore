package org.entcore.common.user.position.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jQueryAndParams;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.position.UserPosition;
import org.entcore.common.user.position.UserPositionService;
import org.entcore.common.user.position.UserPositionSource;
import org.entcore.common.utils.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class DefaultUserPositionService implements UserPositionService {

	public static final String ADMIN_WITHOUT_STRUCTURE = "Admin not linked to any structure.";

	private final Logger logger = LoggerFactory.getLogger(DefaultUserPositionService.class);

	private final Neo4j neo4jClient = Neo4j.getInstance();

	private final EventBus eventBus;

	/** WB-3374 Restrict CRUD operations to ADMC. */
	private final Boolean restrictCrudToADMC;

  public DefaultUserPositionService(EventBus eventBus, boolean restrictCrudToADMC) {
    this.eventBus = eventBus;
	this.restrictCrudToADMC = Boolean.valueOf(restrictCrudToADMC);
  }

  private Future<Void> checkCrudAccess(final UserInfos adminInfos) {
    if(restrictCrudToADMC.booleanValue() && !adminInfos.isADMC()) {
        return Future.failedFuture("common.service.admc-only");
    }
	return Future.succeededFuture();
 }

  @Override
	public Future<Set<UserPosition>> getUserPositions(UserInfos user) {
		return getUserPositions(null, null, null, user);
	}

	@Override
	public Future<Set<UserPosition>> getUserPositions(String content, String structureId, UserInfos adminInfos) {
		return getUserPositionsForAdmin(null, content, structureId, adminInfos);
	}

	@Override
	public Future<UserPosition> getUserPosition(String userPositionId, UserInfos adminInfos) {
		Promise<UserPosition> promise = Promise.promise();
		getUserPositionsForAdmin(userPositionId, null, null, adminInfos)
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

	private Future<Set<UserPosition>> getUserPositionsForAdmin(String positionId, String content, String structureId, UserInfos adminInfos) {
		return checkCrudAccess(adminInfos)
			.recover( throwable -> {
				// Read access may be authorized if the structure is known.
				return structureId==null ? Future.failedFuture(throwable) : Future.succeededFuture();
			})
			.compose( Void -> fetchAdminStructures(adminInfos) )
			.compose(structureIds -> {
				if (structureIds.isEmpty()) {
					logger.warn(ADMIN_WITHOUT_STRUCTURE);
					return Future.failedFuture(ADMIN_WITHOUT_STRUCTURE);
				}
				return getUserPositions(positionId, content, structureId, new ArrayList<String>(structureIds));
			});
	}

	private Future<Set<UserPosition>> getUserPositions(String positionId, String prefix, String structureId, UserInfos userInfos) {
		return getUserPositions(
			positionId,
			prefix,
			structureId,
			userInfos.getStructures() == null ? Collections.emptyList() : userInfos.getStructures()
		);
	}

	private Future<Set<UserPosition>> getUserPositions(String positionId, String content, String structureId, List<String> structureIds) {
		Promise<Set<UserPosition>> promise = Promise.promise();
		final JsonObject params = new JsonObject()
			.put("structureIds", new JsonArray(structureIds));

		final StringBuilder query = new StringBuilder();
		query.append("MATCH (p:UserPosition)-[:IN]->(s:Structure) ")
				.append("WHERE s.id IN {structureIds} ");
		//filters user positions whose id match the specified user position id
		if (!StringUtils.isEmpty(positionId)) {
			query.append("AND p.id = {positionId} ");
			params.put("positionId", positionId);
		}
		// filters user positions whose name don't match the content
		if (content != null) {
			final String simplifiedContent = getSimplifiedString(content);
			query.append("AND p.simplifiedName =~ {contentRegex} ");
			params.put("contentRegex", ".*" + simplifiedContent + ".*");
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
		checkCrudAccess(adminInfos)
		.compose( Void -> fetchAdminStructures(adminInfos) )
		.onSuccess(adminStructureIds -> {
			if (adminStructureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else if(!adminStructureIds.contains(structureId)) {
				logger.warn("The user tried to create a position on structure {0} but only can access structures {1}", structureId, adminStructureIds);
				promise.fail("cannot.create.position.on.this.structure");
			} else {
				final String finalPositionName = positionName.trim();
				getPositionByNameInStructure(finalPositionName, structureId, adminStructureIds).onSuccess(userPosition -> {
					if (userPosition.isPresent()) {
						promise.fail("position.already.exists:"+userPosition.get().getId());
					} else {
						final String simplifiedName = getSimplifiedString(finalPositionName);
						final JsonArray adminStructureArray = new JsonArray();
						adminStructureIds.forEach(adminStructureArray::add);
						final String positionId = UUID.randomUUID().toString();
						final JsonObject userPositionProps = new JsonObject()
								.put("id", positionId)
								.put("name", finalPositionName)
								.put("simplifiedName", simplifiedName)
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
		checkCrudAccess(adminInfos)
		.compose( Void -> fetchAdminStructures(adminInfos) )
		.onSuccess(structureIds -> {
			if (structureIds.isEmpty()) {
				logger.warn(ADMIN_WITHOUT_STRUCTURE);
				promise.fail(ADMIN_WITHOUT_STRUCTURE);
			} else {
				final String simplifiedName = getSimplifiedString(positionName);
				final JsonArray adminStructureIds = new JsonArray();
				structureIds.forEach(adminStructureIds::add);
				final JsonObject params = new JsonObject()
						.put("positionId", positionId)
						.put("positionName", positionName)
						.put("simplifiedName", simplifiedName)
						.put("adminStructureIds", adminStructureIds);


				final String query =
						// Get the desired position and its structure
						"MATCH (p:UserPosition {id: {positionId}})-[:IN]->(s:Structure) " +
						// Ensure the structure is accessible by the user
						"WHERE s.id IN {adminStructureIds} " +
						// Try to find a position with the name we're trying to use...
						"OPTIONAL MATCH (s)<-[:IN]-(otherPosition:UserPosition{name:{positionName}}) " +
						// ...which is not the one we're trying to rename
						"WHERE otherPosition.id <> p.id " +
						// Store the fact that a position whith the same name already exists
						"WITH p, count(otherPosition) AS conflict, s " +
						"WITH p, s, conflict, CASE WHEN conflict = 0 THEN true ELSE false END AS canUpdate " +
						// Perform the update only if no conflict was detected
						"FOREACH (_ IN CASE WHEN canUpdate THEN [1] ELSE [] END | " +
							"SET p.name = {positionName}, p.simplifiedName = {simplifiedName} " +
						")" +
						// Return the field of the position and a boolean 'updated' which is false if we could not perform the
						// update because a conflict was detected
						"RETURN canUpdate AS updated, p.id as id, p.name as name, p.source as source, s.id as structureId ";
				neo4jClient.execute(query, params, Neo4jResult.validUniqueResultHandler(event -> {
					if (event.isLeft()) {
						logger.warn("Failed renaming user position : " + event.left().getValue());
						promise.fail(event.left().getValue());
					} else {
						JsonObject result = event.right().getValue();
						if (result.containsKey("updated") && !result.getBoolean("updated")) {
							promise.fail("position.name.already.used");
						} else if (result.containsKey("id")) {
							promise.complete(createUserPositionFromResult(result));
						} else {
							promise.fail("position.not.accessible");
						}
					}
				}));
			}
		}).onFailure(promise::fail);
		return promise.future();
	}

	@Override
	public Future<Void> deleteUserPosition(String positionId, UserInfos adminInfos) {
		Promise<Void> promise = Promise.promise();
		checkCrudAccess(adminInfos)
		.compose( Void -> fetchAdminStructures(adminInfos) )
		.onSuccess(structureIds -> {
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
	public Future<Neo4jQueryAndParams> getUserPositionSettingQueryAndParam(final Set<String> positionIds, final String userId, final String callerId) {
		final Promise<UserInfos> promise = Promise.promise();
		if(callerId == null) {
			final UserInfos adminInfos = new UserInfos();
			Map<String, UserInfos.Function> functions = new HashMap<>();
			functions.put(DefaultFunctions.SUPER_ADMIN, new UserInfos.Function());
			adminInfos.setFunctions(functions);
			promise.complete(adminInfos);
		} else {
			UserUtils.getUserInfos(eventBus, callerId, promise::complete);
		}
		return promise.future().compose(adminInfos -> fetchAdminStructures(adminInfos).map(adminStructureIds -> {
			final String query;
			final JsonArray sids = new JsonArray();
			adminStructureIds.forEach(sids::add);
			final JsonObject
				params = new JsonObject()
				.put("userId", userId)
				.put("structureIds", sids);
			if(CollectionUtils.isEmpty(positionIds)) {
				query =
					"MATCH (u:User {id:{userId}})-[h:HAS_POSITION]->(p:UserPosition)-[:IN]->(s:Structure) " +
					"WHERE head(u.profiles) IN ['Teacher', 'Personnel'] AND s.id in {structureIds} " +
					"DELETE h";
			} else {
				final JsonArray positionIdsArray = new JsonArray();
				positionIds.forEach(positionIdsArray::add);
				params.put("positionIds", positionIdsArray);
				query =
					"MATCH (u:User {id:{userId}}) " +
						"WHERE head(u.profiles) IN ['Teacher', 'Personnel'] " +
						"OPTIONAL MATCH (u)-[h:HAS_POSITION]->(p:UserPosition)-[:IN]->(s: Structure) " +
						"WHERE s.id IN {structureIds}" +
						"DELETE h " +
						"WITH u " +
						"MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure)<-[:IN]-(p:UserPosition) " +
						"WHERE p.id IN {positionIds} AND s.id IN {structureIds} " +
						"MERGE (u)-[:HAS_POSITION]->(p) ";
			}
			return new Neo4jQueryAndParams(query, params);
		}));
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
	
	/**
	 * Static method to detach a user from his positions in a Structure,
	 * in Feeder importation process.
	 * Note: in Feeder context, external IDs are used.
	 * @param userExternalId the user external ID
	 * @param structureExternalId the structure external ID
	 * @param sourceFilter a filter on the source of the user positions to detach
	 * @param transactionHelper the transaction helper for current query to commit
	 */
	public static void detachUserFromItsPositions(String userExternalId, String structureExternalId, UserPositionSource sourceFilter, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
				.put("structureExternalId", structureExternalId)
				.put("userExternalId", userExternalId);
		StringBuilder query = new StringBuilder()
				.append("MATCH (u:User {externalId: {userExternalId}})-[hasPosition:HAS_POSITION]->(p:UserPosition)-[:IN]->(s:Structure {externalId: {structureExternalId}}) ");
		if (sourceFilter != null) {
			query.append("WHERE p.source = {sourceFilter} ");
			params.put("sourceFilter", sourceFilter.name());
		}
		query.append("DELETE hasPosition ");
		transactionHelper.add(query.toString(), params);
	}

	/**
	 * Static method to detach and delete user positions of a source from a Structure.
	 * Note: in Feeder context, the structure id used is the external structure id
	 * @param source the source of user positions to detach
	 * @param structureExternalId external ID of the structure
	 * @param transactionHelper the transaction helper for current query to commit
	 */
	public static void detachSourcedUserPositionsFromStructure(UserPositionSource source, String structureExternalId, TransactionHelper transactionHelper) {
		JsonObject params = new JsonObject()
			.put("externalId", structureExternalId)
			.put("source", source.toString());
		String query =
				"MATCH (s:Structure {externalId : {externalId}})<-[:IN]-(p:UserPosition {source: {source}}) " +
				"DETACH DELETE p";
		transactionHelper.add(query, params);
	}

	/**
	 * Static method to create user positions in Feeder importation process
	 * Note: in Feeder context, the structure id used is the external structure id
	 * @param userPosition the user position to create
	 * @param transactionHelper the transaction helper for current query to commit
	 */
	public static void createUserPosition(UserPosition userPosition, TransactionHelper transactionHelper) {
		String query =
				"MATCH (s:Structure {externalId : {structureExternalId}}) " +
				"MERGE (s)<-[:IN]-(p:UserPosition {name : {positionName}}) " +
				"ON CREATE SET " +
				"   p.id = {id}, " +
				"   p.simplifiedName = {simplifiedName}, " +
				"   p.source = {source} ";
		JsonObject params = new JsonObject()
				.put("structureExternalId", userPosition.getStructureId())
				.put("positionName", userPosition.getName())
				.put("id", UUID.randomUUID().toString())
				.put("simplifiedName", DefaultUserPositionService.getSimplifiedString(userPosition.getName()))
				.put("source", userPosition.getSource().toString());
		transactionHelper.add(query, params);
	}

	/**
	 * Static method to link positions to a user in Feeder importation process
	 * Note: in Feeder context, the structure id used is the external structure id
	 * @param userPosition the user position to link
	 * @param userExternalId the external id of the target user
	 * @param transactionHelper transaction helper for current query to commit
	 */
	public static void linkPositionToUser(UserPosition userPosition, String userExternalId, TransactionHelper transactionHelper) {
		String query = "" +
				"MATCH (p:UserPosition {name: {positionName}})-[:IN]->(s:Structure {externalId : {structureExternalId}}), (u:User {externalId : {userExternalId}}) " +
				"MERGE (u)-[:HAS_POSITION]->(p) ";
		JsonObject params = new JsonObject()
				.put("structureExternalId", userPosition.getStructureId())
				.put("userExternalId", userExternalId)
				.put("positionName", userPosition.getName());
		transactionHelper.add(query, params);
	}

	private static UserPosition createUserPositionFromResult(JsonObject jsonResult) {
		return new UserPosition(jsonResult.getString("id"),
				jsonResult.getString("name"),
				UserPositionSource.valueOf(jsonResult.getString("source")),
				jsonResult.getString("structureId"));
	}

	/**
	 * Simplifies source string (removes diacritics and sets to lower case)
	 * @param source the source string to be simplified
	 * @return the source string set to lower case and without diacritics
	 */
	public static String getSimplifiedString(String source) {
		return Pattern.compile("\\p{M}")
				.matcher(Normalizer.normalize(source, Normalizer.Form.NFD))
				.replaceAll("")
				.toLowerCase();
	}
}
