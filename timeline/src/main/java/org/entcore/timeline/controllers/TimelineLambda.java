/*
 * Copyright © "Open Digital Education", 2017
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
				JsonArray nestedArray = new fr.wseduc.webutils.collections.JsonArray((List<Object>) ctx.get("nestedTemplatesArray"));
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
