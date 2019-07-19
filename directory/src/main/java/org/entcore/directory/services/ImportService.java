/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.pojo.ImportInfos;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ImportService {

	void validate(ImportInfos importInfos, UserInfos user, Handler<Either<JsonObject, JsonObject>> handler);

	void validate(String id, UserInfos user, Handler<Either<JsonObject, JsonObject>> handler);

	void doImport(ImportInfos result, Handler<Either<JsonObject, JsonObject>> eitherHandler);

	void doImport(String importId, Handler<Either<JsonObject, JsonObject>> eitherHandler);

	void columnsMapping(ImportInfos result, Handler<Either<JsonObject,JsonObject>> handler);

	void classesMapping(ImportInfos result, Handler<Either<JsonObject,JsonObject>> handler);

	void addLine(String importId, String profile, JsonObject line, Handler<Either<String,JsonObject>> handler);

	void updateLine(String importId, String profile, JsonObject line, Handler<Either<String,JsonObject>> handler);

	void deleteLine(String importId, String profile, Integer line, Handler<Either<String,JsonObject>> handler);

	void findById(String importId,  Handler<Either<String,JsonObject>> handler);
}
