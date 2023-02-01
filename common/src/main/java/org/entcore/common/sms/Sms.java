/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.common.sms;

import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

//-------------------
public class Sms {
//-------------------

	//--------------------------------------
	private static class SmsFactoryHolder {
	//--------------------------------------
		private static final SmsFactory instance = new SmsFactory();
	}

	//--------------------------------------
	public static class SmsFactory {
	//--------------------------------------
		private Vertx vertx;
		private EventBus eb;
		private JsonObject config;
		private String smsProvider, smsAddress;

		protected SmsFactory() {
		}

		public void init(Vertx vertx, JsonObject config) {
			this.eb = Server.getEventBus(vertx);
			this.vertx = vertx;
			this.config = config;
			LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
			if(server != null && server.get("smsProvider") != null) {
				smsProvider = (String) server.get("smsProvider");
				final String node = (String) server.get("node");
				smsAddress = (node != null ? node : "") + "entcore.sms";
			} else {
				smsAddress = "entcore.sms";
			}
		}

		public Sms newInstance( Renders render ) {
			return new Sms( (render == null) ? new Renders(vertx, config) : render );
		}

		protected JsonObject getSmsObjectFor(final String target, final String body) {
			return new JsonObject()
			.put("provider", smsProvider)
			.put("action", "send-sms")
			.put("parameters", new JsonObject()
				.put("receivers", new fr.wseduc.webutils.collections.JsonArray().add(target))
				.put("message", body)
				.put("senderForResponse", true)
				.put("noStopClause", true));
		}

		protected Future<JsonObject> send(final JsonObject smsObject) {
			Promise<JsonObject> promise = Promise.promise();
			eb.request(smsAddress, smsObject, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					if("error".equals(event.body().getString("status"))){
						promise.fail(event.body().getString("message", ""));
					} else {
						promise.complete( new JsonObject() );
					}
				}
			}));
			return promise.future();
		}
	}

	////////////////////////////////////////

	public static SmsFactory getFactory() {
		return SmsFactoryHolder.instance;
	}

	////////////////////////////////////////

	private Renders render;

	private Sms(final Renders render) {
		this.render = render;
	}

	public Future<JsonObject> send(HttpServerRequest request, final String phone, String template, JsonObject params){
		if (StringUtils.isEmpty(phone)) {
			return Future.failedFuture("invalid.phone");
		}

		final String formattedPhone = StringValidation.formatPhone(phone);

		return processTemplate(request, template, params)
		.compose( body -> {
			final JsonObject smsObject = getFactory().getSmsObjectFor(formattedPhone, body);
			return getFactory().send( smsObject );
		});
	}

	private Future<String> processTemplate(HttpServerRequest request, String template, JsonObject params){
		Promise<String> promise = Promise.promise();
		render.processTemplate(request, template, params, body -> {
			if (body != null) {
				promise.complete(body);
			} else {
				promise.fail("template.error");
			}
		});
		return promise.future();
	}
}