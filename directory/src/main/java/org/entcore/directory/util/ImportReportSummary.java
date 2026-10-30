package org.entcore.directory.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Condensed view of the report returned by the feeder at the end of an import : what a test needs to explain a
 * failure (errors, ignored records and their reason, number of imported records per file) without the list of
 * every imported user, which can be huge.
 */
public final class ImportReportSummary {
	/** Maximum number of ignored records returned for a file. */
	public static final int MAX_IGNORED_PER_FILE = 50;

	private ImportReportSummary() {}

	/**
	 * @param feederReply The body of the reply of the feeder to an import ({@code {status, result|message}})
	 * @return The same reply with a report holding only {@code errors}, {@code ignored} (truncated), {@code files}
	 * (counts), {@code usersImported} (number of users read by the import) and {@code profiles}
	 */
	public static JsonObject summarize(final JsonObject feederReply) {
		final JsonObject summary = new JsonObject().put("status", feederReply.getString("status"));
		if (feederReply.containsKey("message")) {
			summary.put("message", feederReply.getValue("message"));
		}
		final JsonObject report = feederReply.getJsonObject("result");
		if (report == null) {
			return summary;
		}
		final JsonObject result = new JsonObject()
				.put("errors", report.getJsonObject("errors", new JsonObject()))
				.put("profiles", report.getJsonArray("profiles", new JsonArray()));
		final JsonObject files = new JsonObject();
		final JsonObject imported = report.getJsonObject("files", new JsonObject());
		for (String file : imported.fieldNames()) {
			final Object content = imported.getValue(file);
			files.put(file, content instanceof JsonArray ? ((JsonArray) content).size() : 0);
		}
		result.put("files", files);
		// the AAF imports do not fill "files" : the number of users they wrote is the size of the list of their external ids
		final JsonArray usersExternalIds = report.getJsonArray("usersExternalIds");
		if (usersExternalIds != null) {
			result.put("usersImported", usersExternalIds.size());
		}
		final JsonObject ignored = new JsonObject();
		final JsonObject truncated = new JsonObject();
		final JsonObject allIgnored = report.getJsonObject("ignored", new JsonObject());
		for (String file : allIgnored.fieldNames()) {
			final JsonArray records = allIgnored.getJsonArray(file, new JsonArray());
			final JsonArray kept = new JsonArray();
			for (int i = 0; i < Math.min(records.size(), MAX_IGNORED_PER_FILE); i++) {
				kept.add(records.getValue(i));
			}
			ignored.put(file, kept);
			if (records.size() > kept.size()) {
				truncated.put(file, records.size() - kept.size());
			}
		}
		result.put("ignored", ignored);
		if (!truncated.isEmpty()) {
			result.put("ignoredNotReturned", truncated);
		}
		return summary.put("result", result);
	}
}
