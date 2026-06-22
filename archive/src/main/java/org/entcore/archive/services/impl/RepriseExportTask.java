package org.entcore.archive.services.impl;

import io.vertx.core.Handler;
import org.entcore.archive.services.RepriseService;

public class RepriseExportTask implements Handler<Long> {

	final Boolean teacherPersonnelFirst;
	final RepriseService repriseService;

	public RepriseExportTask(Boolean teacherPersonnelFirst, RepriseService repriseService) {
		this.teacherPersonnelFirst = teacherPersonnelFirst;
		this.repriseService = repriseService;
	}

	@Override
	public void handle(Long event) {
		repriseService.launchExportForUsersFromOldPlatform(teacherPersonnelFirst.booleanValue());
	}
}
