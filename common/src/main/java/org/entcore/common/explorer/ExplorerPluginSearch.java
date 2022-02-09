package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public abstract class ExplorerPluginSearch extends ExplorerPlugin {

    protected ExplorerPluginSearch(final IExplorerPluginCommunication communication) {
        super(communication);
    }

    protected final Future<List<String>> doCreate(final List<JsonObject> sources, final boolean isCopy){
        return Future.failedFuture("doDelete is not implemented for ExplorerSearchPlugin");
    }

    protected final Future<List<Boolean>> doDelete(final List<String> ids){
        return Future.failedFuture("doDelete is not implemented for ExplorerSearchPlugin");
    }

    @Override
    protected boolean isForSearch() {
        return true;
    }
}
