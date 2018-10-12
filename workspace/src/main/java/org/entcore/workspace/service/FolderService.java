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

	void shareFolderAction(String id, UserInfos owner, JsonObject share, ShareService shareService, Handler<Either<String, JsonObject>> result);

	void rename(String id, String newName, UserInfos owner, Handler<Either<String, JsonObject>> result);

	public void getParentRights(String parentName, String parentFolder, String owner, Handler<Either<String, JsonArray>> result);
	public void getParentRights(String parentName, String parentFolder, UserInfos owner, Handler<Either<String, JsonArray>> result);
}
