package org.entcore.directory.services;

import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.pojo.UserPosition;
import org.entcore.directory.pojo.UserPositionSource;

import java.util.Optional;
import java.util.Set;

public interface UserPositionService {
	/**
	 * Retrieve user positions linked to the structures managed by the current admin
	 * @param prefix if present, filters the result : keeps only the user positions whose name match the prefix pattern
	 * @param structureId if present, filters the result : keeps the user positions linked to the structure
	 * @param adminInfos the current admin infos
	 * @return the retrieved user positions, filtered by prefix or structure
	 */
	Future<Set<UserPosition>> getUserPositions(Optional<String> prefix, Optional<String> structureId, UserInfos adminInfos);

	/**
	 * Retrieve a user position by its id
	 * @param userPositionId the id of the user position to retrieve
	 * @return the user position
	 */
	Future<UserPosition> getUserPosition(String userPositionId);

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

	/**
	 * Set the
	 * @param positionIds
	 * @param userId
	 * @return
	 */
	Future<Void> setUserPositions(Set<String> positionIds, String userId);
}
