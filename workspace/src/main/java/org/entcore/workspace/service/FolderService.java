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

package org.entcore.workspace.service;

import java.util.List;

import fr.wseduc.webutils.Either;

import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface FolderService {

	void create(String name, String path, String application, UserInfos author,
				Handler<Either<String, JsonObject>> result);

	void move(String id, String path, UserInfos author, Handler<Either<String, JsonObject>> result);

	void copy(String id, String name, String path, UserInfos author, long emptySize,
				Handler<Either<String, JsonArray>> result);

	void trash(String id, UserInfos author, Handler<Either<String, JsonObject>> result);

	void delete(String id, UserInfos author, Handler<Either<String, JsonArray>> result);

	void list(String name, UserInfos author, boolean hierarchical, String filter,
				Handler<Either<String, JsonArray>> results);

	void restore(String id, UserInfos author, Handler<Either<String, JsonObject>> result);

	void shareFolderAction(String id, UserInfos owner, List<String> actions, String groupId,
			String userId, ShareService shareService, boolean remove, Handler<Either<String, JsonObject>> result);

	void rename(String id, String newName, UserInfos owner, Handler<Either<String, JsonObject>> result);

	public void getParentRights(String parentName, String parentFolder, String owner, Handler<Either<String, JsonArray>> result);
	public void getParentRights(String parentName, String parentFolder, UserInfos owner, Handler<Either<String, JsonArray>> result);
}
