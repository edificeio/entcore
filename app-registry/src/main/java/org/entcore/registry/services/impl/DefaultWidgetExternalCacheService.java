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

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.buffer.Buffer;

import org.entcore.registry.services.WidgetExternalCacheService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.mongodb.MongoDbResult;

import fr.wseduc.webutils.Either;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;

import com.mongodb.QueryBuilder;

import java.util.Map;
import java.util.HashMap;

public class DefaultWidgetExternalCacheService implements WidgetExternalCacheService
{
    public static String EXTERNAL_CACHE_COLLECTION = "widgetExternalCache";

    static class WidgetExternalCacheConf
    {
        String  id;
        String  widgetName;
        String  url;
        int     ttl;

        public WidgetExternalCacheConf(String id, String widgetName, String url, int ttl)
        {
            this.id = id;
            this.widgetName = widgetName;
            this.url = url;
            this.ttl = ttl;
        }
    }

    private final MongoDb mongo = MongoDb.getInstance();
    private HttpClient httpClient;
    private Map<String, WidgetExternalCacheConf> cacheEntries = new HashMap<String, WidgetExternalCacheConf>();

    public DefaultWidgetExternalCacheService(JsonObject moduleConfig, HttpClient httpClient)
    {
        JsonArray config = moduleConfig.getJsonArray("widget-external-cache", new JsonArray());

        for(int i = config.size(); i-- > 0;)
        {
            JsonObject conf = config.getJsonObject(i);
            WidgetExternalCacheConf cache = new WidgetExternalCacheConf(
                conf.getString("id"), conf.getString("widget-name"), conf.getString("url"), conf.getInteger("ttl")
            );

            this.cacheEntries.put(cache.id, cache);
        }
        this.httpClient = httpClient;
    }

    public void getCache(String cacheId, UserInfos userinfos, Handler<Either<String, JsonObject>> handler)
    {
        WidgetExternalCacheConf cache = this.cacheEntries.get(cacheId);

        if(cache == null)
            handler.handle(new Either.Left<String, JsonObject>("widget.external.cache.unknown"));
        else
        {
            for(int i = userinfos.getWidgets().size(); i-- > 0;)
                if(userinfos.getWidgets().get(i).getName().equals(cache.widgetName) == true)
                {
                    this.getCache(cache, handler);
                    return;
                }
            handler.handle(new Either.Left<String, JsonObject>("widget.external.cache.unauthorized"));
        }
    }

    private void getCache(WidgetExternalCacheConf cache, Handler<Either<String, JsonObject>> handler)
    {
        final QueryBuilder builder = QueryBuilder.start("_id").is(cache.id);
        mongo.findOne(EXTERNAL_CACHE_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(new Handler<Either<String, JsonObject>>()
        {
            @Override
            public void handle(Either<String, JsonObject> res)
            {
                if(res.isLeft())
                    handler.handle(res);
                else
                {
                    JsonObject cacheHit = res.right().getValue();
                    if(cacheHit.getString("_id") != null)
                        handler.handle(new Either.Right<String, JsonObject>(cacheHit));
                    else
                        populateCache(cache, handler);
                }
            }
        }));
    }

    private void populateCache(WidgetExternalCacheConf cache, Handler<Either<String, JsonObject>> handler)
    {
        HttpClientRequest externalRequest = this.httpClient.getAbs(cache.url, new Handler<HttpClientResponse>()
        {
            @Override
            public void handle(final HttpClientResponse response)
            {
                if (response.statusCode() == 200)
                {
                    response.bodyHandler(new Handler<Buffer>()
                    {
                        @Override
                        public void handle(Buffer bodyBuffer)
                        {
                            JsonObject cacheEntry =
                                new JsonObject()
                                    .put("_id", cache.id)
                                    .put("cache", bodyBuffer.toString())
                                    .put("created", MongoDb.now())
                                    .put("expire", MongoDb.offsetFromNow(cache.ttl));

                            mongo.save(EXTERNAL_CACHE_COLLECTION, cacheEntry);

                            handler.handle(new Either.Right<String, JsonObject>(cacheEntry));
                        }
                    });
                }
                else
                    handler.handle(new Either.Left<String, JsonObject>("widget.external.cache.failure"));
            }
        });
        externalRequest.end();
    }
}
