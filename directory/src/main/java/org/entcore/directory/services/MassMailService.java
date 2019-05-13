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

package org.entcore.directory.services;


import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface MassMailService {
	void massmailNoCheck(String structureId, JsonObject filter, boolean groupChildren, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void massmailUsers(String structureId, JsonObject filter, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void massmailUsers(String structureId, JsonObject filter, boolean groupClasses, boolean groupChildren,
					   Boolean hasMail, boolean performCheck, UserInfos userInfos, Handler<Either<String, JsonArray>> results);
	void massMailAllUsersByStructure(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void massMailUser(String userId, UserInfos userInfos, Handler<Either<String, JsonArray>> results);

	void massMailTypeMail(UserInfos user, final HttpServerRequest request, final String templatePath, final JsonArray users);

	void massMailTypeCSV(final HttpServerRequest request, JsonArray users);

	void massMailTypePdf(UserInfos user, final HttpServerRequest request, final String templatePath, final String baseUrl, final String filename, final String type, final JsonArray users);

}
