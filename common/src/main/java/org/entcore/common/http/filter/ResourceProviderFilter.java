/*
 * Copyright © "Open Digital Education", 2014
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
