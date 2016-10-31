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
