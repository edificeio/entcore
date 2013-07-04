package edu.one.core.infra;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;


/*
 * Dummy implementation
 */
public class I18n {

	private Logger log;
	private final static String messagesDir = "./i18n";
	private final static Locale defaultLocale = Locale.FRENCH;
	private Map<Locale, JsonObject> messages = new HashMap<>();

	public I18n(Container container, Vertx vertx) {
		try {
			log = container.logger();
			for(String path : vertx.fileSystem().readDirSync(messagesDir)) {
				Locale l = Locale.forLanguageTag(new File(path).getName().split("\\.")[0]);
				JsonObject jo = new JsonObject(vertx.fileSystem().readFileSync(path).toString());
				messages.put(l,jo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public String translate(String key, String acceptLanguage) {
		return translate(key, getLocale(acceptLanguage));
	}
	public String translate(String key, Locale locale) {
		JsonObject bundle = messages.get(locale) != null ? messages.get(locale) : messages.get(defaultLocale);
		if (bundle == null) {
			return key;
		}
		return bundle.getString(key) != null ? bundle.getString(key) : key;
	}

	public JsonObject load(String acceptLanguage) {
		Locale l = getLocale(acceptLanguage);
		JsonObject bundle = messages.get(l) != null ? messages.get(l) : messages.get(defaultLocale);
		if (bundle == null) {
			bundle = messages.get(defaultLocale);
		}
		return bundle;
	}

	/* Dummy implementation. Just use the first langage option ...
	 * Header example : "Accept-Language:fr,en-us;q=0.8,fr-fr;q=0.5,en;q=0.3"
	 */
	private Locale getLocale(String acceptLanguage) {
		String[] langs = acceptLanguage.split(",");
		return Locale.forLanguageTag(langs[0].split("-")[0]);
	}
}
