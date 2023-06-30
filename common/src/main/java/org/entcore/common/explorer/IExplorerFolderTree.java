package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IExplorerFolderTree {
    String FOLDER_TYPE = "folder";

    Future<JsonObject> reindex(final Date from, final Date to);
}
