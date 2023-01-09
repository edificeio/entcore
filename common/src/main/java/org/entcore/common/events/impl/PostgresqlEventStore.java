/*
 * Copyright © "Open Digital Education", 2020
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import org.entcore.common.sql.Sql;
import org.entcore.common.validation.ValidationException;

import fr.wseduc.webutils.Either;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.pgclient.SslMode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class PostgresqlEventStore extends GenericEventStore {

	private static final int MAX_RETRY = 10;
	private String platform;
	private PgPool pgClient;
	private Set<String> knownEvents;
	private final AtomicInteger retryInitKnownEvents = new AtomicInteger(MAX_RETRY);

	public void init() {
		init(ar -> {
			if (ar.failed()) {
				logger.error("Error init PostgresqlEventStore", ar.cause());
			}
		});
	}

	public void init(Handler<AsyncResult<Void>> handler) {
		if (pgClient != null && knownEvents != null) {
			handler.handle(Future.succeededFuture());
			return;
		}
		final String eventStoreConf = (String) vertx.sharedData().getLocalMap("server").get("event-store");
		if (eventStoreConf != null) {
			final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
			platform = eventStoreConfig.getString("platform");
			final JsonObject eventStorePGConfig = eventStoreConfig.getJsonObject("postgresql");
			if (eventStorePGConfig != null) {
				final SslMode sslMode = SslMode.valueOf(eventStorePGConfig.getString("ssl-mode", "DISABLE"));
				final PgConnectOptions options = new PgConnectOptions()
					.setPort(eventStorePGConfig.getInteger("port", 5432))
					.setHost(eventStorePGConfig.getString("host"))
					.setDatabase(eventStorePGConfig.getString("database"))
					.setUser(eventStorePGConfig.getString("user"))
					.setPassword(eventStorePGConfig.getString("password"))
					.setIdleTimeout(eventStorePGConfig.getInteger("idle-timeout", 300)); // unit seconds
				final PoolOptions poolOptions = new PoolOptions()
						.setMaxSize(eventStorePGConfig.getInteger("pool-size", 5));
				if (!SslMode.DISABLE.equals(sslMode)) {
					options
						.setSslMode(sslMode)
						.setTrustAll(SslMode.ALLOW.equals(sslMode) || SslMode.PREFER.equals(sslMode) || SslMode.REQUIRE.equals(sslMode));
				}
				pgClient = PgPool.pool(vertx, options, poolOptions);
				listKnownEvents(ar -> {
					if (ar.succeeded()) {
						knownEvents = ar.result();
						handler.handle(Future.succeededFuture());
					} else {
						logger.error("Error listing known events", ar.cause());
						handler.handle(Future.failedFuture(ar.cause()));
					}
				});
			} else {
				handler.handle(Future.failedFuture(new ValidationException("Missing postgresql config.")));
			}
		} else {
			handler.handle(Future.failedFuture(new ValidationException("Missing event store config.")));
		}
	}

	private void listKnownEvents(Handler<AsyncResult<Set<String>>> handler) {
		final String listEventsTypesQuery =
				"SELECT table_name " +
				"FROM information_schema.tables " +
				"WHERE table_schema = 'events' and table_name LIKE '%_events'";
		final Collector<Row, ?, Set<String>> collector = Collectors.mapping(
			row -> row.getString("table_name").replace("_events", "").toUpperCase(), Collectors.toSet());
		pgClient.query(listEventsTypesQuery).collecting(collector).execute(ar -> {
			if (ar.succeeded()) {
				handler.handle(Future.succeededFuture(ar.result().value()));
			} else {
				handler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	@Override
	protected void storeEvent(final JsonObject event, final Handler<Either<String, Void>> handler) {
		store(event, handler);
	}

	public void store(final JsonObject event, final Handler<Either<String, Void>> handler) {
		if (knownEvents == null) {
			logger.error("Knows events is null : " + event.encode());
			if (retryInitKnownEvents.get() > 0) {
				listKnownEvents(ar -> {
					if (ar.succeeded()) {
						knownEvents = ar.result();
						retryInitKnownEvents.set(MAX_RETRY);
					} else {
						logger.error("Error listing known events", ar.cause());
						retryInitKnownEvents.decrementAndGet();
					}
				});
			}
			return;
		}
		final String id = (String) event.remove("_id");
		final String ua = (String) event.remove("ua");
		final String eventType = (String) event.remove("event-type");
		final String userId = (String) event.remove("userId");
		final String profile = (String) event.remove("profil");
		final String resourceType = (String) event.remove("resource-type");
		final LocalDateTime d = Instant.ofEpochMilli((long) event.remove("date"))
				.atZone(ZoneId.systemDefault()).toLocalDateTime();
		final JsonObject e;
		final String tableName;
		if (knownEvents.contains(eventType)) {
			event.remove("structures");
			event.remove("classes");
			event.remove("groups");
			event.remove("referer");
			event.remove("sessionId");
			e = event;
			tableName = "events." + eventType.toLowerCase() + "_events";
		} else {
			e = new JsonObject();
			final String module = (String) event.remove("module");
			if (module != null) {
				e.put("module", module);
			}
			e.put("payload", event.encode());
			tableName = "events.unknown_events";
		}
		e.put("id", (id != null ? id : UUID.randomUUID().toString()));
		e.put("event_type", eventType);
		e.put("date", d.toString());
		e.put("platform_id", platform);
		if (isNotEmpty(ua)) {
			e.put("ua", ua);
		}
		if (isNotEmpty(userId)) {
			e.put("user_id", userId);
		}
		if (isNotEmpty(profile)) {
			e.put("profile", profile);
		}
		if (isNotEmpty(resourceType)) {
			e.put("resource_type", resourceType);
		}
		insertEvent(e, tableName, handler);
	}

	private void insertEvent(final JsonObject e, final String tableName, final Handler<Either<String, Void>> handler) {
		final String ip = e.getString("ip");
		if (ip != null) {
			final int idxComma = ip.indexOf(',');
			if (idxComma > 0) {
				logger.warn("Remove proxy ip part in ip : " + ip);
				e.put("ip", ip.substring(0, idxComma));
			}
		}
		final String query = Sql.insertQuery(tableName, e);
		pgClient.query(query).execute(ar -> {
			if (ar.succeeded()) {
				handler.handle(new Either.Right<String, Void>(null));
			} else {
				logger.error("Error persisting events on postgresql : " + e.encode(), ar.cause());
				handler.handle(new Either.Left<String, Void>(
							"Error : " + ar.cause().getMessage() + ", Event : " + e.encode()));
			}
		});
	}

	@Override
	public void storeCustomEvent(String baseEventType, JsonObject payload) {
		final String tableName = "events." + baseEventType.toLowerCase() + "_events";
		payload.put("id", UUID.randomUUID().toString());
		payload.put("date", LocalDateTime.now().toString());
		payload.put("platform_id", platform);
		insertEvent(payload, tableName, either -> {
			if (either.isLeft()) {
				logger.error("Error adding custom event : " + payload.encode() + " - " + either.left().getValue());
			}
		});
	}

	public Set<String> getKnownEvents() {
		return knownEvents;
	}

}
