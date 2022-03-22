package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IExplorerFolderTree {

    Future<JsonObject> reindex(final Optional<Long> from, final Optional<Long> to);
}
