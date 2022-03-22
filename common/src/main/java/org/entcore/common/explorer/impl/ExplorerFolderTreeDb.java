package org.entcore.common.explorer.impl;

import io.vertx.core.json.JsonObject;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerDb;
import org.entcore.common.user.UserInfos;

import java.util.Date;
import java.util.Optional;

public abstract class ExplorerFolderTreeDb extends ExplorerFolderTree{
    protected final IExplorerDb explorerDb;

    protected ExplorerFolderTreeDb(final ExplorerPlugin parent, final IExplorerDb crud) {
        super(parent);
        this.explorerDb = crud;
    }

    @Override
    protected String getFolderId(final JsonObject source) {
        return this.explorerDb.getIdForModel(source);
    }

    @Override
    protected UserInfos getCreatorForModel(final JsonObject json) {
        return this.explorerDb.getCreatorForModel(json);
    }

    @Override
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to) {
        this.explorerDb.fetchByDate(stream, from, to);
    }
}
