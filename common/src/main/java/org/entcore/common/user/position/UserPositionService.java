package org.entcore.common.user.position;

import io.vertx.core.Future;
import org.entcore.common.neo4j.Neo4jQueryAndParams;
import org.entcore.common.user.UserInfos;

import java.util.Set;

public interface UserPositionService {
	/** Check if CRUD operations are restricted to ADMC. */
	Boolean isCrudRestrictedToADMC();

	/**
	 * Retrieve user positions linked to structures the user or admin is attached to.
	 * @return the retrieved user positions
	 */
	Future<Set<UserPosition>> getUserPositions(UserInfos userInfos);

	/**
	 * Retrieve user positions linked to the structures managed by the current admin
	 * @param content if present, filters the result : keeps only the user positions whose name match the content pattern
	 * @param structureId if present, filters the result : keeps the user positions linked to the structure
	 * @param adminInfos the current admin infos
	 * @return the retrieved user positions, filtered by content or structure
	 */
	Future<Set<UserPosition>> getUserPositions(String content, String structureId, UserInfos adminInfos);

	/**
	 * Retrieve a user position by its id
	 * @param userPositionId the id of the user position to retrieve
	 * @param adminInfos the current admin infos
	 * @return the user position
	 */
	Future<UserPosition> getUserPosition(String userPositionId, UserInfos adminInfos);

	/**
	 * create a user position and links it to a structure
	 * @param positionName the name of the user position being created
	 * @param structureId the id of the structure it must be attached to
	 * @param source the type of source of the user position being created
	 * @param adminInfos the current admin infos
	 * @return the newly created user position
	 */
	Future<UserPosition> createUserPosition(String positionName, String structureId, UserPositionSource source, UserInfos adminInfos);

	/**
	 * Rename the user position
	 * @param positionName the new user position name
	 * @param positionId the id of the user position to rename
	 * @param adminInfos the current admin infos
	 * @return the renamed user position
	 */
	Future<UserPosition> renameUserPosition(String positionName, String positionId, UserInfos adminInfos);

	/**
	 * Delete a user position in a specific structure
	 * @param positionId the id of the position to delete
	 * @param adminInfos the current admin infos
	 * @return void
	 */
	Future<Void> deleteUserPosition(String positionId, UserInfos adminInfos);

	Future<Neo4jQueryAndParams> getUserPositionSettingQueryAndParam(Set<String> positionIds, String userId, String callerId);
}
