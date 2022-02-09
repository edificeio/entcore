package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IExplorerResourceCrud {
    String getIdForModel(final JsonObject json);

    void setIdForModel(final JsonObject json, final String id);

    UserInfos getCreatorForModel(final JsonObject json);

    void fetchByDate(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to);

    Future<List<JsonObject>> getByIds(final Set<String> ids);

    Future<List<String>> createAll(final UserInfos user, final List<JsonObject> sources);

    Future<List<Boolean>> deleteById(final List<String> ids);
}
