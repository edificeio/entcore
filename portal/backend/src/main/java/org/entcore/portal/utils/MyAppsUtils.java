package org.entcore.portal.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.function.Function;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

/**
 * Builds the user's application list enriched with "my-apps-config", as the portal front
 * does in useApplications(): the config entry overrides the application fields.
 */
public final class MyAppsUtils {

	/** Library applications without their own category take the "library-info" config entry. */
	private static final String LIBRARY_ADDRESS = "library.edifice.io";
	private static final String LIBRARY_CONFIG_NAME = "library-info";
	/** Category given to applications that have none after enrichment. */
	private static final String DEFAULT_CATEGORY = "connector";

	private MyAppsUtils() {
	}

	/**
	 * @param apps         the user's applications (session "apps")
	 * @param myAppsConfig the "my-apps-config" entries
	 * @param translate    i18n key to translated label
	 * @return new application objects, enriched, with a translated "appName"
	 */
	public static JsonArray buildMyApplications(JsonArray apps, JsonArray myAppsConfig, Function<String, String> translate) {
		final JsonArray result = new JsonArray();
		for (Object o : apps) {
			if (!(o instanceof JsonObject)) continue;
			final JsonObject app = ((JsonObject) o).copy();
			final Optional<JsonObject> appConfig = findAppConfig(myAppsConfig, app.getString("name", ""));
			appConfig.ifPresent(c -> app.mergeIn(c.copy()));

			final boolean isLibrary = app.getString("address", "").contains(LIBRARY_ADDRESS)
					&& isEmpty(appConfig.map(c -> c.getString("category")).orElse(null));
			if (isLibrary) {
				findAppConfig(myAppsConfig, LIBRARY_CONFIG_NAME).ifPresent(c -> {
					final JsonObject libraryConfig = c.copy();
					libraryConfig.remove("name");
					app.mergeIn(libraryConfig);
				});
			}

			final String prefix = app.getString("prefix");
			final String key = isNotEmpty(prefix) ? prefix.substring(1) : app.getString("displayName");
			app.put("appName", translate.apply(key));
			if (isEmpty(app.getString("category"))) {
				app.put("category", DEFAULT_CATEGORY);
			}
			result.add(app);
		}
		return result;
	}

	private static Optional<JsonObject> findAppConfig(JsonArray myAppsConfig, String name) {
		return myAppsConfig.stream()
				.filter(JsonObject.class::isInstance)
				.map(JsonObject.class::cast)
				.filter(c -> name.equals(c.getString("name")))
				.findFirst();
	}
}
