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

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.IExplorerPluginClient;
import org.entcore.common.explorer.to.ExplorerReindexResourcesRequest;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.user.RepositoryEvents;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

/**
 * This implementation of the RepositoryEvents follows the proxy pattern.
 * Any app using BaseServer.setRepositoryEvents() and the explorer feature should use it.
 */
public class ExplorerRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(ExplorerRepositoryEvents.class);

	/** Proxyfied events repository that will be used to import/export resources.*/
	private final RepositoryEvents realRepositoryEvents;
	/**
	 * It will be used after an import to select the right {@code pluginClients} corresponding to the types of resources
	 * which were imported.
	 */
	private final Map<String, IExplorerPluginClient> pluginClientsForApp;

	/**
	 *
	 * @param realRepositoryEvents The repository event to proxyfy
	 * @param pluginClientsForApp Mapping table to use after an import to route imported resources reindexation queries
	 *                            to their matching {@code pluginClients}.
	 *                            <u>Example:</u><br />
	 *                            For blog, we should have an association :
	 *                            <ul>
	 *                            <li>"blogs" -> blogPluginClient</li>
  	 *                            <li>"posts" -> postsPluginClient</li>
	 *                            </ul>
	 *                            because blogs and posts are stored in collections named "blogs" and "posts".
	 */
	public ExplorerRepositoryEvents(final RepositoryEvents realRepositoryEvents,
									final Map<String, IExplorerPluginClient> pluginClientsForApp) {
		this.realRepositoryEvents = realRepositoryEvents;
		this.pluginClientsForApp = pluginClientsForApp;
	}

	@Override
	public void exportResources(boolean exportDocuments, boolean exportSharedResources, String exportId, String userId, JsonArray groups, String exportPath,
						 String locale, String host, Handler<Boolean> handler) {
		realRepositoryEvents.exportResources(exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
	}

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
				indexResourcesAfterImport(userId, userLogin, userName, jo, handler);
			}
		});
	}

	/**
	 * For the resources imported :
	 * <ol>
	 *     <li>regroup them by type (blogs, subjects, posts, etc.)</li>
	 *     <li>get the pluginClient corresponding to the type</li>
	 *     <li>if the pluginClient exists, send a reindexation request with the ids of the requests</li>
	 * </ol>
	 * After reindex requests were sent (even if they fail) call the downstream process
	 * @param userId User id
	 * @param userLogin User login
	 * @param userName User name
	 * @param reindexationReport Report returned by a call to {@link ExplorerRepositoryEvents#importResources(String, String, String, String, String, String, String, boolean, Handler)}
	 * @param handler downstream process which will be called after reindexation queries were emitted
	 */
	private void indexResourcesAfterImport(final String userId, final String userLogin, final String userName,
											 final JsonObject reindexationReport, final Handler<JsonObject> handler) {
		try {
			// resourcesIdsMap associates :
			// - key : the table or collections in which the resources were imported
			// - value : a Map associating the id of the resource we wanted to import and the actual id with which it
			//           was actually imported
			if (pluginClientsForApp != null && reindexationReport != null && reindexationReport.containsKey("resourcesIdsMap")) {
				final JsonObject resourcesIdsMap = reindexationReport.getJsonObject("resourcesIdsMap");
				resourcesIdsMap.stream().filter(e -> pluginClientsForApp.containsKey(e.getKey())).forEach(e -> {
					final String collection = e.getKey();
					final JsonObject idsMapForApp = (JsonObject) e.getValue();
					if (idsMapForApp != null && !idsMapForApp.isEmpty()) {
						final Set<String> idsToReindex = idsMapForApp.stream().map(i -> (String) i.getValue()).collect(Collectors.toSet());
						if (idsToReindex.isEmpty()) {
							log.info("Nothing to reindex in EUR");
						} else {
							log.info("Reindexing " + idsToReindex.size() + " resources in EUR of type " + collection);
							final IExplorerPluginClient pluginClient = pluginClientsForApp.get(collection);
							final UserInfos userInfos = new UserInfos();
							userInfos.setUserId(userId);
							userInfos.setLogin(userLogin);
							userInfos.setFirstName(userName);
							pluginClient.reindex(userInfos, new ExplorerReindexResourcesRequest(idsToReindex));
						}
					}
				});
			} else {
				log.debug("Nothing to do as no plugin client is defined (" + (pluginClientsForApp == null) +
						") and/or resourcesIdsMap is not set (" + (reindexationReport == null || reindexationReport.containsKey("resourcesIdsMap")) + ")");
			}
		} catch (Exception e) {
			log.error("An error occurred while trying to index imported content " + (reindexationReport == null ? "" : reindexationReport.encodePrettily()), e);
		}
		handler.handle(reindexationReport);
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		this.deleteGroups(groups, (e)-> {});
	}

	@Override
	public void deleteGroups(JsonArray groups, Handler<List<DeletedResource>> handler) {
		realRepositoryEvents.deleteGroups(groups, deleted -> {
			handler.handle(deleted);
			final Set<String> ids = deleted.stream().map(e -> e.id).collect(Collectors.toSet());
			plugin.onReindexByIdAction(Optional.empty(), ids);
		});
	}

	@Override
	public void deleteUsers(JsonArray users) {
		this.deleteUsers(users, (e) -> {});
	}

	@Override
	public void deleteUsers(JsonArray users, Handler<List<DeletedResource>> handler) {
		realRepositoryEvents.deleteUsers(users, deleted -> {
			handler.handle(deleted);
			final Set<String> ids = deleted.stream().map(e -> e.id).collect(Collectors.toSet());
			plugin.onReindexByIdAction(Optional.empty(), ids);
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
	@Override
	public  void timetableImported(String uai) {
		realRepositoryEvents.timetableImported(uai);
	}

	@Override
	public Optional<String> getMainRepositoryName() {
		return realRepositoryEvents.getMainRepositoryName();
	}
}
