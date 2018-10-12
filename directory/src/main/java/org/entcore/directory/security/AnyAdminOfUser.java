/*
 * Copyright © "Open Digital Education", 2018
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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Set;

import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class AnyAdminOfUser implements ResourcesProvider {

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {

        //Super-admin "hack"
        if(user.getFunctions().containsKey(SUPER_ADMIN)) {
            handler.handle(true);
            return;
        }
        String userId = request.params().get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            handler.handle(false);
            return;
        }

        Set<String> ids = DirectoryResourcesProvider.getIds(user);
        String query =
                "MATCH (u:User {id : {userId}})-[:IN]->()-[:DEPENDS]->()-[:BELONGS*0..1]->s2 " +
                        "WHERE s2.id IN {ids} RETURN count(*) > 0 as exists UNION " +
                        "MATCH (u: User {id : {userId}})-[:HAS_RELATIONSHIPS]->(b: Backup) " +
                        "WHERE ANY(structId IN b.structureIds WHERE structId IN {ids}) " +
                        "RETURN count(*) > 0 as exists";
        JsonObject params = new JsonObject()
                .put("id", request.params().get("groupId"))
                .put("userId", userId)
                .put("ids", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ids)));
        request.pause();
        Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> r) {
                request.resume();
                JsonArray res = r.body().getJsonArray("result");
                if ("ok".equals(r.body().getString("status")) &&
                        res.size() == 2 && (((JsonObject) res.getJsonObject(0)).getBoolean("exists", false)
                        || ((JsonObject) res.getJsonObject(1)).getBoolean("exists", false))) {
                    handler.handle(true);
                } else if ("ok".equals(r.body().getString("status")) && res.size() == 1 &&
                        ((JsonObject) res.getJsonObject(0)).getBoolean("exists", false)) {
                    handler.handle(true);
                } else {
                    handler.handle(false);
                }
            }
        });
    }
}
