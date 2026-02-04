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

package org.entcore.common.events.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.events.DeviceInfoDTO;
import org.entcore.common.events.UserAgentService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreSQL-based User-Agent parsing service with LRU cache
 */
public class UserAgentServiceImpl implements UserAgentService {

	private static final Logger logger = LoggerFactory.getLogger(UserAgentServiceImpl.class);
	private static final int CACHE_SIZE = 10000;
	private static final String QUERY = "SELECT platform, platform_version, device_name, device_type FROM utils.user_agents WHERE user_agent = $1";
	
	private static UserAgentServiceImpl instance;
	
	private final Map<String, DeviceInfoDTO> cache;
	private final Vertx vertx;
	private final Pool pgClient;
	private boolean enabled = false;
	
	private UserAgentServiceImpl(Vertx vertx) {
		this.vertx = vertx;
		this.pgClient = this.initPool();
		this.cache = new LinkedHashMap<String, DeviceInfoDTO>(CACHE_SIZE, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, DeviceInfoDTO> eldest) {
				return size() > CACHE_SIZE;
			}
		};
	}
	
	public static UserAgentServiceImpl getInstance(final Vertx vertx) {
		if (instance == null) {
			instance = new UserAgentServiceImpl(vertx);
		}
		return instance;
	}
	
	/**
	 * Initialize PostgreSQL connection from event-store config
	 */
	private Pool initPool() {
		final String eventStoreConf = (String) vertx.sharedData().getLocalMap("server").get("event-store");
		if (eventStoreConf == null) {
			logger.warn("UserAgentService: no event-store config, service disabled");
			return null;
		}
		
		try {
			final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
			final JsonObject config = eventStoreConfig.getJsonObject("postgresql");
			
			if (config == null) {
				logger.warn("UserAgentService: no PostgreSQL config, service disabled");
				return null;
			}
			
			final SslMode sslMode = SslMode.valueOf(config.getString("ssl-mode", "DISABLE"));
			final PgConnectOptions options = new PgConnectOptions()
				.setPort(config.getInteger("port", 5432))
				.setHost(config.getString("host"))
				.setDatabase(config.getString("database"))
				.setUser(config.getString("user"))
				.setPassword(config.getString("password"))
				.setIdleTimeout(config.getInteger("idle-timeout", 300));
			
			final PoolOptions poolOptions = new PoolOptions()
				.setMaxSize(config.getInteger("pool-size", 5));
			
			if (!SslMode.DISABLE.equals(sslMode)) {
				options
					.setSslMode(sslMode)
					.setTrustAll(SslMode.ALLOW.equals(sslMode) || SslMode.PREFER.equals(sslMode) || SslMode.REQUIRE.equals(sslMode));
			}
			
			this.enabled = true;
			logger.info("UserAgentService initialized with PostgreSQL pool");
			// Use the generic Pool factory to return an io.vertx.sqlclient.Pool
			return Pool.pool(vertx, options, poolOptions);
		} catch (Exception e) {
			logger.error("Failed to initialize UserAgentService PostgreSQL pool", e);
			this.enabled = false;
			return null;
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public Future<DeviceInfoDTO> parseUserAgent(String userAgent) {
		if (userAgent == null || userAgent.trim().isEmpty()) {
			return Future.succeededFuture(createFallbackDeviceInfo());
		}
		
		final DeviceInfoDTO cached = cache.get(userAgent);
		if (cached != null) {
			return Future.succeededFuture(cached);
		}
		
		if (!enabled || pgClient == null) {
			return Future.succeededFuture(createFallbackDeviceInfo());
		}
		
		final Promise<DeviceInfoDTO> promise = Promise.promise();
		
		pgClient.preparedQuery(QUERY)
			.execute(Tuple.of(userAgent))
			.onSuccess(rows -> {
				DeviceInfoDTO deviceInfo;
				if (rows.size() > 0) {
					final Row row = rows.iterator().next();
					deviceInfo = new DeviceInfoDTO(
						row.getString("platform") != null ? row.getString("platform") : "Unknown",
						row.getString("platform_version") != null ? row.getString("platform_version") : "Unknown",
						row.getString("device_type") != null ? row.getString("device_type") : "desktop",
						row.getString("device_name") != null ? row.getString("device_name") : "Desktop"
					);
				} else {
					deviceInfo = createFallbackDeviceInfo();
				}
				cache.put(userAgent, deviceInfo);
				promise.complete(deviceInfo);
			})
			.onFailure(err -> {
				logger.error("Failed to query user_agents table for: " + userAgent, err);
				promise.complete(createFallbackDeviceInfo());
			});
		
		return promise.future();
	}
	
	private DeviceInfoDTO createFallbackDeviceInfo() {
		return new DeviceInfoDTO("Unknown", "Unknown", "desktop", "Desktop");
	}
}
