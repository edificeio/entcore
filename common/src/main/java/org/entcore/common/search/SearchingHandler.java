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

package org.entcore.common.search;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

/**
 * Created by dbreyton on 23/02/2016.
 */
public class SearchingHandler implements Handler<Message<JsonObject>> {

	private SearchingEvents searchingEvents;
	private final EventBus eb;
	private static final Logger log = LoggerFactory.getLogger(SearchingHandler.class);

	public SearchingHandler(EventBus eb) {
		this.eb = eb;
		this.searchingEvents = new LogSearchingEvents();
	}

	public SearchingHandler(SearchingEvents searchingEvents, EventBus eb) {
		this.eb = eb;
		this.searchingEvents = searchingEvents;
	}

	@Override
	public void handle(final Message<JsonObject> message) {
		final JsonArray searchWords = message.body().getJsonArray("searchWords");
		final String userId = message.body().getString("userId", "");
		final Integer page =  message.body().getInteger("page", 0);
		final Integer limit =  message.body().getInteger("limit", 0);
		final String searchId = message.body().getString("searchId", "");
		final JsonArray groupIds = message.body().getJsonArray("groupIds", new fr.wseduc.webutils.collections.JsonArray());
		final JsonArray columnsHeader = message.body().getJsonArray("columnsHeader", new fr.wseduc.webutils.collections.JsonArray());
		final List<String> appFilters = message.body().getJsonArray("appFilters", new fr.wseduc.webutils.collections.JsonArray()).getList();
		final String locale = message.body().getString("locale", "fr");

		searchingEvents.searchResource(appFilters, userId, groupIds, searchWords, page, limit, columnsHeader, locale, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					final String address = "search." + searchId;
					final JsonObject message = new JsonObject().put("application", searchingEvents.getClass().getSimpleName());
					message.put("results", event.right().getValue());
					eb.send(address, message, new DeliveryOptions().setSendTimeout(5000l),
							new Handler<AsyncResult<Message<JsonObject>>>() {
								@Override
								public void handle(AsyncResult<Message<JsonObject>> res) {
									if (res != null && res.succeeded()) {
										if (!"ok".equals(res.result().body().getString("message"))) {
											log.error(res.result().body().getString("message"));
										}
									}
								}
							});
				} else {
					log.error("Failure of the research module : " + searchingEvents.getClass().getSimpleName() +
							"; message : " + event.left().getValue());
				}
			}
		});
	}

	public void setSearchingEvents(SearchingEvents searchingEvents) {
		this.searchingEvents = searchingEvents;
	}

}
