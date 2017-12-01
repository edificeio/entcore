/*
 * Copyright © WebServices pour l'Éducation, 2014
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
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class ResourceProviderFilter implements ResourcesProvider {

	protected static final Logger log = LoggerFactory.getLogger(BaseResourceProvider.class);
	private static final String DEFAULT = "default";
	private Map<String, ResourcesProvider> filtersMapping = new HashMap<>();

	public ResourceProviderFilter() {
		loadFiltersMapping();
	}

	protected void loadFiltersMapping() {
		InputStream is = BaseResourceProvider.class.getClassLoader().getResourceAsStream(
				"Filters.json");
		final ServiceLoader<ResourcesProvider> filters = ServiceLoader.load(ResourcesProvider.class);
		Map<String, ResourcesProvider> mf = new HashMap<>();
		for (ResourcesProvider rp : filters) {
			log.debug(rp.getClass().getName());
			mf.put(rp.getClass().getName(), rp);
		}
		if (is != null) {
			BufferedReader r = null;
			try {
				r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while((line = r.readLine()) != null) {
					JsonObject filter = new JsonObject(line);
					String method = filter.getString("method");
					String f = filter.getString("filter");
					if (f != null && method != null &&
							!f.trim().isEmpty() && !method.trim().isEmpty()) {
						ResourcesProvider rp = mf.get(f);
						if (rp != null) {
							filtersMapping.put(method, rp);
						} else {
							log.error("Missing filter " + f);
						}
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

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
		ResourcesProvider filter = filtersMapping.get(binding.getServiceMethod());
		if (filter == null) {
			filter = filtersMapping.get(DEFAULT);
			if (filter == null) {
				log.warn("Missing filter for method " + binding.getServiceMethod());
				handler.handle(false);
				return;
			}
		}
		filter.authorize(resourceRequest, binding, user, handler);
	}

	public void setDefault(ResourcesProvider f) {
		filtersMapping.put(DEFAULT, f);
	}

}
