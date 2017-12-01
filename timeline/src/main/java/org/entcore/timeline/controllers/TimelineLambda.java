/*
 * Copyright © WebServices pour l'Éducation, 2017
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.timeline.controllers;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public final class TimelineLambda {

	private static final Logger log = LoggerFactory.getLogger(TimelineLambda.class);

	private TimelineLambda() {}

	public static void setLambdaTemplateRequest(final HttpServerRequest request, final Map<String, Object> ctx,
			final LocalMap<String, String> eventsI18n, final HashMap<String, JsonObject> lazyEventsI18n) {

		ctx.put("i18n", new Mustache.Lambda() {
			@Override
			public void execute(Template.Fragment frag, Writer out) throws IOException {
				String key = frag.execute();
				String language = Utils.getOrElse(I18n.acceptLanguage(request), "fr", false);

				JsonObject timelineI18n;
				if (!lazyEventsI18n.containsKey(language)) {
					String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
					i18n = i18n != null ? i18n : "}";
					try {
						timelineI18n = new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}");
						lazyEventsI18n.put(language, timelineI18n);
					} catch (DecodeException de) {
						timelineI18n = new JsonObject();
						log.error("Bad json : " + "{" + i18n.substring(0, i18n.length() - 1) + "}", de);
					}
				} else {
					timelineI18n = lazyEventsI18n.get(language);
				}

				String translatedContents = I18n.getInstance().translate(key, Renders.getHost(request), language);
				if (translatedContents.equals(key)) {
					translatedContents = timelineI18n.getString(key, key);
				}
				Mustache.compiler().compile(translatedContents).execute(ctx, out);
			}
		});

		ctx.put("host", new Mustache.Lambda() {
			@Override
			public void execute(Template.Fragment frag, Writer out) throws IOException{
				String contents = frag.execute();
				if(contents.matches("^(http://|https://).*")){
					out.write(contents);
				} else {
					String host = Renders.getScheme(request) + "://" + Renders.getHost(request);
					out.write(host + contents);
				}
			}
		});

		ctx.put("nested", new Mustache.Lambda() {
			public void execute(Template.Fragment frag, Writer out) throws IOException {
				String nestedTemplateName = frag.execute();
				String nestedTemplate = (String) ctx.get(nestedTemplateName);
				if(nestedTemplate != null)
					Mustache.compiler().compile(nestedTemplate).execute(ctx, out);
			}
		});

		ctx.put("nestedArray", new Mustache.Lambda() {
			public void execute(Template.Fragment frag, Writer out) throws IOException {
				String nestedTemplatePos = frag.execute();
				JsonArray nestedArray = new JsonArray((List<Object>) ctx.get("nestedTemplatesArray"));
				try {
					JsonObject nestedTemplate = nestedArray.getJsonObject(Integer.parseInt(nestedTemplatePos) - 1);
					ctx.putAll(nestedTemplate.getJsonObject("params", new JsonObject()).getMap());
					Mustache.compiler()
							.compile(nestedTemplate.getString("template", ""))
							.execute(ctx, out);
				} catch(NumberFormatException e) {
					log.error("Mustache compiler error while parsing a nested template array lambda.");
				}
			}
		});
	}

}
