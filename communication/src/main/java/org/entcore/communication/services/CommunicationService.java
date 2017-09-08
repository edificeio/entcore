/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.communication.services;

import fr.wseduc.webutils.Either;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface CommunicationService {

	//enum VisibleType { USERS, GROUPS, BOTH }
	enum Direction { INCOMING, OUTGOING, BOTH }

	void addLink(String startGroupId, String endGroupId,
			Handler<Either<String, JsonObject>> handler);

	void removeLink(String startGroupId, String endGroupId,
			Handler<Either<String, JsonObject>> handler);

	void addLinkWithUsers(String groupId, Direction direction,
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

	void usersCanSeeMe(String userId, final Handler<Either<String, JsonArray>> handler);

	void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams, String preFilter,
			Handler<Either<String, JsonArray>> handler);

	void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams,
			Handler<Either<String, JsonArray>> handler);

}
