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

	/**
	 * Endpoint to trigger pre delete users task for specific profile, with custom delay.
	 * @param request the HTTP request containing :
	 *                - profile : the user profile for which the pre delete users task should be triggered (e.g., "Student", "Teacher", etc.)
	 *                - delay : the delay in milliseconds before the pre delete users task is executed for the specified profile (e.g., 2592000000 for 30 days)
	 *                Example of request body:   { "profile": "Student", "delay": 2592000000 }
	 *                This will trigger the pre delete users task for Student profile and with a delay of 30 days.
	 */
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

	/**
	 * Endpoint to trigger import task for specific feeder, with options for auto export.
	 * @param request the HTTP request containing :
	 *                - feeder : the type of feeder (AAF, AAF1D, CSV)
	 *                - auto-export : whether to trigger auto export after import, default false
	 *                - auto-export-delay : the delay in milliseconds before the auto export is triggered after import, default 30 minutes (1800000 milliseconds)
	 *                Example of request body:   { "feeder": "AAF", "auto-export": true, "auto-export-delay": 1800000 }
	 *                This will trigger AAF import, with auto export enabled and a delay of 1 hour (3600000 milliseconds) before the auto export is triggered.
	 */
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

	/**
	 * Endpoint to trigger CSV import task with specified path and configuration, used for Educasud project
	 * @param request the HTTP request containing
	 *                - path : the path to the CSV file to be imported (e.g., "/path/to/csv/file.zip")
	 *                - config : a JSON object containing configuration options for the CSV import :
	 *                  {
	 *                    "namePattern" : ".*?/Educasud_[0-9]{8}_[0-9]{7}[A-Z]_(.*?).zip",
	 *                    "profiles" : {
	 *                      "_Eleves_":"Student",
	 *                      "_Enseignants_":"Teacher",
	 * 					    "_Personnels_":"Personnel",
	 *                      "_Parents_":"Relative"
	 *                    },
	 *                    "preDelete" : true
	 *                  }
	 */
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
