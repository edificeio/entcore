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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.ServiceTicket;

import io.vertx.core.eventbus.EventBus;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonObject;
import org.entcore.cas.mapping.Mapping;

public interface RegisteredService {

	Optional<Mapping> foundMappingByService(final Set<String> structureIds, final String serviceUri);

	void configure(EventBus eb, Map<String, Object> configuration);

	boolean matches(final AuthCas authCas, String serviceUri);

	void getUser(AuthCas authCas, String service, Handler<User> userHandler);

	String formatService(String serviceUri, ServiceTicket st);

	void addPatterns(String structureId,String... pattern);

	void cleanPatterns();

	JsonObject getInfos(String acceptLanguage);

	String getId();

}
