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
			"User", "Group", "ManualGroup", "ProfileGroup", "FunctionalGroup", "FunctionGroup", "HTGroup", "CommunityGroup");

	//enum VisibleType { USERS, GROUPS, BOTH }
	enum Direction { INCOMING, OUTGOING, BOTH, NONE }

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

	void initDefaultRules(JsonArray structureIds, JsonObject defaultRules,
			Handler<Either<String, JsonObject>> handler);

	void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler);

	void applyRules(String groupId, Handler<Either<String,JsonObject>> responseHandler);

	void removeRules(String structureId, Handler<Either<String, JsonObject>> handler);

	void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
					  boolean profile, String preFilter, String customReturn, JsonObject additionnalParams,
					  Handler<Either<String, JsonArray>> handler);

	void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
			boolean profile, String preFilter, String customReturn, JsonObject additionnalParams, String userProfile,
			Handler<Either<String, JsonArray>> handler);

	void usersCanSeeMe(String userId, final Handler<Either<String, JsonArray>> handler);

	void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams, String preFilter,
			Handler<Either<String, JsonArray>> handler);

	void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams,
			Handler<Either<String, JsonArray>> handler);

	void getGroupsReachableByGroup(String id, Handler<Either<String, JsonArray>> results);

	void safelyRemoveLinkWithUsers(String groupId, Handler<Either<String, JsonObject>> handler);

	void getDirections(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler);
	
	void addLinkCheckOnly(String startGroupId, String endGroupId, UserInfos userInfos, Handler<Either<String, JsonObject>> handler);
	
	void processChangeDirectionAfterAddingLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler);
}
