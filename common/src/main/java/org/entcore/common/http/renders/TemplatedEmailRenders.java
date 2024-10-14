/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.http.renders;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.utils.I18nUtils;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

/**
 * This class retrieves overloaded i18n from Timeline because it contains variables for email templating.
 * Those i18n will be available through the {{theme}} mustache directive.
 * {{theme}} directives may have inner {{host}}
 */
public abstract class TemplatedEmailRenders extends Renders {
	protected Map<String, JsonObject> requestThemeKV = null;

	protected TemplatedEmailRenders(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
		super(vertx, config);
	}

    protected void processEmailTemplate(
        final HttpServerRequest request, 
        JsonObject parameters, 
        String template, 
        boolean reader, 
        final Handler<String> handler
        ) {
		processEmailTemplate(request, parameters, template, reader).onSuccess(handler);
    }

	protected Future<String> processEmailTemplate(
			final HttpServerRequest request,
			JsonObject parameters,
			String template,
			boolean reader
	) {
		Promise<String> promise = Promise.promise();
		// From now until the end of the template processing, code execution cannot be async.
		// So initialize requestedThemeKV here and now.
		loadThemeKVs(request)
				.onSuccess( themeKV -> {
					this.requestThemeKV = themeKV;
					if(reader){
						final StringReader templateReader = new StringReader(template);
						processTemplate(request, parameters, "", templateReader, writer -> promise.complete(writer.toString()));

					} else {
						processTemplate(request, template, parameters, promise::complete);
					}
				}).onFailure(promise::fail);
		return promise.future();
	}

	/** Find the theme associated to the request. */
	protected Future<String> getThemePath(HttpServerRequest request) {
        Promise<String> promise = Promise.promise();
		vertx.eventBus().request( 
			"portal",
			new JsonObject().put("action", "getTheme"),
			new DeliveryOptions().setHeaders(request.headers()), 
			handlerToAsyncHandler( reply -> {
				promise.complete( String.join(File.separator, config.getString("assets-path", "../.."), "assets", "themes", reply.body().getString("theme")) );
			})
		);
		return promise.future();
	}

	/** Load the keys/values for Timeline i18n. */
	protected Future<Map<String, JsonObject>> loadThemeKVs(final HttpServerRequest request) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
		getThemePath(request).onComplete( result -> {
			if( result.succeeded() ) {
				final String i18nDirectory = String.join(File.separator, result.result(), "i18n", "Timeline");
				vertx.fileSystem().exists(i18nDirectory, ar -> {
					if (ar.succeeded() && ar.result()) {
						vertx.fileSystem().readDir(i18nDirectory, asyncResult -> {
							if (asyncResult.succeeded()) {
								readI18nTimeline(asyncResult.result())
								.onSuccess( themeKV -> promise.complete(themeKV) );
							} else {
								log.error("Error loading assets at "+i18nDirectory, asyncResult.cause());
								promise.complete(null);
							}
						});
					} else if (ar.failed()) {
						log.error("Error loading assets at "+i18nDirectory, ar.cause());
						promise.complete(null);
					}
				});
			} else {
				promise.fail("Unable to load keys/values for email templating.");
			}
		});
		return promise.future();
	}

	/** Default values for email templating. */
	protected Map<String, JsonObject> getThemeDefaults() {
		Map<String, JsonObject> themeKVs = new HashMap<String, JsonObject>();
		themeKVs.put("fr", new JsonObject()
			.put("timeline.mail.body.bgcolor", "#f9f9f9")
			.put("timeline.mail.body.bg", "background-color: #f9f9f9;")
			.put("timeline.mail.main", "background-color: #fff;")
			.put("timeline.mail.main.border", "border: 1px solid #e9e9e9;")
			.put("timeline.mail.maincolor", "#fff")
			.put("timeline.mail.text.color", "color: #fff;")
			.put("timeline.mail.header.bg", "background-color: #209DCC;")
			.put("timeline.mail.header.bgcolor", "#209DCC")
			.put("timeline.mail.main.text.color", "color: #000;")
			.put("timeline.mail.footer.color", "color: #999;")
		);
		return themeKVs;
	}

	/** Load Timeline i18n to retrieve PF/Project name and add it to custom email subject. */
	protected Future<String> getProjectNameFromTimelineI18n(final HttpServerRequest request) {
		final String[] keys = {"timeline.mail.projectName"};
		return I18nUtils.getI18nOfModule(vertx, request, "timeline", keys, null)
			.map(i18n->i18n.getString(keys[0]));
	}

	/** Load and parse i18n files. */
	protected Future<Map<String, JsonObject>> readI18nTimeline(List<String> filePaths) {
		Promise<Map<String, JsonObject>> promise = Promise.promise();
		final Map<String, JsonObject> themeKV = new HashMap<String, JsonObject>();
		final AtomicInteger count = new AtomicInteger(filePaths.size());
		for(final String path : filePaths) {
			vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
				@Override
				public void handle(AsyncResult<FileProps> ar) {
					if (ar.succeeded() && ar.result().isRegularFile()) {
						final String k = new File(path).getName().split("\\.")[0];
						vertx.fileSystem().readFile(path, ar2 -> {
							if (ar2.succeeded()) {
								JsonObject jo = new JsonObject(ar2.result().toString("UTF-8"));
								themeKV.put(k, jo);
							}
							if (count.decrementAndGet() == 0) {
								promise.complete(themeKV);
							}
						});
					} else {
						if (count.decrementAndGet() == 0) {
							promise.complete(themeKV);
						}
					}
				}
			});
		}
		return promise.future();
	}

	/* Override i18n to use additional theme variables */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request) {
		super.setLambdaTemplateRequest(request);

		final Mustache.Lambda hostLambda = new Mustache.Lambda() {
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
		};

		this.templateProcessor.setLambda("theme", new Mustache.Lambda() {
			@Override
			public void execute(Template.Fragment frag, Writer out) throws IOException {
				String key = frag.execute();
				String language = getOrElse(I18n.acceptLanguage(request), "fr", false);
				// {{theme}} directives may have inner {{host}}
				Object innerCtx = new Object() {
					Mustache.Lambda host = hostLambda;
				};

				// #46383, translations from the theme takes precedence over those from the domain
				final String translatedContents = I18n.getInstance().translate(key, Renders.getHost(request), I18n.getTheme(request), I18n.getLocale(language));
				if (!translatedContents.equals(key)) {
					Mustache.compiler().compile(translatedContents).execute(innerCtx, out);
				} else {
					JsonObject timelineI18n = (requestThemeKV==null ? getThemeDefaults():requestThemeKV).getOrDefault( language.split(",")[0].split("-")[0], new JsonObject() );
					Mustache.compiler().compile(timelineI18n.getString(key, key)).execute(innerCtx, out);
				}
			}
		});

		this.templateProcessor.setLambda("host", hostLambda);
	}

	/**
	 * Generate email subject by loading Timeline i18n to retrieve PF/Project name and add it to custom email subject.
	 * @param request to get host, language, ...
	 * @param i18nKey key for email subject
	 * @return subject
	 */
	protected Future<String> formatEmailSubject(HttpServerRequest request, String i18nKey, JsonObject parameters) {
		return getProjectNameFromTimelineI18n(request).compose( projectName -> {
			parameters.put("projectName", projectName);
			final String initialValue = I18n.getInstance().translate(i18nKey, getHost(request), I18n.acceptLanguage(request));
			return processEmailTemplate(request, parameters, initialValue, true);
		});
	}
}
