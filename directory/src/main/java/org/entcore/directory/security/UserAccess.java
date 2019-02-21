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
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserInfos;

public class UserAccess extends AnyAdminOfUser {

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {

        String userId = request.params().get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            handler.handle(false);
            return;
        }
        if (userId.equals(user.getUserId())) {
            handler.handle(true);
            return;
        }
        super.authorize(request, binding, user, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if(event){
                    handler.handle(true);
                }else{
                    if (Vertx.currentContext().config().getBoolean("visible-check", true)) {
                        ((SecureHttpServerRequest) request).setAttribute("visibleCheck", "true");
                    }
                    DirectoryResourcesProvider.isTeacherOf(request, user, handler);
                }
            }
        });
    }
}
