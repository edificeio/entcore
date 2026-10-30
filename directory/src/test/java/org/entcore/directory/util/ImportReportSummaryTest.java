package org.entcore.directory.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ImportReportSummaryTest {

	@Test
	public void summaryKeepsWhatExplainsAFailureAndDropsTheUsers() {
		final JsonArray ignored = new JsonArray();
		for (int i = 0; i < ImportReportSummary.MAX_IGNORED_PER_FILE + 5; i++) {
			ignored.add(new JsonObject().put("reason", "invalid.birthDate").put("object", new JsonObject().put("externalId", "S" + i)));
		}
		final JsonObject reply = new JsonObject().put("status", "ok").put("result", new JsonObject()
				.put("errors", new JsonObject().put("error.Eleve", new JsonArray().add(new JsonObject().put("key", "k"))))
				.put("profiles", new JsonArray().add("Student"))
				.put("files", new JsonObject().put("Eleve", new JsonArray().add(new JsonObject()).add(new JsonObject())))
				.put("ignored", new JsonObject().put("Eleve", ignored))
				.put("usersExternalIds", new JsonArray().add("S1").add("S2")));

		final JsonObject result = ImportReportSummary.summarize(reply).getJsonObject("result");

		assertFalse(result.containsKey("usersExternalIds"));
		assertEquals(2, (int) result.getInteger("usersImported"));
		assertEquals(1, result.getJsonObject("errors").getJsonArray("error.Eleve").size());
		assertEquals(2, (int) result.getJsonObject("files").getInteger("Eleve"));
		assertEquals(ImportReportSummary.MAX_IGNORED_PER_FILE, result.getJsonObject("ignored").getJsonArray("Eleve").size());
		assertEquals(5, (int) result.getJsonObject("ignoredNotReturned").getInteger("Eleve"));
	}

	@Test
	public void summaryOfAFailedImportKeepsTheMessage() {
		final JsonObject summary = ImportReportSummary.summarize(new JsonObject().put("status", "error").put("message", "import.error"));
		assertEquals("error", summary.getString("status"));
		assertEquals("import.error", summary.getString("message"));
		assertNull(summary.getJsonObject("result"));
	}
}
