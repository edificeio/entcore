package org.entcore.common.http.i18n;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Renders;

import java.util.Arrays;
import java.util.Locale;

import fr.wseduc.webutils.I18n;
import org.apache.commons.lang3.ArrayUtils;


public class I18nBusRequest {
    static final String ACCEPT_LANGUAGE = "acceptLanguage";
    static final String DOMAIN = "domain";
    static final String THEME = "theme";
    static final String KEYS = "keys";
    static final String ARGS = "args";
    final private JsonObject message;

    /** Private constructor, use fromMessage() and translate() instead to get an instance. */
    private I18nBusRequest(final JsonObject json) {
        message = json;
    }

    /** 
     * Generate a new I18nBusRequest instance
     * @param json the vertx bus message to decode.
     * @return  a new I18nBusRequest instance
     */
    static public I18nBusRequest fromMessage(final JsonObject json) {
        return new I18nBusRequest(json);
    }

    /**
     * Generate a new I18nBusRequest instance.
     * @param request
     * @param keys i18n keys to translate.
     * @param args args for parameterized translations.
     * @return  a new I18nBusRequest instance
     */
    static public I18nBusRequest translate(final HttpServerRequest request, String[] keys, String[] args) {
        final JsonObject m = new JsonObject();

        m.put(ACCEPT_LANGUAGE, I18n.acceptLanguage(request));
        m.put(DOMAIN, Renders.getHost(request));
        m.put(THEME, I18n.getTheme(request));
        m.put(KEYS, new JsonArray(Arrays.asList(ArrayUtils.nullToEmpty(keys))));
        m.put(ARGS, new JsonArray(Arrays.asList(ArrayUtils.nullToEmpty(args))));

        return new I18nBusRequest(m);
    }

    /** Accessor to the inner vertx bus message. */
    public JsonObject toMessage() {
        return message;
    }

    public boolean isValid() {
        return message!=null && 
            message.containsKey(ACCEPT_LANGUAGE) && 
            message.containsKey(DOMAIN) && 
            message.containsKey(THEME) && 
            message.containsKey(KEYS) && 
            message.containsKey(ARGS);
    }

    public String getAcceptLanguage() {
        return message.getString(ACCEPT_LANGUAGE);
    }
    public Locale getLocale() {
        return I18n.getLocale(message.getString(ACCEPT_LANGUAGE));
    }
    public String getDomain() {
        return message.getString(DOMAIN);
    }
    public String getTheme() {
        return message.getString(THEME);
    }
    public String[] getKeys() {
        final JsonArray value = message.getJsonArray(KEYS);
        return (value!=null) ? value.stream().toArray(String[]::new) : null;
    }
    public String[] getArgs() {
        final JsonArray value = message.getJsonArray(ARGS);
        return (value!=null) ? value.stream().toArray(String[]::new) : null;
    }
}
