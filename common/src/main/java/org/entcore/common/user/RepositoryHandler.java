/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.user;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.collections.SharedDataHelper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.edifice.storage.common.Storage;
import org.entcore.common.utils.Config;

import java.io.File;

import static io.vertx.core.Future.succeededFuture;


public class RepositoryHandler implements Handler<Message<JsonObject>> {

	private RepositoryEvents repositoryEvents;
	private final EventBus eb;
    private final Storage storage;
    private static final Logger log = LoggerFactory.getLogger(RepositoryHandler.class);
    private final long LOCK_RELEASE_TIMEOUT = 500L;
    private final long LOCK_RELEASE_DELAY = 2 * LOCK_RELEASE_TIMEOUT;

	public RepositoryHandler(EventBus eb, Storage storage) {
		this.eb = eb;
    this.storage = storage;
    this.repositoryEvents = new LogRepositoryEvents();
	}

	public RepositoryHandler(RepositoryEvents repositoryEvents, EventBus eb, Storage storage) {
		this.eb = eb;
		this.repositoryEvents = repositoryEvents;
    this.storage = storage;
  }

	@Override
	public void handle(Message<JsonObject> message)
	{
		String action = message.body().getString("action", "");

		String exportedBusAddress = "entcore.export";
		String importedBusAddress = "entcore.import";
		boolean forceImportAsDuplication = false;

		switch (action)
		{
			case "duplicate:export" :
				exportedBusAddress = "entcore.duplicate";
				// Fallthrough
			case "export" :
				final JsonArray exportApps = message.body().getJsonArray("apps");
				final JsonArray resourcesIds = message.body().getJsonArray("resourcesIds");
				final Boolean exportDocuments = message.body().getBoolean("exportDocuments", true);
				final Boolean exportSharedResources = message.body().getBoolean("exportSharedResources", true);
				String pathPrefix = Server.getPathPrefix(Config.getConf());

				if (!Utils.isEmpty(pathPrefix) && exportApps.contains(pathPrefix.substring(1)))
				{
                    final String exportId = message.body().getString("exportId", "");
                    final String userId = message.body().getString("userId", "");
                    final String finalBusAddress = exportedBusAddress;
                    final SharedDataHelper sharedData = SharedDataHelper.getInstance();
                    final String lockName = "export_" + exportId + "_" + pathPrefix;
                    sharedData.getLock(lockName, LOCK_RELEASE_TIMEOUT)
                        .onFailure(th -> log.debug("We could not get the export lock " + lockName+ " so it means that someone else is already treating the export", th))
                        .onSuccess(lock -> {
                            String path = message.body().getString("path", "");
                            final String locale = message.body().getString("locale", "fr");
                            final String host = message.body().getString("host", "");
                            final JsonArray groupIds = message.body().getJsonArray("groups", new fr.wseduc.webutils.collections.JsonArray());
                            final String appTitle = pathPrefix.replaceFirst("/", "");
                            try {
                                log.info("We got a lock to process export " + exportId + " for user " + userId + " for app " + pathPrefix);

                                repositoryEvents.exportResources(resourcesIds, exportDocuments.booleanValue(), exportSharedResources.booleanValue(),
                                        exportId, userId, groupIds, path, locale, host,
                                        isExported -> {
                                            final boolean ok = isExported.isOk();
                                            final Future<Void> future;
                                            if (ok) {
                                                final String finalPath = isExported.getExportPath();
												log.debug("Moving exported resources from fs to Storage '" + finalPath + "' for application '" + appTitle + "'");
                                                future = storage.moveFsDirectory(finalPath, finalPath);
                                            } else {
                                                future = succeededFuture();
                                            }
                                            future.onComplete(res -> {
												if(!res.succeeded()) {
													log.error("An error occurred while moving exported files to storage", res.cause());
												}
                                                sharedData.releaseLockAfterDelay(lock, LOCK_RELEASE_DELAY);
                                                final boolean exported = ok && res.succeeded();
                                                JsonObject responsePayload = new JsonObject()
                                                        .put("action", "exported")
                                                        .put("app", appTitle)
                                                        .put("status", (exported ? "ok" : "error"))
                                                        .put("exportId", exportId)
                                                        .put("locale", locale)
                                                        .put("host", host);
                                                eb.send(finalBusAddress, responsePayload);
                                            });
                                        });
                            } catch (Exception e) {
                                sharedData.releaseLockAfterDelay(lock, LOCK_RELEASE_DELAY);
                                log.error("An error occurred while treating an export " + message.body().encode(), e);
                                JsonObject responsePayload = new JsonObject()
                                        .put("action", "exported")
                                        .put("app", appTitle)
                                        .put("status", "error")
                                        .put("exportId", exportId)
                                        .put("locale", locale)
                                        .put("host", host);
                                eb.send(finalBusAddress, responsePayload);
                            }
                        });
                }
				break;
			case "duplicate:import" :
				importedBusAddress = "entcore.duplicate";
				forceImportAsDuplication = true;
				// Fallthrough
			case "reprise:import":
				if(forceImportAsDuplication == false)
					importedBusAddress = "entcore.reprise";
				//Fallthrough
			case "import" :
				final JsonObject importApps = message.body().getJsonObject("apps");
				final String appTitle = Server.getPathPrefix(Config.getConf());

				if (!Utils.isEmpty(appTitle) && importApps.containsKey(appTitle.substring(1)))
				{
                    final JsonObject body = message.body();
                    final String importId = body.getString("importId", "");
                    final String userId = body.getString("userId", "");
                    final boolean force = forceImportAsDuplication;
                    final String finalBusAddress = importedBusAddress;
                    final SharedDataHelper sharedData = SharedDataHelper.getInstance();
                    sharedData.getLock("import_" + importId + "_" + appTitle, LOCK_RELEASE_TIMEOUT)
                    .onFailure(th -> log.info("We could not get the lock so it means that someone else is already treating the import", th))
                    .onSuccess(lock -> {
                        log.info("We got a lock to process import " + importId + " for user " + userId + " for app " + appTitle);
                        try {
                            final String userLogin = body.getString("userLogin", "");
                            final String userName = body.getString("userName", "");
                            final String path = body.getString("path", "");
                            final String locale = body.getString("locale", "fr");
                            final String folderPath = path + File.separator + importApps.getJsonObject(appTitle.substring(1)).getString("folder");
                            final String host = body.getString("host", "");
                            storage.copyDirectoryToFs(folderPath, folderPath)
                                    .onSuccess(e -> {
                                        repositoryEvents.importResources(importId, userId, userLogin, userName, folderPath, locale, host, force, success -> {
                                            sharedData.releaseLockAfterDelay(lock, LOCK_RELEASE_DELAY);
                                            JsonObject imported = new JsonObject()
                                                    .put("action", "imported")
                                                    .put("importId", importId)
                                                    .put("app", appTitle.substring(1))
                                                    .put("rapport", success);
                                            eb.send(finalBusAddress, imported);
                                        });
                                    }).onFailure(th -> {
                                        sharedData.releaseLockAfterDelay(lock, LOCK_RELEASE_DELAY);
                                        log.error("Error while copying from FS", th);
                                        final JsonObject imported = new JsonObject()
                                                .put("action", "imported")
                                                .put("importId", importId)
                                                .put("app", appTitle.substring(1))
                                                .put("rapport", new JsonObject().put("status", "error"));
                                        eb.send(finalBusAddress, imported);
                                    });
                        } catch (Exception e) {
                            log.error("Error while processing the import", e);
                            sharedData.releaseLockAfterDelay(lock, LOCK_RELEASE_DELAY);
                            final JsonObject imported = new JsonObject()
                                    .put("action", "imported")
                                    .put("importId", importId)
                                    .put("app", appTitle.substring(1))
                                    .put("rapport", new JsonObject().put("status", "error"));
                            eb.send(finalBusAddress, imported);
                        }
                    });
				}
				break;
			case "delete-groups" :
				JsonArray groups = message.body().getJsonArray("old-groups", new fr.wseduc.webutils.collections.JsonArray());
				repositoryEvents.deleteGroups(groups);
				break;
			case "delete-users" :
				JsonArray users = message.body().getJsonArray("old-users", new fr.wseduc.webutils.collections.JsonArray());
				repositoryEvents.deleteUsers(users);
				break;
			case "users-classes-update" :
				JsonArray updates = message.body().getJsonArray("users-classes-update", new fr.wseduc.webutils.collections.JsonArray());
				repositoryEvents.usersClassesUpdated(updates);
				break;
			case "transition" :
				JsonObject structure = message.body().getJsonObject("structure");
				repositoryEvents.transition(structure);
				break;
			case "merge-users":
				JsonObject body = message.body();
				repositoryEvents.mergeUsers(body.getString("keepedUserId"), body.getString("deletedUserId"));
				break;
			case "remove-share-groups":
				JsonArray oldGroups = message.body().getJsonArray("old-groups", new fr.wseduc.webutils.collections.JsonArray());
				repositoryEvents.removeShareGroups(oldGroups);
				break;
			case "tenants-structures-update":
				final JsonArray addedTenantsStructures = message.body().getJsonArray("added", new JsonArray());
				final JsonArray deletedTenantsStructures = message.body().getJsonArray("deleted", new JsonArray());
				repositoryEvents.tenantsStructuresUpdated(addedTenantsStructures, deletedTenantsStructures);
				break;
			case "timetable-import":
				final String uai = message.body().getString("UAI");
				repositoryEvents.timetableImported(uai);
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	public void setRepositoryEvents(RepositoryEvents repositoryEvents) {
		this.repositoryEvents = repositoryEvents;
	}

}
