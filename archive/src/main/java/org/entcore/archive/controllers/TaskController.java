package org.entcore.archive.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.archive.services.impl.DeleteOldArchivesTask;
import org.entcore.archive.services.impl.RepriseExportTask;
import org.entcore.archive.services.impl.RepriseImportTask;

public class TaskController extends BaseController {

	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final DeleteOldArchivesTask deleteOldArchivesTask;
	private final RepriseExportTask repriseExportTask;
	private final RepriseImportTask repriseImportTask;

	public TaskController(DeleteOldArchivesTask deleteOldArchivesTask, RepriseExportTask repriseExportTask, RepriseImportTask repriseImportTask) {
		this.deleteOldArchivesTask = deleteOldArchivesTask;
		this.repriseExportTask = repriseExportTask;
		this.repriseImportTask = repriseImportTask;
	}

	@Post("api/internal/purge")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void purgeArchives(final HttpServerRequest request) {
		log.info("[Archive] Triggered purge task");
		deleteOldArchivesTask.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/reprise/export")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void repriseExport(final HttpServerRequest request) {
		log.info("[Archive] Triggered Reprise Export task");
		repriseExportTask.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/reprise/import")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void repriseImport(final HttpServerRequest request) {
		log.info("[Archive] Triggered Reprise Import task");
		repriseImportTask.handle(0L);
		render(request, null, 202);
	}
}
