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

package org.entcore.common.mongodb;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.share.impl.MongoDbShareService;
import io.vertx.core.Vertx;
import org.vertx.java.core.http.RouteMatcher;

import java.util.List;
import java.util.Map;

public abstract class MongoDbControllerHelper extends ControllerHelper {

	private final Map<String, List<String>> groupedActions;
	private final String sharedCollection;
	protected MongoDb mongo;

	public MongoDbControllerHelper(String collection) {
		this(collection, null);
	}

	public MongoDbControllerHelper(String collection, Map<String, List<String>> groupedActions) {
		if (collection == null || collection.trim().isEmpty()) {
			log.error("MongoDB collection name must be not empty.");
			throw new IllegalArgumentException(
					"MongoDB collection name must be not empty.");
		}
		this.sharedCollection = collection;
		this.groupedActions = groupedActions;
	}

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		this.mongo = MongoDb.getInstance();
		setShareService(new MongoDbShareService(eb, this.mongo,
				sharedCollection, securedActions, groupedActions));
		setCrudService(new MongoDbCrudService(sharedCollection));
	}

}
