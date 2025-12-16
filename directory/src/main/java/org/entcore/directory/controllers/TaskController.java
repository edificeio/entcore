package org.entcore.directory.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.directory.Directory;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	@Post("/api/internal/delete-users")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void deleteUsers(final HttpServerRequest request) {
		eb.request(Directory.FEEDER, new JsonObject().put("action", "delete-users"))
				.onSuccess(e -> {
					log.info("Triggered deletion of pre-deleted users task");
					render(request, e.body(), 202);
				})
				.onFailure(error -> render(request, error.getMessage(), 400));
	}

	@Post("/api/internal/pre-delete-users")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void preDeleteUsers(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, jsonBody -> {
			String profile = jsonBody.getString("profile");
			Long delay = jsonBody.getLong("delay");
			if (profile == null || delay == null) {
				String message = "Missing profile or delay in pre delete users task request";
				log.warn(message);
				render(request, message, 400);
			} else {
				JsonObject message = new JsonObject()
						.put("action", "pre-delete-users")
						.put("profile", profile)
						.put("delay", delay);
				eb.request(Directory.FEEDER, message)
						.onSuccess(e -> {
							log.info("Triggered pre delete users task");
							render(request, e.body(), 202);
						})
						.onFailure(error -> render(request, error.getMessage(), 400));
			}
		});
	}

	@Post("/api/internal/trigger-import")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void triggerImport(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, jsonBody -> {
			String feederType = jsonBody.getString("feeder");
			JsonObject message = new JsonObject()
					.put("action", "import-with-auto-export")
					.put("feeder", feederType)
					.put("autoExport", jsonBody.getBoolean("auto-export", false))
					.put("autoExportDelay", jsonBody.getLong("auto-export-delay", 1800000l));
			eb.request(Directory.FEEDER, message)
					.onSuccess(e -> {
						log.info("Triggered import task for feeder: " + feederType);
						render(request, e.body(), 202);
					})
					.onFailure(error -> render(request, error.getMessage(), 400));
		} );
	}

	@Post("/api/internal/trigger-csv-import")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void triggerCsvImport(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, jsonBody -> {
			String csvPath = jsonBody.getString("path");
			JsonObject config = jsonBody.getJsonObject("config");
			if (csvPath == null || config == null) {
				String message = "Missing path or config in CSV import task request";
				log.warn(message);
				render(request, message, 400);
			} else {
				JsonObject message = new JsonObject()
						.put("action", "import-csv")
						.put("path", csvPath)
						.put("config", config);
				eb.request(Directory.FEEDER, message)
						.onSuccess(e -> {
							log.info("Triggered CSV import task");
							render(request, e.body(), 202);
						})
						.onFailure(error -> render(request, error.getMessage(), 400));
			}
		});
	}

	@Post("/api/internal/reinit-login")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void reinitLogin(final HttpServerRequest request) {
		eb.request(Directory.FEEDER, new JsonObject().put("action", "reinit-logins"))
				.onSuccess(e -> {
					log.info("Triggered reinit login task");
					render(request, e.body(), 202);
				})
				.onFailure(error -> render(request, error.getMessage(), 400));

	}

	@Post("/api/internal/erase-timetable-reports")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void eraseTimetableReports(final HttpServerRequest request) {
		eb.request(Directory.FEEDER, new JsonObject().put("action", "erase-timetable-reports"))
				.onSuccess(e -> {
					log.info("Triggered erase timetable reports task");
					render(request, e.body(), 202);
				})
				.onFailure(error -> render(request, error.getMessage(), 400));
	}

}
