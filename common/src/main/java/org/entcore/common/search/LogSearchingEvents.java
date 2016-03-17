package org.entcore.common.search;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;

/**
 * Created by dbreyton on 23/02/2016.
 */
public class LogSearchingEvents implements SearchingEvents {
	private static final Logger log = LoggerFactory.getLogger(LogSearchingEvents.class);

	@Override
	public void searchResource(List<String> appFilters, String userId, JsonArray groupIds, JsonArray searchWords,
							   Integer page, Integer limit, JsonArray columnsHeader, String locale,
							   Handler<Either<String, JsonArray>> handler) {
		log.info("Search : " + userId + ", words attributes : " + searchWords.toString() +
				", requested engine : " + appFilters.toString());
	}
}