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

package org.entcore.common.explorer.impl;

import java.util.List;

import org.entcore.common.user.RepositoryEvents;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This implementation of the RepositoryEvents follows the proxy pattern.
 * Any app using BaseServer.setRepositoryEvents() and the explorer feature should use it.
 */
public class ExplorerRepositoryEvents implements RepositoryEvents {

	private RepositoryEvents realRepositoryEvents;
	private ExplorerPlugin plugin;

	public ExplorerRepositoryEvents(final RepositoryEvents realRepositoryEvents) {
		this.realRepositoryEvents = realRepositoryEvents;
	}

	public ExplorerRepositoryEvents setPlugin(ExplorerPlugin plugin) {
		this.plugin = plugin;
		return this;
	}

	@Override
	public void exportResources(boolean exportDocuments, boolean exportSharedResources, String exportId, String userId, JsonArray groups, String exportPath,
						 String locale, String host, Handler<Boolean> handler) {
		realRepositoryEvents.exportResources(exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
	};

	@Override
	public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId,
						 JsonArray groups, String exportPath, String locale, String host, Handler<Boolean> handler) {
		realRepositoryEvents.exportResources(resourcesIds, exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
	}

	@Override
	public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
						 String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {
		realRepositoryEvents.importResources(importId, userId, userLogin, userName, importPath, locale, host, forceImportAsDuplication, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject jo) {
				handler.handle(jo);
				if( plugin != null ) {
					// TODO ya quoi dans le JsonObject ? Est-ce bien ce qui est attendu par notifyUpsert() ??
					plugin.notifyUpsert(/*FIXME*/ null, jo);
				}
			}
		});
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		realRepositoryEvents.deleteGroups(groups, new Handler<List<JsonObject>>() {
			@Override
			public void handle(List<JsonObject> jos) {
				if( plugin != null ) {
					plugin.notifyUpsert(/*FIXME*/ null, jos);
				}
			}
		});
	}

	@Override
	public void deleteUsers(JsonArray users) {
		realRepositoryEvents.deleteUsers(users, new Handler<List<JsonObject>>() {
			@Override
			public void handle(List<JsonObject> jos) {
				if( plugin != null ) {
					plugin.notifyUpsert(/*FIXME*/ null, jos);
				}
			}
		});
	}

	@Override
	public void usersClassesUpdated(JsonArray updates) {
		realRepositoryEvents.usersClassesUpdated(updates);
	}

	@Override
	public void transition(JsonObject structure) {
		realRepositoryEvents.transition(structure);
	}

	@Override
	public void mergeUsers(String keepedUserId, String deletedUserId) {
		realRepositoryEvents.mergeUsers(keepedUserId, deletedUserId);
	}

	@Override
	public void removeShareGroups(JsonArray oldGroups) {
		realRepositoryEvents.removeShareGroups(oldGroups);
	}

	@Override
	public void tenantsStructuresUpdated(JsonArray addedTenantsStructures, JsonArray deletedTenantsStructures) {
		realRepositoryEvents.tenantsStructuresUpdated(addedTenantsStructures, deletedTenantsStructures);
	}
}
