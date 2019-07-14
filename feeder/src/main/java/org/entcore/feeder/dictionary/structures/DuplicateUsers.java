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

package org.entcore.feeder.dictionary.structures;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.validation.StringValidation;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.joda.time.DateTime;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class DuplicateUsers {

	private static final Logger log = LoggerFactory.getLogger(DuplicateUsers.class);
	private static final String INCREMENT_RELATIVE_SCORE =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}), " +
			"(u1)-[:RELATED]->()-[rp:DUPLICATE]-()<-[:RELATED]-(u2) " +
			"SET rp.score = rp.score + 1 ";
	private static final String SIMPLE_MERGE_QUERY =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}})-[r2]-() " +
			"SET u1.ignoreDuplicates = FILTER(uId IN u1.ignoreDuplicates WHERE uId <> {userId2}) " +
			"WITH u1, u2, r, r2, u2.IDPN as IDPN, u2.id as oldId, u2.externalId as u2ExternalId " +
			"SET u1.mergedIds = FILTER(oldIdF IN coalesce(u1.mergedIds, []) WHERE oldIdF <> oldId) + oldId, " +
			"u1.mergedExternalIds = FILTER(u2ExternalIdF IN coalesce(u1.mergedExternalIds, []) WHERE u2ExternalIdF <> u2ExternalId) + u2ExternalId " +
			"DELETE r, r2, u2 " +
			"WITH u1, IDPN, oldId, u2ExternalId " +
			"WHERE NOT(HAS(u1.IDPN)) AND NOT(IDPN IS NULL) " +
			"SET u1.IDPN = IDPN " +
			"RETURN DISTINCT oldId, u1.id as id, HEAD(u1.profiles) as profile ";
	private static final String SWITCH_MERGE_QUERY =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}})-[r2]-() " +
			"WITH u1, u2, r, r2, u2.source as source, u2.externalId as externalId, u2.IDPN as IDPN, u2.id as oldId " +
			"DELETE r, r2, u2 " +
			"WITH u1, source, externalId, IDPN, oldId, u1.externalId as u1ExternalId " +
			"SET u1.ignoreDuplicates = FILTER(uId IN u1.ignoreDuplicates WHERE uId <> {userId2}), " +
			"u1.mergedIds = FILTER(oldIdF IN coalesce(u1.mergedIds, []) WHERE oldIdF <> oldId) + oldId, " +
			"u1.mergedExternalIds = FILTER(u1ExternalIdF IN coalesce(u1.mergedExternalIds, []) WHERE u1ExternalIdF <> u1ExternalId) + u1ExternalId, " +
			"u1.externalId = externalId, u1.source = source, u1.disappearanceDate = null " +
			"WITH u1, IDPN, oldId " +
			"WHERE NOT(HAS(u1.IDPN)) AND NOT(IDPN IS NULL) " +
			"SET u1.IDPN = IDPN " +
			"RETURN DISTINCT oldId, u1.id as id, HEAD(u1.profiles) as profile ";
	private final List<String> notDeduplicateSource = Arrays.asList("AAF", "AAF1D");
	private final Map<String, Integer> sourcePriority = new HashMap<>();
	private final boolean updateCourses;
	private final boolean autoMergeOnlyInSameStructure;
	private final EventBus eb;
	private EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
	public static final JsonArray defaultSourcesOrder = new JsonArray()
			.add("AAF").add("AAF1D").add("CSV").add("EDT").add("UDT").add("MANUAL");

	public DuplicateUsers(boolean updateCourses, boolean autoMergeOnlyInSameStructure, EventBus eb) {
		this(null, updateCourses, autoMergeOnlyInSameStructure, eb);
	}

	public DuplicateUsers(JsonArray sourcesPriority, boolean updateCourses, boolean autoMergeOnlyInSameStructure,
			EventBus eb) {
		if (sourcesPriority == null) {
			sourcesPriority = defaultSourcesOrder;
		}
		final int size = sourcesPriority.size();
		for (int i = 0; i < size; i++) {
			sourcePriority.put(sourcesPriority.getString(i), size - i);
		}
		this.eb = eb;
		this.updateCourses = updateCourses;
		this.autoMergeOnlyInSameStructure = autoMergeOnlyInSameStructure;
	}

	public void markDuplicates(Handler<JsonObject> handler) {
		markDuplicates(null, handler);
	}

	public void markDuplicates(final Message<JsonObject> message) {
		markDuplicates(message, null);
	}

	public void markDuplicates(final Message<JsonObject> message, final Handler<JsonObject> handler) {
		final String now = DateTime.now().toString();
		final String query = "MATCH (s:System {name : 'Starter'}) return s.lastSearchDuplicates as lastSearchDuplicates ";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null &&
						res.size() == 1 && res.getJsonObject(0).getString("lastSearchDuplicates") != null) {
					final String last = res.getJsonObject(0).getString("lastSearchDuplicates");
					final String[] profiles = ManualFeeder.profiles.keySet().toArray(new String[ManualFeeder.profiles.keySet().size()]);
					final Handler[] handlers = new Handler[profiles.length + 1];
					final long start = System.currentTimeMillis();
					handlers[handlers.length - 1] = new Handler<Void>() {
						@Override
						public void handle(Void v) {
							final String updateDate = "MATCH (s:System {name : 'Starter'}) set s.lastSearchDuplicates = {now} ";
							TransactionManager.getNeo4jHelper().execute(updateDate, new JsonObject().put("now", now),
									new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> event) {
											if (!"ok".equals(event.body().getString("status"))) {
												log.error("Error updating last search duplicate date : " + event.body().getString("message"));
											}
										}
									});
				log.info("Mark duplicates users finished - elapsed time " + (System.currentTimeMillis() - start) + " ms.");
							if (message != null) {
								message.reply(new JsonObject().put("status", "ok"));
							}
							if (handler != null) {
								handler.handle(new JsonObject().put("status", "ok"));
							}
						}
					};
					for (int i = profiles.length - 1; i >= 0; i--) {
						final int j = i;
						handlers[i] = new Handler<Void>() {
							@Override
							public void handle(Void v) {
								searchDuplicatesByProfile(last, profiles[j], handlers[j + 1]);
							}
						};
					}
					handlers[0].handle(null);
				} else {
					log.warn("lastSearchDuplicates not found.");
					message.reply(new JsonObject().put("status", "ok"));
				}
			}
		});
	}

	public void ignoreDuplicate(final Message<JsonObject> message) {
		String userId1 = message.body().getString("userId1");
		String userId2 = message.body().getString("userId2");
		if (userId1 == null || userId2 == null || userId1.trim().isEmpty() || userId2.trim().isEmpty()) {
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.id"));
			return;
		}
		String query =
				"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}) " +
				"SET u1.ignoreDuplicates = coalesce(u1.ignoreDuplicates, []) + u2.id, " +
				"u2.ignoreDuplicates = coalesce(u2.ignoreDuplicates, []) + u1.id " +
				"DELETE r";
		JsonObject params = new JsonObject().put("userId1", userId1).put("userId2", userId2);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				message.reply(event.body());
			}
		});
	}

	public void listDuplicates(final Message<JsonObject> message) {
		JsonArray structures = message.body().getJsonArray("structures");
		boolean inherit = message.body().getBoolean("inherit");
		final Integer minScore = message.body().getInteger("minScore");
		final boolean inSameStructure = message.body().getBoolean("inSameStructure", false);

		final String filter = (minScore != null) ? ((inSameStructure) ? "AND":"WHERE") + " r.score >= {minScore} " : "";
		String query;
		if (structures != null && structures.size() > 0) {
			if (inherit) {
				query = "MATCH (s:Structure)<-[:HAS_ATTACHMENT*0..]-(so:Structure)<-[:DEPENDS]-(pg:ProfileGroup) ";
			} else {
				query = "MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup) ";
			}
			query +="WHERE s.id IN {structures} " +
					"WITH COLLECT(pg.id) as groupIds " +
					"MATCH (g1:ProfileGroup)<-[:IN]-(u1:User)-[r:DUPLICATE]->(u2:User)-[:IN]->(g2:ProfileGroup) " +
					"WHERE g1.id IN groupIds AND g2.id IN groupIds " +
					"MATCH (s1:Structure)<-[:DEPENDS]-(g1) " + filter +
					"OPTIONAL MATCH (s2:Structure)<-[:DEPENDS]-(g2) ";
			query +="RETURN r.score as score, " +
					"{id: u1.id, firstName: u1.firstName, lastName: u1.lastName, birthDate: u1.birthDate, email: u1.email, profiles: u1.profiles, structures: collect(distinct s1.id)} as user1, " +
					"{id: u2.id, firstName: u2.firstName, lastName: u2.lastName, birthDate: u2.birthDate, email: u2.email, profiles: u2.profiles, structures: collect(distinct s2.id)} as user2 " +
					"ORDER BY score DESC";
		} else {
			if (inSameStructure) {
				query = "match (s:Structure)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u1:User)-[r:DUPLICATE]-(u2:User) WHERE u2-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s) " ;
			} else {
				query = "MATCH (u1:User)-[r:DUPLICATE]->(u2:User) ";
			}
			query += filter + "RETURN r.score as score, " +
					"{id: u1.id, firstName: u1.firstName, lastName: u1.lastName, birthDate: u1.birthDate, email: u1.email, profiles: u1.profiles} as user1, " +
					"{id: u2.id, firstName: u2.firstName, lastName: u2.lastName, birthDate: u2.birthDate, email: u2.email, profiles: u2.profiles} as user2 " +
					"ORDER BY score DESC";
		}
		JsonObject params = new JsonObject().put("structures", structures).put("minScore", minScore);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				message.reply(event.body());
			}
		});
	}

	public void mergeDuplicate(final Message<JsonObject> message) {
		mergeDuplicate(message, null);
	}

	public void mergeDuplicate(final Message<JsonObject> message, final TransactionHelper tx) {
		String userId1 = message.body().getString("userId1");
		String userId2 = message.body().getString("userId2");
		if (userId1 == null || userId2 == null || userId1.trim().isEmpty() || userId2.trim().isEmpty()) {
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.id"));
			return;
		}
		String query =
				"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}) " +
				"RETURN DISTINCT u1.id as userId1, u1.source as source1, NOT(HAS(u1.activationCode)) as activatedU1, " +
				"u1.disappearanceDate as disappearanceDate1, u1.deleteDate as deleteDate1, " +
				"u2.id as userId2, u2.source as source2, NOT(HAS(u2.activationCode)) as activatedU2, " +
				"u2.disappearanceDate as disappearanceDate2, u2.deleteDate as deleteDate2";
		JsonObject params = new JsonObject().put("userId1", userId1).put("userId2", userId2);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				JsonObject error = new JsonObject().put("status", "error");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					JsonObject r = res.getJsonObject(0);
					if (r.getBoolean("activatedU1", true) && r.getBoolean("activatedU2", true)) {
						message.reply(error.put("message", "two.users.activated"));
					} else {
						mergeDuplicate(r, message, tx);
					}
				} else if ("ok".equals(event.body().getString("status"))) {
					message.reply(error.put("message", "not.found.duplicate"));
				} else {
					message.reply(event.body());
				}
			}
		});
	}

	private void sendMergedEvent(String keepedUserId, String deletedUserId) {
		JsonObject body = new JsonObject().put("keepedUserId", keepedUserId).put("deletedUserId", deletedUserId);
		eb.publish(Feeder.USER_REPOSITORY, body.copy().put("action", "merge-users"));
		eventStore.createAndStoreEvent(Feeder.FeederEvent.MERGE_USER.name(), (UserInfos) null, body);

	}

	private void mergeDuplicate(final JsonObject r, final Message<JsonObject> message, final TransactionHelper tx) {
		final String source1 = r.getString("source1");
		final String source2 = r.getString("source2");
		final boolean activatedU1 = r.getBoolean("activatedU1", false);
		final boolean activatedU2 = r.getBoolean("activatedU2", false);
		final String userId1 = r.getString("userId1");
		final String userId2 = r.getString("userId2");
		final boolean missing1 = r.getLong("disappearanceDate1") != null || r.getLong("deleteDate1") != null;
		final boolean missing2 = r.getLong("disappearanceDate2") != null || r.getLong("deleteDate2") != null;
		final JsonObject error = new JsonObject().put("status", "error");
		if (source1 != null && source1.equals(source2) && notDeduplicateSource.contains(source1) &&
				!missing1 && !missing2) {
			message.reply(error.put("message", "two.users.in.same.source"));
			return;
		}
		String query;
		JsonObject params = new JsonObject();
		if ((activatedU1 && prioritySource(source1) >= prioritySource(source2)) ||
				(activatedU2 && prioritySource(source1) <= prioritySource(source2)) ||
				(!activatedU1 && !activatedU2)) {
			query = SIMPLE_MERGE_QUERY;
			if (prioritySource(source1) == prioritySource(source2) && notDeduplicateSource.contains(source1)) {
				if (!missing1 && activatedU1) {
					params.put("userId1", userId1).put("userId2", userId2);
				} else if (!missing2 && activatedU2) {
					params.put("userId1", userId2).put("userId2", userId1);
				} else {
					query = SWITCH_MERGE_QUERY;
					if (activatedU1) {
						params.put("userId1", userId1).put("userId2", userId2);
					} else {
						params.put("userId1", userId2).put("userId2", userId1);
					}
				}
			} else {
				if (activatedU1) {
					params.put("userId1", userId1).put("userId2", userId2);
				} else if (activatedU2) {
					params.put("userId1", userId2).put("userId2", userId1);
				} else {
					if (prioritySource(source1) > prioritySource(source2)) {
						params.put("userId1", userId1).put("userId2", userId2);
					} else {
						params.put("userId1", userId2).put("userId2", userId1);
					}
				}
			}
		} else if ((activatedU1 && prioritySource(source1) < prioritySource(source2)) ||
				(activatedU2 && prioritySource(source1) > prioritySource(source2))) {
			query = SWITCH_MERGE_QUERY;
			if (activatedU1) {
				params.put("userId1", userId1).put("userId2", userId2);
			} else {
				params.put("userId1", userId2).put("userId2", userId1);
			}
		} else {
			message.reply(error.put("message", "invalid.merge.case"));
			return;
		}
		if (tx != null) {
			tx.add(INCREMENT_RELATIVE_SCORE, params);
			tx.add(query, params);
			sendMergedEvent(params.getString("userId1"), params.getString("userId2"));
			message.reply(new JsonObject().put("status", "ok"));
		} else {
			try {
				TransactionHelper txl = TransactionManager.getTransaction();
				txl.add(INCREMENT_RELATIVE_SCORE, params);
				txl.add(query, params);
				txl.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
						  log.info("Merge duplicates : " + r.encode());
							if (updateCourses) {
								AbstractTimetableImporter.updateMergedUsers(event.body().getJsonArray("results"));
							}
							sendMergedEvent(params.getString("userId1"), params.getString("userId2"));
						}
						message.reply(event.body());
					}
				});
			} catch (TransactionException e) {
				message.reply(error.put("message", "invalid.transaction"));
			}
		}
	}

	public void mergeBykeys(final Message<JsonObject> message) {
		final JsonObject error = new JsonObject().put("status", "error");
		final String originalUserId = message.body().getString("originalUserId");
		if (originalUserId == null || originalUserId.isEmpty()) {
			message.reply(error.put("message", "invalid.original.user"));
			return;
		}
		final JsonArray mergeKeys = message.body().getJsonArray("mergeKeys");
		if (mergeKeys == null || mergeKeys.size() < 1) {
			message.reply(error.put("message", "invalid.merge.keys"));
			return;
		}
		final JsonObject params = new JsonObject()
				.put("userId", originalUserId);
		TransactionManager.getNeo4jHelper().execute(
				"MATCH (u:User {id: {userId}}) RETURN u.mergeKey as mergeKey", params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonArray result = event.body().getJsonArray("result");
						if ("ok".equals(event.body().getString("status")) && result.size() == 1) {
							String mergeKey = result.getJsonObject(0).getString("mergeKey");
							if (mergeKey != null && mergeKeys.contains(mergeKey)) {
								final JsonArray tmp = new fr.wseduc.webutils.collections.JsonArray();
								for (Object o : mergeKeys) {
									if (!mergeKey.equals(o)) {
										tmp.add(o);
									}
								}
								if (tmp.size() > 0) {
									params.put("mergeKeys", tmp);
								} else {
									message.reply(error.put("message", "invalid.merge.keys"));
									return;
								}
							} else {
								params.put("mergeKeys", mergeKeys);
							}
							try {
								TransactionHelper tx = TransactionManager.getTransaction();

								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User)-[rin:IN]->(gin:Group) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " +
												"MERGE u-[:IN]->gin " +
												"DELETE rin ", params);
								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User)-[rcom:COMMUNIQUE]->(gcom:Group) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " +
												"MERGE  u-[:COMMUNIQUE]->gcom " +
												"DELETE rcom ", params);
								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User)<-[rcomr:COMMUNIQUE]-(gcomr:Group) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " +
												"MERGE u<-[:COMMUNIQUE]-gcomr " +
												"DELETE rcomr ", params);
								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User)-[rr:RELATED]->(ur:User) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " +
												"MERGE u-[:RELATED]->ur " +
												"DELETE rr ", params);
								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User)<-[rrr:RELATED]-(urr:User) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " +
												"MERGE u<-[:RELATED]-urr " +
												"DELETE rrr ", params);
								tx.add(
										"MATCH (u:User {id: {userId}}), (mu:User) " +
												"WHERE HEAD(u.profiles) = 'Relative' AND HEAD(mu.profiles) = 'Relative' " +
												"AND NOT(HAS(u.mergedWith)) AND mu.mergeKey IN {mergeKeys} " + // AND LENGTH(mu.joinKey) < 2  " +
												"SET mu.mergedWith = {userId}, mu.mergeKey = null, u.mergedLogins = coalesce(u.mergedLogins, []) + mu.login " +
//					", u.joinKey =  FILTER(eId IN u.joinKey WHERE eId <> mu.externalId) + mu.externalId " +
												"MERGE mu-[:MERGED]->u " +
												"RETURN u.mergedLogins as mergedLogins ", params);
								tx.commit(new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										message.reply(event.body());
									}
								});
							} catch (TransactionException e) {
								log.error("transaction.error", e);
								message.reply(error.put("message", "transaction.error"));
							}
						} else {
							message.reply(error.put("message", "user.not.found"));
						}
					}
				});

	}

	private int prioritySource(String source) {
		Integer priority = sourcePriority.get(source);
		return (priority != null) ? priority : 0;
	}

	private void searchDuplicatesByProfile(String last, final String profile, final Handler<Void> handler) {
		String query =
				"MATCH (u:User) WHERE u.modified > {lastSearchDuplicate} AND HEAD(u.profiles) = {profile} AND NOT(HAS(u.deleteDate)) " +
				"RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, " +
						"u.birthDate as birthDate, u.email as email, u.source as source, u.disappearanceDate as disappearanceDate";
		JsonObject params = new JsonObject().put("profile", profile).put("lastSearchDuplicate", last);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray result = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && result != null && result.size() > 0) {
					scoreDuplicates(profile, result, handler);
				} else {
					if ("ok".equals(event.body().getString("status"))) {
						log.info("No users findings for search duplicates");
					} else {
						log.error("Error finding users for search duplicates : " + event.body().getString("message"));
					}
					handler.handle(null);
				}
			}
		});
	}

	private void scoreDuplicates(final String profile, final JsonArray search, final Handler<Void> handler) {
		final String query =
				"START u=node:node_auto_index({luceneQuery}) " +
				"WHERE HEAD(u.profiles) = {profile} AND u.id <> {id} AND NOT(HAS(u.deleteDate)) " +
				"RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, " +
				"u.birthDate as birthDate, u.email as email, u.source as source, u.disappearanceDate as disappearanceDate";
		final JsonObject params = new JsonObject().put("profile", profile);
		TransactionHelper tx;
		try {
			tx = TransactionManager.getTransaction(false);
		} catch (TransactionException e) {
			log.error("Error when find duplicate users.", e);
			return;
		}
		final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
		for (int i = 0; i < search.size(); i++) {
			final JsonObject json = search.getJsonObject(i);
			final String firstNameAttr = luceneAttribute("firstName", json.getString("firstName"), 0.6);
			final String lastNameAttr = luceneAttribute("lastName", json.getString("lastName"), 0.6);
			String luceneQuery;
			if (firstNameAttr != null && lastNameAttr != null &&
					!firstNameAttr.trim().isEmpty() && !lastNameAttr.trim().isEmpty()) {
				luceneQuery = firstNameAttr + " AND " + lastNameAttr;
				result.add(json);
				tx.add(query, params.copy().put("luceneQuery", luceneQuery).put("id", json.getString("id")));
			}
		}
		tx.commit(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() > 0) {
					TransactionHelper tx;
					try {
						tx = TransactionManager.getTransaction();
						tx.setAutoSend(false);
					} catch (TransactionException e) {
						log.error("Error when score duplicate users.", e);
						return;
					}
					for (int i = 0; i < results.size(); i++) {
						JsonArray findUsers = results.getJsonArray(i);
						if (findUsers == null || findUsers.size() == 0) continue;
						JsonObject searchUser = result.getJsonObject(i);
						calculateAndStoreScore(searchUser, findUsers, tx);
					}
					if (!tx.isEmpty()) {
						tx.commit(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status"))) {
									log.info("Mark duplicates " + profile + " finished.");
								} else {
									log.error("Error marking duplicates : " + event.body().getString("message"));
								}
								handler.handle(null);
							}
						});
					} else {
						log.info("No duplicate user with score > 3 found in profile " + profile);
						handler.handle(null);
					}
				} else {
					if ("ok".equals(event.body().getString("status"))) {
						log.info("No duplicate user found in profile " + profile);
					} else {
						log.error("Error finding users for search duplicates : " + event.body().getString("message"));
					}
					handler.handle(null);
				}
			}
		});
	}

	private String luceneAttribute(String attributeName, String value, double distance) {
		if (value == null || value.trim().isEmpty() || attributeName == null || attributeName.trim().isEmpty()) {
			return "";
		}
		String d = (distance > 0.9 || distance < 0.1) ? "" : ("~" + distance);
		StringBuilder sb = new StringBuilder().append("(");
		String[] values = StringValidation.removeAccents(value).split("\\s+");
		for (String v : values) {
			if (v.startsWith("-")) {
				v = v.replaceFirst("-+", "");
			}
			v = v.replaceAll("\\W+", "");
			if (v.isEmpty() || (v.length() < 4 && values.length > 1)) continue;
			if ("OR".equalsIgnoreCase(v) || "AND".equalsIgnoreCase(v) || "NOT".equalsIgnoreCase(v)) {
				v = "\"" + v + "\"";
			}
			sb.append(attributeName).append(":").append(v).append(d).append(" OR ");
		}
		int len = sb.length();
		if (len == 1) {
			return "";
		}
		sb.delete(len - 4, len);
		return sb.append(")").toString();
	}

	private void calculateAndStoreScore(JsonObject searchUser, JsonArray findUsers, TransactionHelper tx) {
		String query =
				"MATCH (u:User {id : {sId}}), (d:User {id : {dId}}) " +
				"WHERE NOT({dId} IN coalesce(u.ignoreDuplicates, [])) AND NOT({sId} IN coalesce(d.ignoreDuplicates, [])) " +
				"AND (has(u.activationCode) OR has(d.activationCode)) " +
				"MERGE u-[:DUPLICATE {score:{score}}]-d ";
		JsonObject params = new JsonObject().put("sId", searchUser.getString("id"));

		final String lastName = cleanAttribute(searchUser.getString("lastName"));
		final String firstName = cleanAttribute(searchUser.getString("firstName"));
		final String birthDate = cleanAttribute(searchUser.getString("birthDate"));
		final String email = cleanAttribute(searchUser.getString("email"));
		final String source = searchUser.getString("source");
		final Long disappearanceDate = searchUser.getLong("disappearanceDate");

		for (int i = 0; i < findUsers.size(); i++) {
			int score = 2;
			JsonObject fu = findUsers.getJsonObject(i);
			score += exactMatch(lastName, cleanAttribute(fu.getString("lastName")));
			score += exactMatch(firstName, cleanAttribute(fu.getString("firstName")));
			score += exactMatch(birthDate, cleanAttribute(fu.getString("birthDate")));
			score += exactMatch(email, cleanAttribute(fu.getString("email")));
			if (score > 3 && ((!source.equals(fu.getString("source")) &&
					(notDeduplicateSource.contains(source) ^ notDeduplicateSource.contains(fu.getString("source")))) ||
					disappearanceDate != null || fu.getLong("disappearanceDate") != null)) {
				tx.add(query, params.copy().put("dId", fu.getString("id")).put("score", score));
			}
		}
	}

	private int exactMatch(String attribute0, String attribute1) {
		return (attribute0 == null || attribute1 == null || !attribute0.equals(attribute1)) ? 0 : 1;
	}

	private String cleanAttribute(String attribute) {
		if (attribute == null || attribute.trim().isEmpty()) {
			return null;
		}
		return Validator.removeAccents(attribute).replaceAll("\\s+", "").toLowerCase();
	}

	public void autoMergeDuplicatesInStructure(final Handler<AsyncResult<JsonArray>> handler) {
		final Handler<JsonObject> duplicatesHandler = new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject duplicatesRes) {
				JsonArray res = duplicatesRes.getJsonArray("result");
				if ("ok".equals(duplicatesRes.getString("status")) && res != null && res.size() > 0) {
					try {
						final TransactionHelper tx = TransactionManager.getTransaction();
						final AtomicInteger count = new AtomicInteger(res.size());
						final Handler<JsonObject> mergeHandler = new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject event) {
								decrementCount(count, tx);
							}
						};
						for (Object o : res) {
							if (!(o instanceof JsonObject)) {
								decrementCount(count, tx);
								continue;
							}
							JsonObject j = (JsonObject) o;
							JsonObject u1 = j.getJsonObject("user1");
							JsonObject u2 = j.getJsonObject("user2");
							if (u1 != null && u2 != null) {
								mergeDuplicate(new ResultMessage(mergeHandler)
										.put("userId1", u1.getString("id"))
										.put("userId2", u2.getString("id")), tx);
								log.info("AutoMerge duplicates - u1 : " + u1.encode() + ", u2 : " + u2.encode());
							} else {
								decrementCount(count, tx);
							}
						}
					} catch (TransactionException e) {
						log.error("Error in automerge transaction", e);
						handler.handle(new DefaultAsyncResult<JsonArray>(e));
					}
				} else {
					log.info("No duplicates automatically mergeable.");
					handler.handle(new DefaultAsyncResult<>(new fr.wseduc.webutils.collections.JsonArray()));
				}
			}

			private void decrementCount(AtomicInteger count, TransactionHelper tx) {
				if (count.decrementAndGet() == 0) {
					tx.commit(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								if (updateCourses) {
									AbstractTimetableImporter.updateMergedUsers(event.body().getJsonArray("results"));
								}
								handler.handle(new DefaultAsyncResult<>(event.body().getJsonArray("results")));
							} else {
								log.error("Error in automerge duplicates transaction : " + event.body().getString("message"));
								handler.handle(new DefaultAsyncResult<JsonArray>(
										new TransactionException(event.body().getString("message"))));
							}
						}
					});
				}
			}
		};
		listDuplicates(new ResultMessage(duplicatesHandler).put("minScore", 5)
				.put("inSameStructure", autoMergeOnlyInSameStructure).put("inherit", false));
	}

	void mergeSameINE(boolean execute, final Handler<AsyncResult<Void>> handler) {
		if (!execute) {
			handler.handle(new DefaultAsyncResult<>((Void) null));
			return;
		}
		final String searchDuplicateIneUsers =
				"MATCH (u:User) " +
				"WHERE has(u.ine) " +
				"WITH u.ine as ine, COLLECT(DISTINCT {id: u.id, source: u.source, disappearanceDate: u.disappearanceDate, " +
				"deleteDate: u.deleteDate, activationCode: u.activationCode, created: u.created, " +
				"login : u.login, externalId : u.externalId }) as users " +
				"WHERE LENGTH(users) > 1 " +
				"RETURN ine, users ";

		TransactionManager.getNeo4jHelper().execute(searchDuplicateIneUsers, new JsonObject(), r -> {
			if ("ok".equals(r.body().getString("status"))) {
				final Comparator<JsonObject> userComparator = new UserComparator();
				final long now = System.currentTimeMillis();
				final TransactionHelper tx;
				try {
					tx = TransactionManager.getTransaction();
				} catch (TransactionException e) {
					log.error("Error beginning merge same INE transaction.", e);
					handler.handle(new DefaultAsyncResult<>(e));
					return;
				}
				for (Object o : getOrElse(r.body().getJsonArray("result"), new JsonArray())) {
					if (!(o instanceof JsonObject)) continue;
					final String ine = ((JsonObject) o).getString("ine");
					final List<JsonObject> users = (List) getOrElse(((JsonObject) o).getJsonArray("users"), new JsonArray())
							.stream().map(e -> (e instanceof Map) ? new JsonObject((Map) e) : e).collect(Collectors.toList());
					if (users.isEmpty()) continue;
					users.sort(userComparator);

					final JsonObject principalUser = users.remove(0);
					if (principalUser.getLong("disappearanceDate") != null ||
							principalUser.getLong("deleteDate") != null) {
						log.warn("All " + users.size() + " users with ine " + ine + " has disappearanceDate or deleteDate.");
						continue;
					}
					final String principalSource = principalUser.getString("source");
					final boolean principalActivated = isEmpty(principalUser.getString("activationCode"));
					for (Object o2 :  users) {
						if (!(o2 instanceof JsonObject)) continue;
						final JsonObject oldUser = (JsonObject) o2;
						if (!principalActivated && isEmpty(oldUser.getString("activationCode"))) {
							mergeDuplicateIneUser(ine, now, principalUser, oldUser, false, tx);
						} else if (!principalSource.equals(oldUser.getString("source")) ||
								oldUser.getLong("disappearanceDate") != null ||
								oldUser.getLong("deleteDate") != null) {
							deleteDuplicateIneUser(oldUser, tx);
						}
					}
				}
				if (tx.isEmpty()) {
					handler.handle(new DefaultAsyncResult<>((Void) null));
				} else {
					tx.commit(res -> {
						if ("ok".equals(res.body().getString("status"))) {
							mergeSameRelative(now, handler);
							//handler.handle(new DefaultAsyncResult<>((Void) null));
						} else {
							final String err = res.body().getString("message");
							log.error("Error when commit merge same INE transaction : " + err);
							handler.handle(new DefaultAsyncResult<>(new TransactionException(err)));
						}
					});
				}
			} else {
				log.error("Error searching user with same INE : " + r.body().getString("message"));
				handler.handle(new DefaultAsyncResult<>(new TransactionException(r.body().getString("message"))));
			}
		});
	}

	private void mergeDuplicateIneUser(String ine, long now, JsonObject principalUser, JsonObject oldUser,
			boolean relative, TransactionHelper tx) {
		final JsonObject params = new JsonObject()
				.put("id", principalUser.getString("id")).put("oldId", oldUser.getString("id"));
		final String query1 =
				"MATCH (old:User {id: {oldId}})-[r:USERBOOK]->(ub:UserBook), (u:User {id: {id}}) " +
				"SET ub.theme = null " +
				"CREATE UNIQUE u-[:USERBOOK]->ub " +
				"DELETE r";
		tx.add(query1, params);
		final String query2 =
				"MATCH (old:User {id: {oldId}})-[r:PREFERS]->(ub:UserAppConf), (u:User {id: {id}}) " +
				"SET ub.theme = null " +
				"CREATE UNIQUE u-[:PREFERS]->ub " +
				"DELETE r";
		tx.add(query2, params);
		if (!relative) {
			final String query3 =
					"MATCH (old:User {id: {oldId}})-[r:RELATED]->(ub:User), (u:User {id: {id}}) " +
					"SET u.mergeIneDate = {now} " +
					"CREATE UNIQUE u-[:RELATED {source:'MERGE_INE'}]->ub " +
					"DELETE r";
			tx.add(query3, params.copy().put("now", now));
		}
		final String query4 =
				"MATCH (old:User {id: {oldId}}) " +
				"SET old.oldId = old.id, old.id = null, old.oldLogin = old.login, old.login = null " +
				"WITH old " +
				"MATCH (u:User {id: {id}}) " +
				"SET u.oldId = u.id, u.id = old.oldId, u.oldLogin = u.login, u.login = old.oldLogin, " +
				"u.activationCode = null, u.password = old.password, u.email = old.email " +
				"WITH old " +
				"OPTIONAL MATCH old-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH old-[r]-() " +
				"DELETE r, rb, b, old ";
		tx.add(query4, params);
		log.info("Merge duplicate INE " + ine + ".\nOld user : " + oldUser.encode() + "\nNew user : " + principalUser.encode());
	}

	private void deleteDuplicateIneUser(JsonObject oldUser, TransactionHelper tx) {
		final String query =
				"MATCH (u:User {id: {id}}) " +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE r, rb, b, u ";
		final JsonObject params = new JsonObject().put("id", oldUser.getString("id"));
		log.info("Remove duplicate ine user : " + oldUser.encode());
		tx.add(query, params);
	}


	private void mergeSameRelative(long now, Handler<AsyncResult<Void>> handler) {
		final String query =
				"MATCH (u:User {mergeIneDate:{now}})-[r:RELATED]->(p:User) " +
				"OPTIONAL MATCH p<-[:RELATED]-(n:User) " +
				"WHERE NOT(HAS(n.mergeIneDate)) " +
				"WITH u, r, p, COUNT(DISTINCT n) as otherChildCount " +
				"RETURN u.id as id, u.ine as ine, COLLECT(DISTINCT {id: p.id, source: p.source, " +
				"disappearanceDate: p.disappearanceDate, deleteDate: p.deleteDate, activationCode: p.activationCode, " +
				"created: p.created, name : p.lastNameSearchField + p.firstNameSearchField, login : p.login, " +
				"externalId : p.externalId, relSource : r.source, otherChildCount : otherChildCount }) as relatives ";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("now", now), r -> {
			if ("ok".equals(r.body().getString("status"))) {
				final JsonArray a = getOrElse(r.body().getJsonArray("result"), new JsonArray());
				final TransactionHelper tx;
				try {
					tx = TransactionManager.getTransaction();
				} catch (TransactionException e) {
					log.error("Error beginning merge same INE transaction.", e);
					handler.handle(new DefaultAsyncResult<>(e));
					return;
				}
				final Comparator<Object> comparator = new RelativeMergeSameRelativeComparator();
				for (Object o : a) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject j = (JsonObject) o;
					final JsonArray deleteRelatives = new JsonArray(j.getJsonArray("relatives").stream()
							.filter(i -> ((JsonObject)i).getInteger("otherChildCount") == 0 &&
									"MERGE_INE".equals(((JsonObject)i).getString("relSource")) &&
									isNotEmpty(((JsonObject)i).getString("activationCode")))
							.map(i -> ((JsonObject)i).getString("id"))
							.collect(Collectors.toList()));
					deleteOldRelativesUsers(deleteRelatives, tx);
					final JsonArray removeLinks = new JsonArray(j.getJsonArray("relatives").stream()
							.filter(i -> ((JsonObject)i).getInteger("otherChildCount") > 0)
							.map(i -> ((JsonObject)i).getString("id"))
							.collect(Collectors.toList()));
					removeLinksMergeIne(j.getString("id"), removeLinks, tx);
					final Map<String, List<Object>> map = j.getJsonArray("relatives").stream()
							.filter(i -> !deleteRelatives.contains(((JsonObject)i).getString("id")) &&
									((JsonObject)i).getInteger("otherChildCount") == 0)
							.collect(Collectors.groupingBy(x -> ((JsonObject) x).getString("name")));
					mergeDuplicateIneRelatives(map, comparator, tx);
				}
				if (tx.isEmpty()) {
					handler.handle(new DefaultAsyncResult<>((Void) null));
				} else {
					tx.commit(res -> {
						if ("ok".equals(res.body().getString("status"))) {
							handler.handle(new DefaultAsyncResult<>((Void) null));
						} else {
							final String err = res.body().getString("message");
							log.error("Error when commit merge relatives same INE transaction : " + err);
							handler.handle(new DefaultAsyncResult<>(new TransactionException(err)));
						}
					});
				}
			} else {
				log.error("Error searching relative with child same INE : " + r.body().getString("message"));
				handler.handle(new DefaultAsyncResult<>(new TransactionException(r.body().getString("message"))));
			}
		});

	}

	private void mergeDuplicateIneRelatives(Map<String, List<Object>> map, Comparator<Object> c, TransactionHelper tx) {
		for (Map.Entry<String, List<Object>> e : map.entrySet()) {
			if (e.getValue().size() < 2) continue;
			e.getValue().sort(c);
			log.info("Sort details relatives : " + new JsonArray(e.getValue()).encode());
			final JsonObject principalUser = (JsonObject) e.getValue().remove(0);
			final boolean principalActivated = isEmpty(principalUser.getString("activationCode"));
			final String principalSource = principalUser.getString("source");
			for (Object o2 :  e.getValue()) {
				if (!(o2 instanceof JsonObject)) continue;
				final JsonObject oldUser = (JsonObject) o2;
				if (!principalActivated && isEmpty(oldUser.getString("activationCode"))) {
					mergeDuplicateIneUser("relative", 0, principalUser, oldUser, true, tx);
				} else if (!principalSource.equals(oldUser.getString("source")) ||
						oldUser.getLong("disappearanceDate") != null ||
						oldUser.getLong("deleteDate") != null) {
					deleteDuplicateIneUser(oldUser, tx);
				}
			}
		}
	}

	private void removeLinksMergeIne(String id, JsonArray removeLinks, TransactionHelper tx) {
		if (removeLinks == null || removeLinks.isEmpty()) return;
		final String query =
				"MATCH (u:User {id: {id}})-[r:RELATED {source: 'MERGE_INE'}]->(p:User) " +
				"WHERE p.id IN {relatives} " +
				"DELETE r ";
		final JsonObject params = new JsonObject().put("id", id).put("relatives", removeLinks);
		log.info("Remove link merge INE user relative : " + removeLinks.encode());
		tx.add(query, params);
	}

	private void deleteOldRelativesUsers(JsonArray deleteRelatives, TransactionHelper tx) {
		if (deleteRelatives == null || deleteRelatives.isEmpty()) return;
		final String query =
				"MATCH (u:User) " +
				"WHERE u.id IN {relatives} " +
				"OPTIONAL MATCH u-[rb:HAS_RELATIONSHIPS]->(b:Backup) " +
				"OPTIONAL MATCH u-[r]-() " +
				"DELETE r, rb, b, u ";
		final JsonObject params = new JsonObject().put("relatives", deleteRelatives);
		log.info("Remove duplicate ine user relatives : " + deleteRelatives.encode());
		tx.add(query, params);
	}

	private class RelativeMergeSameRelativeComparator implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			final JsonObject j1 = (JsonObject) o1;
			final JsonObject j2 = (JsonObject) o2;
			if (isEmpty(j1.getString("relSource"))) {
				if (isNotEmpty(j2.getString("relSource"))) {
					return -1;
				} else {
					return getOrElse(j2.getString("created"), "0")
							.compareTo(getOrElse(j1.getString("created"), "0"));
				}
			} else {
				if (isEmpty(j2.getString("relSource"))) {
					return 1;
				} else {
					return getOrElse(j2.getString("created"), "0")
							.compareTo(getOrElse(j1.getString("created"), "0"));
				}
			}
		}

	}

	private class UserComparator implements Comparator<JsonObject> {

		@Override
		public int compare(JsonObject o1, JsonObject o2) {
			if (o1.getLong("disappearanceDate") == null && o1.getLong("deleteDate") == null) {
				if (o2.getLong("disappearanceDate") == null && o2.getLong("deleteDate") == null) {
					return compareSourceAndCreated(o1, o2);
				} else {
					return 1;
				}
			} else {
				if (o2.getLong("disappearanceDate") == null && o2.getLong("deleteDate") == null) {
					return -1;
				} else {
					return compareSourceAndCreated(o1, o2);
				}
			}
		}

		private int compareSourceAndCreated(JsonObject o1, JsonObject o2) {
			final int c = sourcePriority.get(o2.getString("source"))
					.compareTo(sourcePriority.get(o1.getString("source")));
			return (c != 0) ? c : o2.getString("created").compareTo(o1.getString("created"));
		}

	}

}
