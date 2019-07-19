/*
 * Copyright © "Open Digital Education", 2019
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

package org.entcore.registry.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.registry.services.LibraryService;
import org.entcore.registry.services.impl.DefaultLibraryService;

import static org.entcore.common.appregistry.LibraryUtils.LIBRARY_BUS_ADDRESS;

public class LibraryController extends BaseController {

    @BusAddress(LIBRARY_BUS_ADDRESS)
    public void add(final Message<JsonObject> message) {
        LibraryService service = new DefaultLibraryService(vertx.createHttpClient(), config);
        service.add().thenAccept(message::reply);
    }
}