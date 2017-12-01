/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.common.utils;

import fr.wseduc.webutils.collections.AsyncLocalMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.spi.cluster.ClusterManager;

import java.util.HashMap;
import java.util.Map;

public class MapFactory {

	private static final Logger log = LoggerFactory.getLogger(MapFactory.class);

	public static <K,V> AsyncMap<K,V> getAsyncMap(LocalMap<K,V> localMap) {
		return getAsyncMap(localMap, null);
	}

	public static <K,V> AsyncMap<K,V> getAsyncMap(LocalMap<K,V> localMap, Vertx vertx) {
		if (localMap == null) {
			return null;
		}
		return new AsyncLocalMap<K, V>(localMap, vertx);
	}

	public static <K,V> AsyncMap<K,V> getAsyncMap(AsyncMap<K, V> asyncMap) {
		return asyncMap;
	}

	public static <K,V> void getClusterMap(String name, Vertx vertx, Handler<AsyncMap<K, V>> handler) {
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		Boolean cluster = (Boolean) server.get("cluster");
		if (Boolean.TRUE.equals(cluster)) {
			vertx.sharedData().getClusterWideMap(name, new Handler<AsyncResult<AsyncMap<K, V>>>() {
				@Override
				public void handle(AsyncResult<AsyncMap<K, V>> event) {
					if (event.succeeded()) {
						handler.handle(event.result());
					} else {
						log.error("Error loading clusterMap : " + name);
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(getAsyncMap(vertx.sharedData().getLocalMap(name), vertx));
		}
	}

	@Deprecated
	public static <K,V> Map<K,V> getSyncClusterMap(String name, Vertx vertx) {
		return getSyncClusterMap(name, vertx, true);
	}

	@Deprecated
	public static <K,V> Map<K,V> getSyncClusterMap(String name, Vertx vertx, boolean elseLocalMap) {
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		Boolean cluster = (Boolean) server.get("cluster");
		final Map<K, V> map;
		if (Boolean.TRUE.equals(cluster)) {
			ClusterManager cm = ((VertxInternal) vertx).getClusterManager();
			map = cm.getSyncMap(name);
		} else if (elseLocalMap) {
			map = vertx.sharedData().getLocalMap(name);
		} else {
			map = new HashMap<>();
		}
		return map;
	}

}
