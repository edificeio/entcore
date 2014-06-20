package org.entcore.archive;

import org.entcore.archive.controllers.ArchiveController;
import org.entcore.archive.filters.ArchiveFilter;
import org.entcore.common.http.BaseServer;

public class Archive extends BaseServer {

	@Override
	public void start() {
		setResourceProvider(new ArchiveFilter());
		super.start();
		addController(new ArchiveController());
	}

}
