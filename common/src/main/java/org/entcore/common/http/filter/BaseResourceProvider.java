/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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

	private void loadFiltersMapping() {
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
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error("Unable to load filter " + filterMethod);
		}
	}

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		MethodHandle mh = filtersMapping.get(binding.getServiceMethod());
		if (mh == null) {
			mh = filtersMapping.get(DEFAULT);
			if (mh == null) {
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
