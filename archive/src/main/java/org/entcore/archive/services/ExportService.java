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

package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public interface ExportService {

	void export(UserInfos user, String locale, JsonArray apps, JsonArray resourcesIds, HttpServerRequest request, Handler<Either<String, String>> handler);

	void userExportExists(UserInfos user, Handler<Boolean> handler);

	void userExportId(UserInfos user, Handler<String> handler);

	boolean userExportExists(String exportId);

	void waitingExport(String exportId, Handler<Boolean> handler);

	void exportPath(String exportId, Handler<Either<String, String>> handler);

	void exported(String exportId, String status, String locale, String host);

	void deleteExport(String exportId);

	void setDownloadInProgress(String exportId);

	boolean downloadIsInProgress(String exportId);

}
