/*
 * Copyright © "Open Digital Education", 2014
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

import java.util.HashMap;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.cas.entities.User;

public class PronoteRegisteredService extends DefaultRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(PronoteRegisteredService.class);

	@Override
	protected void prepareUser(User user, String userId, String service, JsonObject data) {
		user.setUser(data.getString(principalAttributeName));
		user.setAttributes(new HashMap<String, String>());

		try {
			if (data.getString("lastName") != null && data.getString("firstName") != null) {
				user.getAttributes().put("nom", data.getString("lastName"));
				user.getAttributes().put("prenom", data.getString("firstName"));
			}

			if (data.getString("birthDate") != null) {
				user.getAttributes().put("dateNaissance", data.getString("birthDate").replaceAll("([0-9]+)-([0-9]+)-([0-9]+)", "$3/$2/$1"));
			}
			if (data.getString("postalCode") != null) {
				user.getAttributes().put("codePostal", data.getString("postalCode"));
			}

			String category = null;
			JsonArray types = data.getJsonArray("type");
			for (Object type : types.getList()) {
			    switch(type.toString()) {
	                case "Student" :
	                    category = checkProfile(category,"National_1");
	                    break;
	                case "Teacher" :
	                    category = checkProfile(category,"National_3");
	                    break;
	                case "Relative" :
	                    category = checkProfile(category,"National_2");
	                    break;
	                case "Personnel" :
	                    category = checkProfile(category,"National_4");
	                    break;
	                }
			}
			if (category != null) {
				user.getAttributes().put("categories", category);
			}
		}
		catch (Exception e) {
			log.error("Failed to transform User for Pronote");
		}
	}
	private String checkProfile(String category, String national){
	    if(category== null){
            return national;
        }else{
           if(category.contains(national)){
               return category;
           }else{
               return category+";"+national;
           }
        }
    }
}
