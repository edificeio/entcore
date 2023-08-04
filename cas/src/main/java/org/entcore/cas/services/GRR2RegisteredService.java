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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class GRR2RegisteredService extends AbstractCas20ExtensionRegisteredService
{
    private static final Logger log = LoggerFactory.getLogger(GRR2RegisteredService.class);
    protected static final String GRR_PROFILE = "profil";
    protected static final String GRR_FIRSTNAME = "firstname";
    protected static final String GRR_LASTNAME = "lastname";
    protected static final String GRR_EMAIL = "email";
    protected static final String GRR_FULLNAME = "fullname";

    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf)
    {
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes)
    {
        final List<String> lExtId = StringUtils.split(data.getString("externalId"), "-");
        //uuid contains five -
        if (lExtId != null && !lExtId.isEmpty() && lExtId.size() < 5) {
            //ext id without prefix
            user.setUser(lExtId.get(lExtId.size() - 1));
        } else {
            user.setUser(data.getString("externalId"));
        }

        try
        {
            // Profile
            switch(data.getString("type")) {
                case "Student" :
                    additionnalAttributes.add(createTextElement(GRR_PROFILE, "National_1", doc));
                    break;
                case "Teacher" :
                    additionnalAttributes.add(createTextElement(GRR_PROFILE, "National_3", doc));
                    break;
                case "Relative" :
                    additionnalAttributes.add(createTextElement(GRR_PROFILE, "National_2", doc));
                    break;
                case "Personnel" :
                    additionnalAttributes.add(createTextElement(GRR_PROFILE, "National_4", doc));
                    break;
            }

            if(data.containsKey("firstName"))
                additionnalAttributes.add(createTextElement(GRR_FIRSTNAME, data.getString("firstName"), doc));

            if(data.containsKey("lastName"))
                additionnalAttributes.add(createTextElement(GRR_LASTNAME, data.getString("lastName"), doc));

            if(data.containsKey("displayName"))
                additionnalAttributes.add(createTextElement(GRR_FULLNAME, data.getString("displayName"), doc));

            if(data.containsKey("emailAcademy"))
                additionnalAttributes.add(createTextElement(GRR_EMAIL, data.getString("emailAcademy"), doc));
            else if (data.containsKey("email"))
                additionnalAttributes.add(createTextElement(GRR_EMAIL, data.getString("email"), doc));
        }
        catch (Exception e)
        {
            log.error("Failed to transform User for GRR2", e);
        }
    }

}
