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

import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class MSELRegisteredService extends AbstractCas20ExtensionRegisteredService {
    
    private static final Logger log = LoggerFactory.getLogger(MSELRegisteredService.class);
    
    protected static final String MSEL_ID = "uid";
    protected static final String MSEL_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
    protected static final String MSEL_PROFILES = "ENTPersonProfils";
    
    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf){
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

  
    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        // TODO Auto-generated method stub
        user.setUser(data.getString(principalAttributeName));
        try{
            //uid
            if (data.containsKey("externalId")) {
                additionnalAttributes.add(createTextElement(MSEL_ID, data.getString("externalId"), doc));
            }
            // Structures
            for (Object o : data.getJsonArray("structures", new fr.wseduc.webutils.collections.JsonArray()).getList()) {
                if (o == null || !(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;
                if (structure.containsKey("UAI")) {
                    additionnalAttributes.add(createTextElement(MSEL_STRUCTURE_UAI, structure.getString("UAI"), doc));
                }
            }
            
            // Profile
            switch(data.getString("type")) {
            case "Student" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_1", doc));
                break;
            case "Teacher" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_3", doc));
                break;
            case "Relative" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_2", doc));
                break;
            case "Personnel" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_4", doc));
                break;
            }
        } catch (Exception e) {
            log.error("Failed to transform User for Mon stage en ligne", e);
        }
    }

}
