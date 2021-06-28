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
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.Config;

import java.io.File;


public class RepositoryHandler implements Handler<Message<JsonObject>> {

	private RepositoryEvents repositoryEvents;
	private final EventBus eb;

	public RepositoryHandler(EventBus eb) {
		this.eb = eb;
		this.repositoryEvents = new LogRepositoryEvents();
	}

	public RepositoryHandler(RepositoryEvents repositoryEvents, EventBus eb) {
		this.eb = eb;
		this.repositoryEvents = repositoryEvents;
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
				String title = Server.getPathPrefix(Config.getConf());

				if (!Utils.isEmpty(title) && exportApps.contains(title.substring(1)))
				{
					final String exportId = message.body().getString("exportId", "");
					String userId = message.body().getString("userId", "");
					String path = message.body().getString("path", "");
					final String locale = message.body().getString("locale", "fr");
					final String host = message.body().getString("host", "");
					JsonArray groupIds = message.body().getJsonArray("groups", new fr.wseduc.webutils.collections.JsonArray());

					String finalBusAddress = exportedBusAddress;
					repositoryEvents.exportResources(resourcesIds, exportDocuments.booleanValue(), exportSharedResources.booleanValue(),
														exportId, userId, groupIds, path, locale, host,
					new Handler<Boolean>()
					{
						@Override
						public void handle(Boolean isExported)
						{
							JsonObject exported = new JsonObject()
									.put("action", "exported")
									.put("status", (isExported ? "ok" : "error"))
									.put("exportId", exportId)
									.put("locale", locale)
									.put("host", host);
							eb.publish(finalBusAddress, exported);
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
					final String importId = message.body().getString("importId", "");
					String userId = message.body().getString("userId", "");
					String userLogin = message.body().getString("userLogin", "");
					String userName = message.body().getString("userName", "");
					String path = message.body().getString("path", "");
					String locale = message.body().getString("locale", "fr");
					String folderPath = path + File.separator + importApps.getJsonObject(appTitle.substring(1)).getString("folder");
					String host = message.body().getString("host", "");

					String finalBusAddress = importedBusAddress;
					repositoryEvents.importResources(importId, userId, userLogin, userName, folderPath, locale, host, forceImportAsDuplication, success -> {
							JsonObject imported = new JsonObject()
									.put("action", "imported")
									.put("importId", importId)
									.put("app", appTitle.substring(1))
									.put("rapport", success);
							eb.publish(finalBusAddress, imported);
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
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	public void setRepositoryEvents(RepositoryEvents repositoryEvents) {
		this.repositoryEvents = repositoryEvents;
	}

}
