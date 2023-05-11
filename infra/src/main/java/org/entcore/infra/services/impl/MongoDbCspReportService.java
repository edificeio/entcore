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

package org.entcore.infra.services.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.entcore.infra.services.CspReportService;

import static org.entcore.common.mongodb.MongoDbResult.*;


public class MongoDbCspReportService implements CspReportService {

	public static final String CSP_VIOLATION_REPORT = "cspviolationreport";
	protected final MongoDb mongo;

	public MongoDbCspReportService() {
		mongo = MongoDb.getInstance();
	}

	@Override
	public Future<JsonObject> store(JsonObject cspReport) {
		Promise<JsonObject> promise = Promise.promise();
		mongo.save(CSP_VIOLATION_REPORT, cspReport, validActionResultHandler( result -> {
			if( result.isLeft() ) {
				promise.fail( result.left().getValue() );
			} else {
				promise.complete( result.right().getValue() );
			}
		}));
		return promise.future();
	}

}
