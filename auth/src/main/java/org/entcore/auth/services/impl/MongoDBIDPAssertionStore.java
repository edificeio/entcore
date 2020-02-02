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

 */

package org.entcore.auth.services.impl;

import org.entcore.auth.services.IDPAssertionStore;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.NameID;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MongoDBIDPAssertionStore implements IDPAssertionStore {

    private static final String IDP_ASSERTIONS_COLLECTION = "idpassertions";
    private MongoDb mongoDb = MongoDb.getInstance();

	public void store(Assertion assertion, Handler<AsyncResult<Void>> handler) {
        final NameID nameID = assertion.getSubject().getNameID();
        final JsonObject a = new JsonObject()
            .put("created", MongoDb.now())
            .put("nameId", nameID.getValue())
            .put("sessionIndex", assertion.getID())
            .put("idp", nameID.getNameQualifier())
            .put("sp", nameID.getSPNameQualifier());
        mongoDb.save(IDP_ASSERTIONS_COLLECTION, a, r -> {
            if (!"ok".equals(r.body().getString("status"))) {
                handler.handle(Future.failedFuture("Error persisting idp assertion : " +
                        a.encode() + " - " + r.body().getString("message")));
            } else {
                handler.handle(Future.succeededFuture());
            }
        });
    }

	public void retrieve(String nameId, Handler<AsyncResult<JsonArray>> handler) {
        final JsonObject query = new JsonObject().put("nameId", nameId);
        mongoDb.find(IDP_ASSERTIONS_COLLECTION, query, r -> {
            final JsonArray a = r.body().getJsonArray("results");
            if ("ok".equals(r.body().getString("status")) && a != null) {
                handler.handle(Future.succeededFuture(a));
            } else {
                handler.handle(Future.failedFuture(r.body().getString("message")));
            }
        });
    }

    public void delete(String id, Handler<AsyncResult<Void>> handler) {
        final JsonObject query = new JsonObject().put("_id", id);
        mongoDb.delete(IDP_ASSERTIONS_COLLECTION, query, r -> {
            if ("ok".equals(r.body().getString("status"))) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(r.body().getString("message")));
            }
        });
    }

}
