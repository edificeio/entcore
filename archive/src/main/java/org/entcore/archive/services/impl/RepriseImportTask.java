package org.entcore.archive.services.impl;

import io.vertx.core.Handler;
import org.entcore.archive.services.RepriseService;

public class RepriseImportTask implements Handler<Long> {

	final Boolean teacherPersonnelFirst;
	final RepriseService repriseService;

	public RepriseImportTask(Boolean teacherPersonnelFirst, RepriseService repriseService) {
		this.teacherPersonnelFirst = teacherPersonnelFirst;
		this.repriseService = repriseService;
	}

	@Override
	public void handle(Long event) {
		repriseService.launchImportForUsersFromOldPlatform(teacherPersonnelFirst.booleanValue());
	}
}
