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


import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.AntivirusClient;

import fr.wseduc.webutils.Utils;

import java.util.Optional;

public abstract class FileValidator extends AbstractValidator<JsonObject, JsonObject> {

    public static FileValidator createNullable(JsonObject fs) {
        try {
            if (fs != null && !fs.isEmpty()) {
                final FileValidator fileValidator = new QuotaFileSizeValidation();
                final JsonArray blockedExtensions = fs.getJsonArray("blockedExtensions");
                if (blockedExtensions != null && blockedExtensions.size() > 0) {
                    fileValidator.setNext(new ExtensionValidator(blockedExtensions));
                }
                return fileValidator;
            }
        } catch (Exception e) {
                LoggerFactory.getLogger(FileValidator.class).warn("Could not create file validator: ", e);
        }
        return null;
    }

    public static Optional<FileValidator> create(JsonObject fs) {
        return Optional.ofNullable(createNullable(fs));
    }

    public static Future<Optional<FileValidator>> create(Vertx vertx) {
        final Promise<Optional<FileValidator>> promise = Promise.promise();
        vertx.sharedData().<String, String>getAsyncMap("server")
        .compose(serverMap -> serverMap.get("file-system"))
        .onSuccess(s ->
            promise.complete(create(Utils.isNotEmpty(s) ?  new JsonObject(s) : new JsonObject()))
        ).onFailure(promise::fail);
        return promise.future();
    }

}
