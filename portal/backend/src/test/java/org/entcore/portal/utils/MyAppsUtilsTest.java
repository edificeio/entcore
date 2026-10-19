package org.entcore.portal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.function.Function;

/**
 * Tests the enrichment of the user's applications with "my-apps-config", which must match what the
 * portal front computes in useApplications().
 */
public class MyAppsUtilsTest {

	private static final Function<String, String> TRANSLATE = key -> "t:" + key;

	private static JsonObject app(final String name, final String prefix, final String address) {
		return new JsonObject()
				.put("name", name)
				.put("displayName", name.toLowerCase())
				.put("prefix", prefix)
				.put("address", address)
				.put("display", true)
				.put("isExternal", false);
	}

	private static JsonArray config() {
		return new JsonArray()
				.add(new JsonObject().put("name", "Messagerie").put("category", "communication").put("color", "orange"))
				.add(new JsonObject().put("name", "Searchengine").put("display", false))
				.add(new JsonObject().put("name", "library-info").put("category", "pedagogy")
						.put("color", "purple").put("isExternal", true))
				.add(new JsonObject().put("name", "Ma bibliotheque").put("category", "organisation"));
	}

	private static JsonObject build(final JsonObject app) {
		final JsonArray result = MyAppsUtils.buildMyApplications(new JsonArray().add(app), config(), TRANSLATE);
		assertEquals(1, result.size());
		return result.getJsonObject(0);
	}

	@Test
	public void configOverridesApplicationFields() {
		final JsonObject result = build(app("Searchengine", "/searchengine", "/searchengine"));
		assertEquals("config display wins over the application one", false, result.getBoolean("display"));
	}

	@Test
	public void configuredApplicationKeepsItsCategoryAndIsTranslatedFromPrefix() {
		final JsonObject result = build(app("Messagerie", "/conversation", "/conversation/conversation"));
		assertEquals("communication", result.getString("category"));
		assertEquals("orange", result.getString("color"));
		assertEquals("t:conversation", result.getString("appName"));
		assertEquals("displayName stays the raw key", "messagerie", result.getString("displayName"));
	}

	@Test
	public void unconfiguredApplicationIsAConnectorTranslatedFromDisplayName() {
		final JsonObject result = build(app("Moodle", null, "https://moodle.example.org"));
		assertEquals("connector", result.getString("category"));
		assertEquals("t:moodle", result.getString("appName"));
	}

	@Test
	public void libraryWithoutCategoryTakesLibraryInfoAndKeepsItsName() {
		final JsonObject result = build(app("Bibliotheque", null, "https://library.edifice.io/fr"));
		assertEquals("Bibliotheque", result.getString("name"));
		assertEquals("pedagogy", result.getString("category"));
		assertEquals("purple", result.getString("color"));
		assertTrue(result.getBoolean("isExternal"));
	}

	@Test
	public void libraryWithItsOwnCategoryIgnoresLibraryInfo() {
		final JsonObject result = build(app("Ma bibliotheque", null, "https://library.edifice.io/fr"));
		assertEquals("organisation", result.getString("category"));
		assertFalse(result.containsKey("color"));
		assertFalse(result.getBoolean("isExternal"));
	}

	@Test
	public void inputsAreNotModified() {
		final JsonArray apps = new JsonArray()
				.add(app("Messagerie", "/conversation", "/conversation/conversation"))
				.add(app("Bibliotheque", null, "https://library.edifice.io/fr"));
		final JsonArray appsBefore = apps.copy();
		final JsonArray config = config();
		final JsonArray configBefore = config.copy();

		MyAppsUtils.buildMyApplications(apps, config, TRANSLATE);

		assertEquals(appsBefore, apps);
		assertEquals(configBefore, config);
	}
}
