/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.common.neo4j;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.impl.HttpClientImpl;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Neo4jRestNodeClient {

	private final Vertx vertx;
	private final HttpClient[] clients;
	private final AtomicInteger master = new AtomicInteger(0);
	private final CopyOnWriteArrayList<Integer> slaves;
	private final long checkTimerId;
	private final Random rnd;
	private static final Logger logger = LoggerFactory.getLogger(Neo4jRestNodeClient.class);

	public Neo4jRestNodeClient(URI[] uris, Vertx vertx, long delay, int poolSize, boolean keepAlive) {
		this.vertx = vertx;
		clients = new HttpClient[uris.length];
		for (int i = 0; i < uris.length; i++) {
			final HttpClientOptions options = new HttpClientOptions()
					.setDefaultHost(uris[i].getHost())
					.setDefaultPort(uris[i].getPort())
					.setMaxPoolSize(poolSize)
					.setKeepAlive(keepAlive);
			clients[i] = vertx.createHttpClient(options);
		}

		if (uris.length > 1) {
			slaves = new CopyOnWriteArrayList<>();
			checkHealth();
			checkTimerId = vertx.setPeriodic(delay, new Handler<Long>() {
				@Override
				public void handle(Long event) {
					checkHealth();
				}
			});
		} else {
			slaves = null;
			checkTimerId = -1;
		}
		rnd = new Random();
	}

	private void checkHealth() {
		for (int i = 0; i < clients.length; i++) {
			final int idx = i;
			HttpClient client = clients[i];
			if (client != null) {
				client.getNow("/db/manage/server/ha/available", new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse resp) {
						if (resp.statusCode() == 200) {
							resp.bodyHandler(new Handler<Buffer>() {
								@Override
								public void handle(Buffer body) {
									if ("master".equals(body.toString())) {
										masterNode(idx);
									} else {
										slaveNode(idx);
									}
								}
							});
						} else {
							unavailableNode(idx);
						}
					}
				});
			} else {
				unavailableNode(idx);
			}
		}
	}

	private void masterNode(int idx) {
		int oldMaster = master.getAndSet(idx);
		if (oldMaster != idx) {
			slaves.remove(Integer.valueOf(idx));
			if (logger.isDebugEnabled()) {
				logger.debug("Neo4j new master node " + idx + " (" + ((HttpClientImpl) clients[idx]).getOptions().getDefaultHost() + ").");
			}
		}
	}

	private void slaveNode(int idx) {
		master.compareAndSet(idx, -1);
		final boolean newSlave = slaves.addIfAbsent(idx);
		if (logger.isDebugEnabled() && newSlave) {
			logger.debug("Neo4j new slave node " + idx + " (" + ((HttpClientImpl) clients[idx]).getOptions().getDefaultHost() + ").");
		}
	}

	private void unavailableNode(int idx) {
		master.compareAndSet(idx, -1);
		slaves.remove(Integer.valueOf(idx));
	}

	public HttpClient getClient() throws Neo4jConnectionException {
		try {
			return clients[master.get()];
		} catch (RuntimeException e) {
			throw new Neo4jConnectionException("Can't get master connection.", e);
		}
	}

	public HttpClient getSlaveClient() throws Neo4jConnectionException {
		if (slaves == null || slaves.size() < 1) {
			return getClient();
		}
		try {
			return clients[slaves.get(rnd.nextInt(slaves.size()))];
		} catch (RuntimeException e) {
			throw new Neo4jConnectionException("Can't get master connection.", e);
		}
	}

	public void close() {
		if (checkTimerId > 0) {
			vertx.cancelTimer(checkTimerId);
		}
		if (clients != null && clients.length > 0) {
			for (HttpClient client : clients) {
				if (client != null) {
					client.close();
				}
			}
		}
	}

}
