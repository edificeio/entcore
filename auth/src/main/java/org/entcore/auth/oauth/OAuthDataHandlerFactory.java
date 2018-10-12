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

package org.entcore.auth.oauth;

import fr.wseduc.mongodb.MongoDb;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;
import jp.eisbahn.oauth2.server.models.Request;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.common.neo4j.Neo4j;

public class OAuthDataHandlerFactory implements DataHandlerFactory {

	private final Neo4j neo;
	private final MongoDb mongo;
	private final OpenIdConnectService openIdConnectService;
	private final boolean checkFederatedLogin;

	public OAuthDataHandlerFactory(Neo4j neo, MongoDb mongo, OpenIdConnectService openIdConnectService, boolean cfl) {
		this.neo = neo;
		this.mongo = mongo;
		this.openIdConnectService = openIdConnectService;
		this.checkFederatedLogin = cfl;
	}

	@Override
	public DataHandler create(Request request) {
		return new OAuthDataHandler(request, neo, mongo, openIdConnectService, checkFederatedLogin);
	}

}
