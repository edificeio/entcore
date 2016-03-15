package org.entcore.common.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

import java.util.List;

/**
 * Created by dbreyton on 23/02/2016.
 */
public interface SearchService {
    void search(String userId, List<String> groupIds, List<String> returnFields, List<String> searchWords,
                List<String> searchFields, Integer page, Integer limit, Handler<Either<String, JsonArray>> handler);
}
