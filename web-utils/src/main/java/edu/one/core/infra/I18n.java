package edu.one.core.infra;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;


/*
 * Dummy implementation
 */
public class I18n {

	private Logger log;
	private final static String messagesDir = "./i18n";
	private final static Locale defaultLocale = Locale.FRENCH;
	private Map<Locale, JsonObject> messages = new HashMap<>();

	private I18n(){}

	private static class I18nHolder {
		private static final I18n instance = new I18n();
	}

	public static I18n getInstance() {
		return I18nHolder.instance;
	}

	public void init(Container container, Vertx vertx) {
		try {
			log = container.logger();
			for(String path : vertx.fileSystem().readDirSync(messagesDir)) {
				Locale l = Locale.forLanguageTag(new File(path).getName().split("\\.")[0]);
				JsonObject jo = new JsonObject(vertx.fileSystem().readFileSync(path).toString());
				messages.put(l,jo);
			}

			for (Locale l : messages.keySet()) {
				InputStream in = this.getClass().
						getResourceAsStream("/i18n/" + l.getLanguage() + ".utils.json");
				if (in != null) {
					String i18n = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
					JsonObject jo = new JsonObject(i18n);
					JsonObject j = messages.get(l);
					if (j == null) {
						messages.put(l,jo);
					} else {
						messages.put(l, jo.mergeIn(j));
					}
				}
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
