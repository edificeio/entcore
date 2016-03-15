package org.entcore.common.search;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

import java.util.List;

/**
 * Created by dbreyton on 23/02/2016.
 */
public interface SearchingEvents {
	void searchResource(List<String> appFilters, String userId, JsonArray groupIds, JsonArray searchWords, Integer page, Integer limit,
						JsonArray columnsHeader, Handler<Either<String, JsonArray>> handler);
}