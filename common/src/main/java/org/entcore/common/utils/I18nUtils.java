/*
 * Copyright © "Open Digital Education", 2023
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

package org.entcore.common.utils;

import org.entcore.common.http.i18n.I18nBusRequest;

import fr.wseduc.webutils.Server;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class I18nUtils {

    /**
     * Translate some i18n keys with translations found in another module.
     * @param vertx
     * @param request
     * @param module the targeted module
     * @param keys i18n keys to translate
     * @param args optional parameters for keys translation
     * @return a future map of [key, translation]
     */
    public static Future<JsonObject> getI18nOfModule(
            final Vertx vertx,
            final HttpServerRequest request,
            final String module, 
            final String[] keys,
            final String[] args
            ) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject m = I18nBusRequest.translate(request, keys, args).toMessage();
        Server.getEventBus(vertx).request(module+".i18n", m, ar -> {
            if(ar.succeeded()) {
                final JsonObject body = (JsonObject) ar.result().body();
                promise.complete(body);
            }else{
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }
}
