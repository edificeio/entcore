package org.entcore.common.explorer;

public abstract class ExplorerPluginResource extends ExplorerPlugin {
    protected ExplorerPluginResource(IExplorerPluginCommunication communication) {
        super(communication);
    }

    @Override
    protected boolean isForSearch() {
        return false;
    }
}