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

package org.entcore.registry.services.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.registry.services.MongoWidgetExternalCacheService;

public class DefaultMongoWidgetExternalCacheService implements MongoWidgetExternalCacheService
{
    public static String EXTERNAL_CACHE_COLLECTION = "widgetExternalCache";
    private final MongoDb mongo = MongoDb.getInstance();

    public void get(String cacheId, Handler<Either<String, JsonObject>> handler) {
        final Bson builder = Filters.eq("_id", cacheId);
        mongo.findOne(EXTERNAL_CACHE_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(res -> {
            if(res.isLeft()) {
                handler.handle(res.left());
            } else {
                JsonObject cacheHit = res.right().getValue();
                if (cacheHit.getString("_id") != null) {
                    handler.handle(new Either.Right<String, JsonObject>(cacheHit));
                } else {
                    handler.handle(new Either.Right<String, JsonObject>(null));
                }
            }
        }));
    }

    public void put(String cachedId, String value, int ttl) {
        JsonObject cacheEntry =
                new JsonObject()
                        .put("_id", cachedId)
                        .put("cache", value)
                        .put("created", MongoDb.now())
                        .put("expire", MongoDb.offsetFromNow(ttl));

        mongo.save(EXTERNAL_CACHE_COLLECTION, cacheEntry);
    }
}
