/*
 * Copyright © "Open Digital Education", 2020
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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.auth;

import org.entcore.auth.services.impl.AbstractSSOProvider;
import org.entcore.auth.services.impl.SSOEduConnect;
import org.entcore.common.neo4j.Neo4j;
import org.opensaml.saml2.core.Assertion;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class MockSSOProvider extends SSOEduConnect {

    public MockSSOProvider(boolean noPrefix) {
        super(noPrefix);
    }

    @Override
    protected boolean validConditions(Assertion assertion, Handler<Either<String, Object>> handler) {
        return true;
    }

    @Override
    protected void executeMultiVectorQuery(String query, JsonObject params, final Assertion assertion,
			final Handler<Either<String, Object>> handler) {
        System.out.println("executeMultiVectorQuery : " + query + (AbstractSSOProvider.RETURN_QUERY + ", s.name as structureName") + " - " + params.encode());
    }

    static void executeFederateQuery(String query, JsonObject params, final Assertion assertion,
            Neo4j neo4j, final Handler<Either<String, Object>> handler) {
        System.out.println("executeFederateQuery : " + query + AbstractSSOProvider.RETURN_QUERY + " - " + params.encode());
    }

}
