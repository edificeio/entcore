package org.entcore.directory.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.directory.Directory;
import org.entcore.directory.util.ImportReportSummary;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);
	/** Environment variable which enables the routes feeding the AAF import directories (integration tests only). */
	private static final String ALLOW_UPLOAD_AAF_FILES_ENV = "ALLOW_UPLOAD_AAF_FILES";
	/** Time in milliseconds a synchronous import is awaited when the request gives none. */
	private static final long DEFAULT_SYNC_IMPORT_TIMEOUT = 900000L;
	/** Longest time in milliseconds a synchronous import can be awaited. */
	private static final long MAX_SYNC_IMPORT_TIMEOUT = 5400000L;

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
			if (jsonBody.getBoolean("wait", false)) {
				triggerImportAndWait(request, feederType, jsonBody.getLong("timeout", DEFAULT_SYNC_IMPORT_TIMEOUT),
						jsonBody.getBoolean("waitPostImport", true));
				return;
			}
			JsonObject message = new JsonObject()
					.put("action", "import-with-auto-export")
					.put("feeder", feederType)
					.put("autoExport", jsonBody.getBoolean("auto-export", false))
					.put("autoExportDelay", jsonBody.getLong("auto-export-delay", 1800000l));
			eb.request(Directory.FEEDER, message, new DeliveryOptions().setLocalOnly(true))
					.onSuccess(e -> {
						log.info("Triggered import task for feeder: " + feederType);
						render(request, e.body(), 202);
					})
					.onFailure(error -> render(request, error.getMessage(), 400));
		} );
	}

	/**
	 * Triggers an import and answers once it is over, with a summary of its report (errors, ignored records...).
	 * Reserved to the platforms accepting the AAF files upload since the report contains personal data.
	 * @param feederType the type of feeder (AAF, AAF1D)
	 * @param timeout the time in milliseconds the end of the import is awaited (capped)
	 * @param waitPostImport whether the answer also waits for the end of the post import (duplicates merge, communication rules)
	 */
	private void triggerImportAndWait(final HttpServerRequest request, final String feederType, final long timeout,
			final boolean waitPostImport) {
		if (!isAafUploadAllowed()) {
			render(request, new JsonObject().put("error", "feature.disabled"), 403);
			return;
		}
		final JsonObject message = new JsonObject().put("action", "import").put("feeder", feederType)
				.put("waitPostImport", waitPostImport);
		final DeliveryOptions options = new DeliveryOptions().setLocalOnly(true)
				.setSendTimeout(Math.max(1000L, Math.min(timeout, MAX_SYNC_IMPORT_TIMEOUT)));
		eb.request(Directory.FEEDER, message, options)
				.onSuccess(reply -> {
					final JsonObject body = (JsonObject) reply.body();
					log.info("Import of feeder " + feederType + " done with status " + body.getString("status"));
					render(request, ImportReportSummary.summarize(body), "ok".equals(body.getString("status")) ? 200 : 500);
				})
				.onFailure(th -> {
					log.warn("Error while waiting for the import of feeder " + feederType, th);
					render(request, new JsonObject().put("error", th.getMessage()), statusOf(th, 504));
				});
	}

	/**
	 * Uploads AAF files into the import directory of a feeder (integration tests).
	 * @param request the HTTP request containing :
	 *                - subPath : the directory, under the import directory, receiving the files
	 *                - files : the content of each file, by file name
	 *                - feeder : AAF or AAF1D, to choose the import directory ("imports.&lt;feeder&gt;.files" of the feeder
	 *                configuration). Optional, the default import directory is used when missing.
	 */
	@Post("/api/internal/upload-aaf")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void uploadAAFFiles(final HttpServerRequest request) {
		if (!isAafUploadAllowed()) {
			render(request, new JsonObject().put("error", "feature.disabled"), 403);
			return;
		}
		RequestUtils.bodyToJson(request, uploadRequest -> {
			eb.request("feeder.upload-aaf", uploadRequest)
			.onSuccess(message -> render(request, new JsonObject().put("success", true)))
			.onFailure(th -> {
				log.warn("Error while uploading aaf files", th);
				render(request, new JsonObject().put("error", th.getMessage()), statusOf(th, 500));
			});
		});
	}

	/**
	 * Removes the directories created by {@link #uploadAAFFiles(HttpServerRequest)} (integration tests). A directory
	 * supplied by an academy is never removed and nothing is removed while an import is running (409).
	 * @param request the HTTP request containing :
	 *                - feeder : AAF or AAF1D, to choose the import directory. Optional, same rule as for the upload.
	 *                - subPath : the uploaded directory to remove. Optional, every uploaded directory is removed when missing.
	 */
	@Post("/api/internal/clean-aaf")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void cleanAAFFiles(final HttpServerRequest request) {
		if (!isAafUploadAllowed()) {
			render(request, new JsonObject().put("error", "feature.disabled"), 403);
			return;
		}
		RequestUtils.bodyToJson(request, cleanRequest -> {
			eb.request("feeder.clean-aaf", cleanRequest)
			.onSuccess(message -> render(request, (JsonObject) message.body()))
			.onFailure(th -> {
				log.warn("Error while cleaning aaf files", th);
				render(request, new JsonObject().put("error", th.getMessage()), statusOf(th, 500));
			});
		});
	}

	/** @return {@code true} iff the platform accepts to have its AAF import directories fed through the API. */
	private static boolean isAafUploadAllowed() {
		return "true".equals(System.getenv(ALLOW_UPLOAD_AAF_FILES_ENV));
	}

	/** @return The HTTP status carried by a failed bus reply, or {@code defaultStatus} when there is none. */
	private static int statusOf(final Throwable th, final int defaultStatus) {
		if (th instanceof ReplyException) {
			final ReplyException replyException = (ReplyException) th;
			if (replyException.failureType() == ReplyFailure.RECIPIENT_FAILURE
					&& replyException.failureCode() >= 400 && replyException.failureCode() < 600) {
				return replyException.failureCode();
			}
			if (replyException.failureType() == ReplyFailure.TIMEOUT) {
				return 504;
			}
		}
		return defaultStatus;
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
				eb.request(Directory.FEEDER, message, new DeliveryOptions().setLocalOnly(true))
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
