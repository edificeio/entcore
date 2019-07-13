/*
 * Copyright © "Open Digital Education", 2019
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.communication.services.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.communication.services.CommunicationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static fr.wseduc.webutils.Utils.isNotEmpty;

@RunWith(VertxUnitRunner.class)
public class OptimComTest {

	private static final String USERS_QUERY =
			"MATCH (u:User) " +
			"WHERE HAS(u.password) AND NOT(HAS(u.deleteDate)) " +
			"RETURN u.id as id, HEAD(u.profiles) as profile LIMIT 1000";
	private static final String CUSTOM_RETURN =
			"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
			"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
			"HEAD(visibles.profiles) as profile ";
	private static final Logger log = LoggerFactory.getLogger(OptimComTest.class);

	private Vertx vertx;
	private CommunicationService defaultComService;
	private CommunicationService xpComService;
	private final Map<String, JsonObject> defaultResults = new HashMap<>();
	private final Map<String, JsonObject> xpResults = new HashMap<>();

	@Before
	public void setUp(TestContext context) {
		vertx = Vertx.vertx();
		defaultComService = new DefaultCommunicationService();
		xpComService = new XpCommunicationService();
		Neo4j.getInstance().init(vertx, new JsonObject().put("server-uri", "http://localhost:7474/db/data/"));
	}

	@Test
	public void testXpComRules(TestContext context) {
		testComRules(context, xpComService, xpResults);
	}

	@Test
	public void testWait(TestContext context) {
		final Async async = context.async();
		vertx.setTimer(30000L, h -> async.complete());
	}

	@Test
	public void testDefaultComRules(TestContext context) {
		testComRules(context, defaultComService, defaultResults);
	}

	private void testComRules(TestContext context, CommunicationService communicationService,
			Map<String, JsonObject> results) {
		final Async async = context.async();
		Neo4j.getInstance().execute(USERS_QUERY, new JsonObject(), r -> {
			final JsonArray a = r.body().getJsonArray("result");
			if ("ok".equals(r.body().getString("status")) && a.size() > 0) {
				final List<Future> futures = new ArrayList<>();
				long i = 1;
				for (Object o : a) {
					final String userId = ((JsonObject) o).getString("id");
					final String userProfile = ((JsonObject) o).getString("profile");
					futures.add(getComRules(communicationService, userId, userProfile, i++));
				}
				CompositeFuture.all(futures).setHandler(ar -> {
					if (ar.succeeded()) {
						int mean = 0;
						List<Integer> times = new ArrayList<>();
						JsonArray res = new JsonArray();
						for (JsonObject e: results.values()) {
							final int time =  e.getInteger("time");
							mean += time;
							times.add(time);
							List<String> vName = new ArrayList<>();
							for (Object o : e.getJsonArray("visibles")) {
								if (isNotEmpty(((JsonObject) o).getString("name"))) {
									vName.add(((JsonObject) o).getString("name"));
								} else {
									vName.add(((JsonObject) o).getString("displayName"));
								}
							}
							Collections.sort(vName);
							JsonObject el = new JsonObject()
									.put("user", e.getString("userId"))
									//.put("time", time)
									.put("visibles", new JsonArray(vName));
							res.add(el);
						}
						Collections.sort(times);
						log.info("Min : " + times.get(0));
						log.info("Max : " + times.get(times.size() -1));
						mean = mean / times.size();
						log.info("Mean : " + mean);
						log.info("95 pct : " + times.get((int) Math.round(0.95 * times.size())));
						vertx.fileSystem().writeFile("/tmp/results-" + communicationService
								.getClass().getSimpleName() + ".json", Buffer.buffer(res.encodePrettily()), arf -> {
							if (arf.succeeded()) {
								async.complete();
							} else {
								context.fail();
							}
						});
					} else {
						context.fail();
					}
				});
			} else {
				context.fail();
			}
		});
	}

	private Future<Void> getComRules(CommunicationService communicationService, String userId, String userProfile, long i) {
		final Future<Void> future = Future.future();
		vertx.setTimer(i * 50L, h -> {
			final long start = System.currentTimeMillis();
			communicationService.visibleUsers(userId, null, null, true, true,
					false, null, CUSTOM_RETURN, new JsonObject(), userProfile, visibles -> {
						if (visibles.isRight()) {
							final JsonObject j = new JsonObject()
									.put("visibles", visibles.right().getValue())
									.put("time", System.currentTimeMillis() - start)
									.put("userId", userId);
							if (communicationService instanceof XpCommunicationService) {
								xpResults.put(userId, j);
							} else {
								defaultResults.put(userId, j);
							}
							future.complete();
						} else {
							future.fail(new RuntimeException(visibles.left().getValue()));
						}
					});
		});
		return future;
	}

}
