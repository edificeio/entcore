/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.utils;

import org.entcore.common.user.UserInfos;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.shareddata.LocalMap;

public class Mfa {
    static public final String TYPE_SMS   = "sms";
    static public final String TYPE_EMAIL = "email";

	public static class Factory {
		private Vertx vertx;
		private JsonObject config;
		private JsonArray mfaProtectedUrls;
		private Mfa service;
	
		private Factory() {}
	
		private static class FactoryHolder {
			private static final Factory instance = new Factory();
		}
	
		public static Factory getFactory() {
			return FactoryHolder.instance;
		}
	
		public void init(Vertx vertx, JsonObject config) {
			this.vertx = vertx;
			this.config = (config!=null) ? config.getJsonObject("mfaConfig") : null;
			if( this.config == null ) {
				LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
				String s = (String) server.get("mfaConfig");
				this.config = (s != null) ? new JsonObject(s) : null;
			}
			// TODO extraire la liste réelle des URLs sensibles
			mfaProtectedUrls = new JsonArray("[\"/auth/url1\", \"/auth/url1\"]");
		}
	
		public static Mfa getInstance() {
			return getFactory().getService();
		}
	
		public Mfa getService() {
			if (service == null ) {
				service = new Mfa(vertx, config);
			}
			return service;
		}
	
		public JsonObject getConfig() {
			return config;
		}
	}

	private boolean withSms = false;
	private boolean withEmail = false;
	private Mfa(final io.vertx.core.Vertx vertx, final JsonObject params) {
		if( params != null ) {
			final JsonArray types = params.getJsonArray("types", new JsonArray());
			for( int i=0; i<types.size(); i++ ) {
				final Object o = types.getValue(i);
				if( o instanceof String ) {
					types.set( i, ((String)o).toLowerCase() );
				}
			}
			this.withSms = types.contains(Mfa.TYPE_SMS);
			this.withEmail = types.contains(Mfa.TYPE_EMAIL);
		}
	}

	/** Check if MFA can be completed through an SMS. */
	public static boolean withSms() {
		return Factory.getInstance().withSms;
	}

	/** Check if MFA can be completed through an email. */
	public static boolean withEmail() {
		return Factory.getInstance().withEmail;
	}

	/** Get an array of MFA-protected URLs. */
	public static JsonArray getMfaProtectedUrls() {
		return Factory.getFactory().mfaProtectedUrls;
	}

    public static boolean isNotActivatedForUser(final UserInfos userInfos) {
        return (!(Factory.getInstance().withSms || Factory.getInstance().withEmail) || userInfos.getIgnoreMFA() );
    }

}
