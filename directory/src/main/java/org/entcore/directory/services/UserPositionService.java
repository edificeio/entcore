package org.entcore.directory.services;

import io.vertx.core.Future;
import org.entcore.directory.pojo.UserPosition;
import org.entcore.directory.pojo.UserPositionSource;

import java.util.Set;

public interface UserPositionService {
	Future<Set<UserPosition>> getUserPositions(Set<String> structureIds, String prefix);
	Future<UserPosition> getUserPosition(String userPositionId);
	Future<UserPosition> createUserPosition(String positionName, String structureId, String userId, UserPositionSource source);
	Future<UserPosition> renameUserPosition(String positionName, String positionId);
	Future<Void> deleteUserPosition(String positionId);
}
