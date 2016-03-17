package org.entcore.common.search;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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
		final JsonArray searchWords = message.body().getArray("searchWords");
		final String userId = message.body().getString("userId", "");
		final Integer page =  message.body().getInteger("page", 0);
		final Integer limit =  message.body().getInteger("limit", 0);
		final String searchId = message.body().getString("searchId", "");
		final JsonArray groupIds = message.body().getArray("groupIds", new JsonArray());
		final JsonArray columnsHeader = message.body().getArray("columnsHeader", new JsonArray());
		final List<String> appFilters = message.body().getArray("appFilters", new JsonArray()).toList();
		final String locale = message.body().getString("locale", "fr");

		searchingEvents.searchResource(appFilters, userId, groupIds, searchWords, page, limit, columnsHeader, locale, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					final String address = "search." + searchId;
					final JsonObject message = new JsonObject().putString("application", searchingEvents.getClass().getSimpleName());
					message.putArray("results", event.right().getValue());
					eb.sendWithTimeout(address, message, 5000l,
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