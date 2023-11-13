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

package org.entcore.common.redis;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Redis {

    private RedisClient redisClient;


    private Redis() {
    }

    private static class RedisHolder {
        private static final Redis instance = new Redis();
        private static JsonObject redisConfig;
    }

    public static Redis getInstance() {
        return RedisHolder.instance;
    }

    public void init(Vertx vertx, JsonObject redisConfig) {
        this.redisClient = RedisClient.create(vertx, new JsonObject().put("redisConfig", redisConfig));
        RedisHolder.redisConfig = redisConfig;
    }

    public RedisClient getRedisClient() {
        return this.redisClient;
    }

    public static RedisClient getClient() {
        return getInstance().getRedisClient();
    }

    public static RedisClient createClientForDb(Vertx vertx, Integer db) {
        if(RedisHolder.redisConfig.getInteger("select", 0).equals(db)) {
            return getInstance().getRedisClient();
        }
        final JsonObject newRedisConfig = RedisHolder.redisConfig.copy();
        newRedisConfig.put("select", db);
        return RedisClient.create(vertx, newRedisConfig);
    }

}
