package org.entcore.directory.services;

import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.pojo.UserPosition;
import org.entcore.directory.pojo.UserPositionSource;

import java.util.Optional;
import java.util.Set;

public interface UserPositionService {
	Future<Set<UserPosition>> getUserPositions(Optional<String> prefix, Optional<String> structureId, UserInfos adminInfos);
	Future<UserPosition> getUserPosition(String userPositionId);
	Future<UserPosition> createUserPosition(String positionName, String structureId, UserPositionSource source, UserInfos adminInfos);
	Future<UserPosition> renameUserPosition(String positionName, String positionId, UserInfos adminInfos);
	Future<Void> deleteUserPosition(String positionId, String structureId, UserInfos adminInfos);
	Future<Void> attachUserPositions(Set<String> positionId, String userId);
}
