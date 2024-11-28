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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;

import java.util.Map;


public class AdmlOfStructureOrClassOrTeachOfUser implements ResourcesProvider {

    private final Neo4j neo = Neo4j.getInstance();

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {
        //superadmin
        if (user.getFunctions() != null && user.getFunctions().containsKey("SUPER_ADMIN")) {
            request.resume();
            handler.handle(true);
            return;
        }
        //
        new AdmlOfStructureOrClass().authorize(request, binding, user, res -> {
            if (res) {
                handler.handle(res);
            } else {
                if (TeacherOfClass.userNotHasClassParam(user)) {
                    handler.handle(false);
                } else {
                    new TeacherOfUserFromDifferentClass().authorize(request, binding, user, handler);
                }
            }
        });
    }
}
