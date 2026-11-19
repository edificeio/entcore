package org.entcore.feeder.csv;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/** Deterministic CSV externalId: the vectors shared with referential (csv/csv-external-id-vectors.json). */
public class CsvExternalIdsTest {

	@Test
	public void sharedVectors() throws IOException {
		final JsonArray vectors = new JsonObject(resource("csv/csv-external-id-vectors.json")).getJsonArray("vectors");
		final Map<String, CsvExternalIds> files = new HashMap<>();
		for (Object o : vectors) {
			final JsonObject vector = (JsonObject) o;
			final JsonObject user = new JsonObject().put("lastName", vector.getString("lastName"))
					.put("firstName", vector.getString("firstName"));
			if (!vector.getString("birthDate").isEmpty()) {
				user.put("birthDate", vector.getString("birthDate"));
			}
			final String structure = vector.getString("structureExternalId");
			final String profile = vector.getString("profile");
			files.computeIfAbsent(structure + '|' + profile, k -> new CsvExternalIds())
					.generateIfAbsent(user, structure, profile);
			assertEquals(vector.getString("comment"), vector.getString("externalId"), user.getString("externalId"));
		}
	}

	@Test
	public void givenExternalIdIsKept() {
		final JsonObject user = new JsonObject().put("externalId", "E1").put("lastName", "A").put("firstName", "B");
		new CsvExternalIds().generateIfAbsent(user, "S1", "Student");
		assertEquals("E1", user.getString("externalId"));
	}

	@Test
	public void sameFileGivesSameExternalIds() {
		final JsonObject first = new JsonObject().put("lastName", "Martin").put("firstName", "Léa");
		final JsonObject second = first.copy();
		new CsvExternalIds().generateIfAbsent(first, "S1", "Teacher");
		new CsvExternalIds().generateIfAbsent(second, "S1", "Teacher");
		assertEquals("two imports of the same file", first.getString("externalId"), second.getString("externalId"));
	}

	private static String resource(String name) throws IOException {
		try (InputStream in = CsvExternalIdsTest.class.getClassLoader().getResourceAsStream(name);
				Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
			return scanner.next();
		}
	}
}
