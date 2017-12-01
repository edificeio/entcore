/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseResourceProvider implements ResourcesProvider {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected static final Logger log = LoggerFactory.getLogger(BaseResourceProvider.class);
	private static final String DEFAULT = "default";
	private Map<String, MethodHandle> filtersMapping = new HashMap<>();

	protected BaseResourceProvider() {
		loadFiltersMapping();
	}

	protected void loadFiltersMapping() {
		loadFilter(DEFAULT, defaultFilter());
		InputStream is = BaseResourceProvider.class.getClassLoader().getResourceAsStream(
				"Filters.json");
		if (is != null) {
			BufferedReader r = null;
			try {
				r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while((line = r.readLine()) != null) {
					JsonObject filter = new JsonObject(line);
					String method = filter.getString("method");
					String filterMethod = filter.getString("filter");
					if (filterMethod != null && method != null &&
							!filterMethod.trim().isEmpty() && !method.trim().isEmpty()) {
						loadFilter(method, filterMethod);
					}
				}
			} catch (IOException e) {
				log.error("Unable to load filters in " + this.getClass().getName(), e);
			} finally {
				if (r != null) {
					try {
						r.close();
					} catch (IOException e) {
						log.error("Close inputstream error", e);
					}
				}
			}
		} else {
			log.warn("Not found resource filter file.");
		}
	}

	private void loadFilter(String method, String filterMethod) {
		try {
			MethodHandle mh = lookup.bind(this, filterMethod,
					MethodType.methodType(void.class, HttpServerRequest.class, String.class,
							UserInfos.class, Handler.class));
			filtersMapping.put(method, mh);
			if (log.isDebugEnabled()) {
				log.debug("loadFilter : " + filterMethod + " for method " + method);
			}
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error("Unable to load filter " + filterMethod, e);
		}
	}

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		MethodHandle mh = filtersMapping.get(binding.getServiceMethod());
		if (mh == null) {
			mh = filtersMapping.get(DEFAULT);
			if (mh == null) {
				log.warn("Missing filter for method " + binding.getServiceMethod());
				handler.handle(false);
				return;
			}
		}
		try {
			String shareMethod = binding.getServiceMethod().replaceAll("\\.", "-");
			mh.invokeExact(resourceRequest, shareMethod, user, handler);
		} catch (Throwable e) {
			log.error("Error invoking method : " + mh.toString(), e);
			handler.handle(false);
		}
	}

	protected abstract String defaultFilter();

}
