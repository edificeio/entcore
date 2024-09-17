/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.communication.services;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface CommunicationService {
	String IMPOSSIBLE_TO_CHANGE_DIRECTION = "impossible to change direction";
	String WARNING_ENDGROUP_USERS_CAN_COMMUNICATE = "endgroup-users-can-communicate";
	String WARNING_STARTGROUP_USERS_CAN_COMMUNICATE = "startgroup-users-can-communicate";
	String WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE = "both-groups-users-can-communicate";
	
	List<String> EXPECTED_TYPES = Arrays.asList(
			"User", "Group", "ManualGroup", "ProfileGroup", "FunctionalGroup", "FunctionGroup", "HTGroup", "CommunityGroup", "DirectionGroup");

	//enum VisibleType { USERS, GROUPS, BOTH }
	enum Direction { 
		INCOMING 	(0x01),
		OUTGOING 	(0x10), 
		BOTH 		(0x11),
		NONE 		(0x00);

		private Direction(int bitmask) {
			this.bitmask = bitmask;
		}
		public int bitmask;
		static public Direction fromString(String dbDirection) {
			if ("".equals(dbDirection) || dbDirection == null) {
				return Direction.NONE;
			}
			return Direction.valueOf(dbDirection.toUpperCase());
		}
		static public Direction fromBitmask(int bitmask) {
			final Direction[] values = {INCOMING, OUTGOING, BOTH};
			for( Direction value : values ) {
				if( value.bitmask == bitmask ) return value;
			}
			return NONE;
		}
	}

	void addLink(String startGroupId, String endGroupId,
			Handler<Either<String, JsonObject>> handler);

	void removeLink(String startGroupId, String endGroupId,
			Handler<Either<String, JsonObject>> handler);

	void addLinkWithUsers(String groupId, Direction direction,
						  Handler<Either<String, JsonObject>> handler);

	void addLinkWithUsers(Map<String, Direction> params,
						  Handler<Either<String, JsonObject>> handler);

	void removeLinkWithUsers(String groupId, Direction direction,
			Handler<Either<String, JsonObject>> handler);

	void communiqueWith(String groupId,// VisibleType filter,
			Handler<Either<String, JsonObject>> handler);

	void addLinkBetweenRelativeAndStudent(String groupId, Direction direction,
			Handler<Either<String, JsonObject>> handler);

	void removeLinkBetweenRelativeAndStudent(String groupId, Direction direction,
			Handler<Either<String, JsonObject>> handler);

	void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, final Integer transactionId,
						  final Boolean commit, final Handler<Either<String, JsonObject>> handler);

	void initDefaultRules(JsonArray structureIds, JsonObject defaultRules,
			Handler<Either<String, JsonObject>> handler);

	void applyDefaultRules(JsonArray structureIds, final Integer transactionId, final Boolean commit,
						   Handler<Either<String, JsonObject>> handler);

	void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler);

	void applyRules(String groupId, Handler<Either<String,JsonObject>> responseHandler);

	void removeRules(String structureId, Handler<Either<String, JsonObject>> handler);

	void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
					  boolean profile, String preFilter, String customReturn, JsonObject additionalParams,
					  Handler<Either<String, JsonArray>> handler);

	void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
			boolean profile, String preFilter, String customReturn, JsonObject additionalParams, String userProfile,
			boolean reverseUnion,
			Handler<Either<String, JsonArray>> handler);

	void usersCanSeeMe(String userId, final Handler<Either<String, JsonArray>> handler);

	void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams, String preFilter,
			Handler<Either<String, JsonArray>> handler);

	void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams,
			Handler<Either<String, JsonArray>> handler);

	void getOutgoingRelations(String id, Handler<Either<String, JsonArray>> results);

	void getIncomingRelations(String id, Handler<Either<String, JsonArray>> results);

	void safelyRemoveLinkWithUsers(String groupId, Handler<Either<String, JsonObject>> handler);

	void getDirections(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler);
	
	void addLinkCheckOnly(String startGroupId, String endGroupId, UserInfos userInfos, Handler<Either<String, JsonObject>> handler);
	
	void processChangeDirectionAfterAddingLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler);
	
	void removeRelations(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler);

	/**
	 * Indicates if a sender (user) can communicate to a receiver (user or group) on using
	 * Returns JsonObject :
	 * {
	 * canCommunicate : true/false
	 * }
	 *
	 * @param senderId    id of the sender
	 * @param recipientId id of the recipient
	 * @param handler     final handler
	 */
	void verify(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler);

	void getDiscoverVisibleUsers(String userId, JsonObject filter, final Handler<Either<String, JsonArray>> handler);

	void getDiscoverVisibleStructures(final Handler<Either<String, JsonArray>> handler);

	void discoverVisibleAddCommuteUsers(UserInfos user, String recipientId, HttpServerRequest request, Handler<Either<String, JsonObject>> handler);

	void discoverVisibleRemoveCommuteUsers(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler);

	void discoverVisibleGetGroups(String userId, Handler<Either<String, JsonArray>> handler);

	void discoverVisibleGetUsersInGroup(String userId, String groupId,  Handler<Either<String, JsonArray>> handler);

	void createDiscoverVisibleGroup(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler);

	void updateDiscoverVisibleGroup(String userId, String groupId, JsonObject body, Handler<Either<String, JsonObject>> handler);

	void addDiscoverVisibleGroupUsers(UserInfos user, String groupId, JsonObject body, HttpServerRequest request, Handler<Either<String, JsonObject>> handler);

	void getDiscoverVisibleAcceptedProfile(Handler<Either<String, JsonArray>> handler);
	
	void searchVisibleContacts(UserInfos user, String search, String language, Handler<Either<String, JsonArray>> results);
}

