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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.exceptions.ImportException;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;
import org.entcore.directory.services.MassMessagingService;

import java.io.File;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.utils.FileUtils.deleteImportPath;


public class MassMessagingController extends BaseController {

	private MassMessagingService massMessagingService;
	private ImportService importService;
	private Storage storage;

	public MassMessagingController(MassMessagingService massMessagingService, ImportService importService, Storage storage) {
		this.massMessagingService = massMessagingService;
		this.importService = importService;
		this.storage = storage;
	}

	@Get("")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void view(HttpServerRequest request) {
		renderView(request);
	}



	@Post("/massmessaging/column/mapping")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void csvColumnsMapping(final HttpServerRequest request) {
		importService.uploadImport(request, new Handler<AsyncResult<ImportInfos>>() {
			@Override
			public void handle(AsyncResult<ImportInfos> event) {
				if (event.succeeded()) {
					massMessagingService.csvColumnsMapping(event.result(), reportResponseHandler(vertx, storage, event.result().getPath(), request));
				} else {
					badRequest(request, event.cause().getMessage());
				}
			}
		});
	}

	@Get("/massmessaging/senderName")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void getSenderName(final HttpServerRequest request) {

		final JsonObject messageConfig =  new JsonObject();
		messageConfig.put("sender-id", config.getString("massMessagingDefaultSenderId"));
		massMessagingService.getSenderDisplayName(request, messageConfig,new Handler<Either<JsonObject, String>>() {
			@Override
			public void handle(Either<JsonObject, String> event) {
				if(event.isRight()) {
					JsonObject response = new JsonObject().put("senderName", event.right().getValue());
					Renders.renderJson(request, response);
				} else {
					notFound(request, event.left().getValue().getString("error"));
				}
			}
		});
	}

	@Post("/massmessaging/validation/populate")
	@ResourceFilter(AdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@MfaProtected()
	public void populateImportedInfos(final HttpServerRequest request) {

		massMessagingService.validateMassMessaging(request, new Handler<Either<JsonObject, JsonArray>>() {
            @Override
            public void handle(Either<JsonObject, JsonArray> event) {
                if(event.isRight()) {
					JsonObject response = new JsonObject().put("csvHeaders", event.right().getValue());
					Renders.renderJson(request, response);
                } else {
                    badRequest(request, event.left().getValue().getString("message"));
                }
            }
        });
	}

    @Post("/massmessaging")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @MfaProtected()
    public void massMessaging(final HttpServerRequest request) {

		final JsonObject messageConfig =  new JsonObject();
		messageConfig.put("sender-id", config.getString("massMessagingDefaultSenderId"));

		massMessagingService.publishMassMessages(request, messageConfig,new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
					JsonObject response = new JsonObject().put("remainingLogins", event.right().getValue().getJsonArray("remainingLogins"))
														.put("loginsSucceeded", event.right().getValue().getInteger("loginsSucceeded"));
					Renders.renderJson(request, response);
                } else {
                    badRequest(request, "send failed");
                }
            }
        });
    }

	private boolean paramToBoolean(String param) {
		return "true".equalsIgnoreCase(param);
	}

	public void setMassMesssagingService(MassMessagingService massMessagingService) {
		this.massMessagingService = massMessagingService;
	}

}