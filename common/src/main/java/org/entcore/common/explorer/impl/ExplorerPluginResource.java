package org.entcore.common.explorer.impl;

import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.impl.ExplorerPlugin;

public abstract class ExplorerPluginResource extends ExplorerPlugin {
    protected ExplorerPluginResource(IExplorerPluginCommunication communication) {
        super(communication);
    }

    @Override
    protected boolean isForSearch() {
        return false;
    }
}