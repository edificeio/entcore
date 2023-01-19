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

package org.entcore.common.emailstate.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.Renders;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.user.SessionAttributes.*;
import static org.entcore.common.emailstate.DataStateUtils.*;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.emailstate.DataStateUtils;
import org.entcore.common.emailstate.DataValidationService;
import org.entcore.common.utils.StringUtils;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public abstract class AbstractDataValidationService extends Renders implements DataValidationService {
	private final Neo4j neo = Neo4j.getInstance();
	private final String field;
	private final String stateField;
	protected Map<String, JsonObject> requestThemeKV = null;

	protected AbstractDataValidationService(final String field, io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
		super(vertx, config);
		this.field = field;
		this.stateField = field+"State";
	}

	/** 
	 * @return {
	 * 	field: String|null, stateField: JsonObject|null,
	 *  firstName:string, lastName:string, displayName:string
	 * }
	 */
	protected Future<JsonObject> retrieveFullState(String userId) {
		final Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN COALESCE(u."+field+", null) as "+field+", COALESCE(u."+stateField+", null) as "+stateField+", " + 
					   "u.firstName as firstName, u.lastName as lastName, u.displayName as displayName ";
		JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, m -> {
			Either<String, JsonObject> r = validUniqueResult(m);
			if (r.isRight()) {
				final JsonObject result = r.right().getValue();
				result.put(stateField, fromRaw(result.getString(stateField)));
				promise.complete( result );
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

	/**
	 * @return updated state
	 */
	protected Future<JsonObject> updateState(String userId, final JsonObject state) {
		final Promise<JsonObject> promise = Promise.promise();
		StringBuilder query = new StringBuilder(
			"MATCH (u:`User` { id : {id}}) " +
			"SET u."+stateField+" = {state} "
		);
		JsonObject params = new JsonObject()
			.put("id", userId)
			.put("state", toRaw(state));
		if( DataStateUtils.getState(state) == DataStateUtils.VALID 
				&& !StringUtils.isEmpty(DataStateUtils.getValid(state)) ) {
			// We are going to update a user's session data => TODO propagate it
			query.append(", u."+field+" = {value} ");
			params.put("value", DataStateUtils.getValid(state));
		}
		neo.execute(query.toString(), params, m -> {
			Either<String, JsonObject> r = validEmpty(m);
			if (r.isRight()) {
				promise.complete(state);
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

    /**
     * Since Neo4j does not allow JSON objects to be node properties, stateField is stored as a JSON string
	 * => serialize it
     * @param state as JSON object
     * @return state as JSON string
     */
    protected String toRaw(final JsonObject state) {
        if( state==null ) return null;
        return state.encode();
    }

    /**
     * Since Neo4j does not allow JSON objects to be node properties, stateField is stored as a JSON string
	 * => deserialize it
     * @param state as JSON string
     * @return state as JSON object
     */
    protected JsonObject fromRaw(final String state) {
        if( state==null ) return null;
        return new JsonObject(state);
    }

	/** Generate a pseudo-random code of 6 digits length. */
	protected String generateRandomCode() {
		return String.format("%06d", Math.round(Math.random()*999999D));
	}

	protected JsonObject formatAsResponse(final int state, final String valid, final Integer tries, final Long ttl) {
		final JsonObject o = new JsonObject()
		.put("state", stateToString(state))
		.put("valid", (valid!=null) ? valid : "");
		if( state==PENDING || state==OUTDATED ) {
			if(tries!=null) o.put("tries", tries);
			if(ttl!=null) o.put("ttl", ttlToRemainingSeconds(ttl.longValue()));
		}
		return o;
	}

	@Override
	public Future<JsonObject> startUpdate(String userId, String value, final long validDurationS, final int triesLimit) {
		return retrieveFullState(userId)
		.compose( j -> {
			// Reset the stateField to a pending state
			final JsonObject originalState = j.getJsonObject("state", new JsonObject());
			setState(originalState, PENDING);
			// Valid mail must stay unchanged if not null, otherwise initialize to an empty string.
			if( getValid(originalState) == null ) {
				setValid(originalState, "");
			}
			setPending(originalState, value);
			setKey(originalState, generateRandomCode());
			setTtl(originalState, System.currentTimeMillis() + validDurationS * 1000l);
			setTries(originalState, triesLimit);

			return updateState(userId, originalState);
		});
	}

	@Override
	public Future<JsonObject> hasValid(String userId) {
		return retrieveFullState(userId)
		.map( j -> {
			Integer state = null;
			String value = j.getString(field);
			JsonObject valueState = j.getJsonObject(stateField);

			if (StringUtils.isEmpty(value) || valueState == null) {
				state = UNCHECKED;
			} else if( !value.equalsIgnoreCase( getValid(valueState) )) {
				// Case where the field was first validated and then changed.
				state = getState(valueState);
				if( state == VALID ) {
					state = UNCHECKED;
				}
			} else {
				// If mobile===valid, then state must be valid
				state = VALID; // /!\ do not replace by   state = getState(valueState); /!\
			}
			return formatAsResponse(state, getValid(valueState), null, null);
		});
	}

	@Override
	public Future<JsonObject> tryValidate(String userId, String code) {
		return retrieveFullState(userId)
		.compose( j -> {
			JsonObject state = j.getJsonObject(stateField);

			// Check business rules
			do {
				if( state == null ) {
					// Unexpected data, should never happen
					state = new JsonObject();
					j.put(stateField, state);
					setState(state, OUTDATED);
					break;
				}
				// if TTL or max tries reached don't check code
				if (getState(state) == OUTDATED) {
					break;
				}
				// Check code
				String key = StringUtils.trimToNull( getKey(state) );
				if( key == null || !key.equals(StringUtils.trimToNull(code)) ) {
					// Invalid
					Integer tries = getTries(state);
					if(tries==null) {
						tries = 0;
					} else {
						tries = Math.max(0, tries.intValue() - 1 );
					}
					if( tries <= 0 ) {
						setState(state, OUTDATED);
					}
					setTries(state, tries);
					break;
				}
				// Check time to live
				Long ttl = getTtl(state);
				if( ttl==null || ttl.compareTo(System.currentTimeMillis()) < 0 ) {
					// TTL reached
					setState(state, OUTDATED);
					break;
				}
				// Check pending mail address
				String pending = StringUtils.trimToNull( getPending(state) );
				if( pending == null) {
					// This should never happen, but treat it like TTL was reached
					setState(state, OUTDATED);
					break;
				}
				// ---Validation succeeded---
				setState(state, VALID);
				setValid(state, pending);
				setPending(state, null);
				setKey(state, null);
				setTtl(state, null);
				setTries(state, null);
			} while(false);

			// ---Validation results---
			return updateState(userId, state)
			.map( newState -> {
				return formatAsResponse(getState(newState), getValid(newState), getTries(newState), getTtl(newState));
			});
		});
	}

	@Override
	public Future<JsonObject> getCurrentState(String userId) {
		return retrieveFullState(userId);
	}

	@Override
    abstract public Future<Long> sendValidationMessage( final HttpServerRequest request, String mobile, JsonObject templateParams );

	////////////////////////////////////////////
	//FIXME The whole methods below are intended to retrieve overloaded i18n from Timeline because it contains variables for email templating...

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
			}
		});
		return promise.future();
	}

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
}
