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

package org.entcore.common.validation;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.AntivirusClient;

import java.util.Optional;

public abstract class FileValidator extends AbstractValidator<JsonObject, JsonObject> {

    public static Optional<FileValidator> create(Vertx vertx) {
        try {
            final LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
            final String s = (String) server.get("file-system");
            if (s != null) {
                final JsonObject fs = new JsonObject(s);
                final FileValidator fileValidator = new QuotaFileSizeValidation();
                final JsonArray blockedExtensions = fs.getJsonArray("blockedExtensions");
                if (blockedExtensions != null && blockedExtensions.size() > 0) {
                    fileValidator.setNext(new ExtensionValidator(blockedExtensions));
                }
                return Optional.of(fileValidator);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(AntivirusClient.class).warn("Could not create file validator: ", e);
        }
        return Optional.empty();
    }
}
