package org.entcore.common.explorer.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IExplorerDb;
import org.entcore.common.user.UserInfos;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public abstract class ExplorerPluginResourceDb extends ExplorerPluginResource {
    protected final IExplorerDb explorerDb;
    protected ExplorerPluginResourceDb(final IExplorerPluginCommunication communication, final IExplorerDb crud) {
        super(communication);
        this.explorerDb = crud;
    }

    @Override
    protected String getIdForModel(final JsonObject json) {
        return explorerDb.getIdForModel(json);
    }

    @Override
    protected JsonObject setIdForModel(final JsonObject json, final String id) {
        explorerDb.setIdForModel(json,id);
        return json;
    }

    @Override
    protected UserInfos getCreatorForModel(final JsonObject json) { return this.explorerDb.getCreatorForModel(json); }

    @Override
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to) {
        this.explorerDb.fetchByDate(stream, from, to);
    }

    @Override
    protected Future<List<String>> doCreate(final UserInfos user, final List<JsonObject> sources, final boolean isCopy) {
        return this.explorerDb.createAll(user, sources);
    }

    @Override
    protected Future<List<Boolean>> doDelete(final UserInfos user, final List<String> ids) {
        return this.explorerDb.deleteById(ids);
    }

    public IExplorerDb getExplorerDb() {
        return explorerDb;
    }
}
