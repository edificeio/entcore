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

package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class EleaRegisteredService extends DefaultRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(EleaRegisteredService.class);
    private static final String directoryAction = "getUser";


    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String, Object> conf) {
        super.configure(eb, conf);
    }

    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();
        JsonObject jo = new JsonObject();
        jo.put("action", directoryAction).put("userId", userId).put("withClasses", true);
        eb.request("directory", jo, handlerToAsyncHandler(new io.vertx.core.Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject res = event.body().getJsonObject("result");
                log.debug("res : " + res);
                if ("ok".equals(event.body().getString("status")) && res != null) {
                    User user = new User();
                    prepareUser(user, userId, service, res);
                    userHandler.handle(user);
                    createStatsEvent(authCas, res, service);
                } else {
                    userHandler.handle(null);
                }
            }
        }));
    }

    @Override
    protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
        if (principalAttributeName != null) {
            user.setUser(data.getString(principalAttributeName));
            data.remove(principalAttributeName);
        } else {
            user.setUser(userId);
        }
        data.remove("password");

        Map<String, String> attributes = new HashMap<>();

        if (data.containsKey("externalId")) {
            attributes.put("externalId", data.getValue("externalId").toString());
        }

        if (data.containsKey("id")) {
            attributes.put("id", data.getValue("id").toString());
        }

        if (data.containsKey("joinKey")) {
            attributes.put("joinKey", data.getValue("joinKey").toString());
        }

        if (data.containsKey("type")) {
            attributes.put("type", data.getValue("type").toString());
        }

        if (data.containsKey("profiles")) {
            attributes.put("profiles", data.getValue("profiles").toString());
        }

        if (data.containsKey("firstName")) {
            attributes.put("firstName", data.getValue("firstName").toString());
        }

        if (data.containsKey("lastName")) {
            attributes.put("lastName", data.getValue("lastName").toString());
        }

        if (data.containsKey("displayName")) {
            attributes.put("displayName", data.getValue("displayName").toString());
        }

        if (data.containsKey("classCategories")) {
            attributes.put("classCategories", data.getValue("classCategories").toString());
        }

        // The classes2D is a re-construction of classes with subject to make all classes uniform using a precise format : structure.externalId$class.name
        if (data.containsKey("classes2D")) {
            attributes.put("classes", data.getValue("classes2D").toString());
        } else if (data.containsKey("classes") && !data.containsKey("classes2D")) {
            attributes.put("classes", data.getValue("classes").toString());
        }

        if (data.containsKey("functionalGroups")) {
            attributes.put("functionalGroups", data.getValue("functionalGroups").toString());
        }

        if (data.containsKey("email")) {
            attributes.put("email", data.getValue("email").toString());
        }

        if (data.containsKey("structures")) {
            attributes.put("structures", data.getValue("structures").toString());
        }

        if (data.containsKey("administrativeStructures")) {
            attributes.put("administrativeStructures", data.getValue("administrativeStructures").toString());
        }

        if (data.containsKey("structureNodes")) {
            attributes.put("structureNodes", data.getValue("structureNodes").toString());
        }

        user.setAttributes(attributes);
    }

}
