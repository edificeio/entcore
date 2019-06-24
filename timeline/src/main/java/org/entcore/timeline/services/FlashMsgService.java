/*
 * Copyright © "Open Digital Education", 2018
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
 */

package org.entcore.timeline.services;

import java.util.List;

import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface FlashMsgService {

	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void delete(String id, Handler<Either<String, JsonObject>> handler);
	public void deleteMultiple(List<String> ids, Handler<Either<String, JsonObject>> handler);
	public void list(String domain, Handler<Either<String, JsonArray>> handler);
	public void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler);
	public void getSubstructuresByMessageId(String messageId, Handler<Either<String, JsonArray>> handler);
	public void setSubstructuresByMessageId(String messageId, JsonObject subStructures, Handler<Either<String, JsonArray>> handler);

	//public void duplicate(String id, Handler<Either<String, JsonObject>> handler);

	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler);
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler);

}
