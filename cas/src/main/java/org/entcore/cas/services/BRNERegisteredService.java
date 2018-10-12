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

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class BRNERegisteredService extends DefaultRegisteredService {

    protected static final String PROFIL = "ENTPersonProfils";

    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, Map<String,Object> conf){
        super.configure(eb, conf);
    }

    @Override
    protected void prepareUser(User user, String userId, String service, JsonObject data) {
        //return the first uai as principalAttributeName
        for (Object s : data.getJsonArray("structureNodes", new fr.wseduc.webutils.collections.JsonArray())) {
            if (s == null || !(s instanceof JsonObject)) continue;
            JsonObject structure = (JsonObject) s;
            String uai = structure.getString("UAI", "");
            if (!uai.isEmpty()) {
                user.setUser(uai);
                break;
            }
        }

        user.setAttributes(new HashMap<String, String>());

        // Profile
        JsonArray profiles = data.getJsonArray("type", new fr.wseduc.webutils.collections.JsonArray());
        if (profiles.contains("Teacher")) {
            user.getAttributes().put(PROFIL, "National_3");
        } else if (profiles.contains("Student")) {
            user.getAttributes().put(PROFIL, "National_1");
        }
    }
}
