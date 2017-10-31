/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.feeder.dictionary.structures;

import fr.wseduc.webutils.DefaultAsyncResult;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.joda.time.DateTime;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DuplicateUsers {

	private static final Logger log = LoggerFactory.getLogger(DuplicateUsers.class);
	private static final String INCREMENT_RELATIVE_SCORE =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}), " +
			"(u1)-[:RELATED]->()-[rp:DUPLICATE]-()<-[:RELATED]-(u2) " +
			"SET rp.score = rp.score + 1 ";
	private static final String SIMPLE_MERGE_QUERY =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}})-[r2]-() " +
			"SET u1.ignoreDuplicates = FILTER(uId IN u1.ignoreDuplicates WHERE uId <> {userId2}) " +
			"WITH u1, u2, r, r2, u2.IDPN as IDPN, u2.id as oldId " +
			"DELETE r, r2, u2 " +
			"WITH u1, IDPN, oldId " +
			"WHERE NOT(HAS(u1.IDPN)) AND NOT(IDPN IS NULL) " +
			"SET u1.IDPN = IDPN " +
			"RETURN DISTINCT oldId, u1.id as id, HEAD(u1.profiles) as profile ";
	private static final String SWITCH_MERGE_QUERY =
			"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}})-[r2]-() " +
			"WITH u1, u2, r, r2, u2.source as source, u2.externalId as externalId, u2.IDPN as IDPN, u2.id as oldId " +
			"DELETE r, r2, u2 " +
			"WITH u1, source, externalId, IDPN, oldId " +
			"SET u1.ignoreDuplicates = FILTER(uId IN u1.ignoreDuplicates WHERE uId <> {userId2}), " +
			"u1.externalId = externalId, u1.source = source, u1.disappearanceDate = null " +
			"WITH u1, IDPN, oldId " +
			"WHERE NOT(HAS(u1.IDPN)) AND NOT(IDPN IS NULL) " +
			"SET u1.IDPN = IDPN " +
			"RETURN DISTINCT oldId, u1.id as id, HEAD(u1.profiles) as profile ";
	private final List<String> notDeduplicateSource = Arrays.asList("AAF", "AAF1D");
	private final Map<String, Integer> sourcePriority = new HashMap<>();
	private final boolean updateCourses;
	private final boolean autoMergeOnlyInSameStructure;

	public DuplicateUsers(boolean updateCourses, boolean autoMergeOnlyInSameStructure) {
		this(null, updateCourses, autoMergeOnlyInSameStructure);
	}

	public DuplicateUsers(JsonArray sourcesPriority, boolean updateCourses, boolean autoMergeOnlyInSameStructure) {
		if (sourcesPriority == null) {
			sourcesPriority = new JsonArray().add("AAF").add("AAF1D").add("CSV").add("EDT").add("UDT").add("MANUAL");
		}
		final int size = sourcesPriority.size();
		for (int i = 0; i < size; i++) {
			sourcePriority.put(sourcesPriority.<String>get(i), size - i);
		}
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
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null &&
						res.size() == 1 && res.<JsonObject>get(0).getString("lastSearchDuplicates") != null) {
					final String last = res.<JsonObject>get(0).getString("lastSearchDuplicates");
					final String[] profiles = ManualFeeder.profiles.keySet().toArray(new String[ManualFeeder.profiles.keySet().size()]);
					final VoidHandler[] handlers = new VoidHandler[profiles.length + 1];
					final long start = System.currentTimeMillis();
					handlers[handlers.length - 1] = new VoidHandler() {
						@Override
						protected void handle() {
							final String updateDate = "MATCH (s:System {name : 'Starter'}) set s.lastSearchDuplicates = {now} ";
							TransactionManager.getNeo4jHelper().execute(updateDate, new JsonObject().putString("now", now),
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
								message.reply(new JsonObject().putString("status", "ok"));
							}
							if (handler != null) {
								handler.handle(new JsonObject().putString("status", "ok"));
							}
						}
					};
					for (int i = profiles.length - 1; i >= 0; i--) {
						final int j = i;
						handlers[i] = new VoidHandler() {
							@Override
							protected void handle() {
								searchDuplicatesByProfile(last, profiles[j], handlers[j + 1]);
							}
						};
					}
					handlers[0].handle(null);
				} else {
					log.warn("lastSearchDuplicates not found.");
					message.reply(new JsonObject().putString("status", "ok"));
				}
			}
		});
	}

	public void ignoreDuplicate(final Message<JsonObject> message) {
		String userId1 = message.body().getString("userId1");
		String userId2 = message.body().getString("userId2");
		if (userId1 == null || userId2 == null || userId1.trim().isEmpty() || userId2.trim().isEmpty()) {
			message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.id"));
			return;
		}
		String query =
				"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}) " +
				"SET u1.ignoreDuplicates = coalesce(u1.ignoreDuplicates, []) + u2.id, " +
				"u2.ignoreDuplicates = coalesce(u2.ignoreDuplicates, []) + u1.id " +
				"DELETE r";
		JsonObject params = new JsonObject().putString("userId1", userId1).putString("userId2", userId2);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				message.reply(event.body());
			}
		});
	}

	public void listDuplicates(final Message<JsonObject> message) {
		JsonArray structures = message.body().getArray("structures");
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
		JsonObject params = new JsonObject().putArray("structures", structures).putNumber("minScore", minScore);
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
			message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.id"));
			return;
		}
		String query =
				"MATCH (u1:User {id: {userId1}})-[r:DUPLICATE]-(u2:User {id: {userId2}}) " +
				"RETURN DISTINCT u1.id as userId1, u1.source as source1, NOT(HAS(u1.activationCode)) as activatedU1, " +
				"u1.disappearanceDate as disappearanceDate1, u1.deleteDate as deleteDate1, " +
				"u2.id as userId2, u2.source as source2, NOT(HAS(u2.activationCode)) as activatedU2, " +
				"u2.disappearanceDate as disappearanceDate2, u2.deleteDate as deleteDate2";
		JsonObject params = new JsonObject().putString("userId1", userId1).putString("userId2", userId2);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				JsonObject error = new JsonObject().putString("status", "error");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					JsonObject r = res.get(0);
					if (r.getBoolean("activatedU1", true) && r.getBoolean("activatedU2", true)) {
						message.reply(error.putString("message", "two.users.activated"));
					} else {
						mergeDuplicate(r, message, tx);
					}
				} else if ("ok".equals(event.body().getString("status"))) {
					message.reply(error.putString("message", "not.found.duplicate"));
				} else {
					message.reply(event.body());
				}
			}
		});
	}

	private void mergeDuplicate(JsonObject r, final Message<JsonObject> message, final TransactionHelper tx) {
		final String source1 = r.getString("source1");
		final String source2 = r.getString("source2");
		final boolean activatedU1 = r.getBoolean("activatedU1", false);
		final boolean activatedU2 = r.getBoolean("activatedU2", false);
		final String userId1 = r.getString("userId1");
		final String userId2 = r.getString("userId2");
		final boolean missing1 = r.getLong("disappearanceDate1") != null || r.getLong("deleteDate1") != null;
		final boolean missing2 = r.getLong("disappearanceDate2") != null || r.getLong("deleteDate2") != null;
		final JsonObject error = new JsonObject().putString("status", "error");
		if (source1 != null && source1.equals(source2) && notDeduplicateSource.contains(source1) &&
				!missing1 && !missing2) {
			message.reply(error.putString("message", "two.users.in.same.source"));
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
					params.putString("userId1", userId1).putString("userId2", userId2);
				} else if (!missing2 && activatedU2) {
					params.putString("userId1", userId2).putString("userId2", userId1);
				} else {
					query = SWITCH_MERGE_QUERY;
					if (activatedU1) {
						params.putString("userId1", userId1).putString("userId2", userId2);
					} else {
						params.putString("userId1", userId2).putString("userId2", userId1);
					}
				}
			} else {
				if (activatedU1) {
					params.putString("userId1", userId1).putString("userId2", userId2);
				} else if (activatedU2) {
					params.putString("userId1", userId2).putString("userId2", userId1);
				} else {
					if (prioritySource(source1) > prioritySource(source2)) {
						params.putString("userId1", userId1).putString("userId2", userId2);
					} else {
						params.putString("userId1", userId2).putString("userId2", userId1);
					}
				}
			}
		} else if ((activatedU1 && prioritySource(source1) < prioritySource(source2)) ||
				(activatedU2 && prioritySource(source1) > prioritySource(source2))) {
			query = SWITCH_MERGE_QUERY;
			if (activatedU1) {
				params.putString("userId1", userId1).putString("userId2", userId2);
			} else {
				params.putString("userId1", userId2).putString("userId2", userId1);
			}
		} else {
			message.reply(error.putString("message", "invalid.merge.case"));
			return;
		}
		if (tx != null) {
			tx.add(INCREMENT_RELATIVE_SCORE, params);
			tx.add(query, params);
			message.reply(new JsonObject().putString("status", "ok"));
		} else {
			try {
				TransactionHelper txl = TransactionManager.getTransaction();
				txl.add(INCREMENT_RELATIVE_SCORE, params);
				txl.add(query, params);
				txl.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							if (updateCourses) {
								AbstractTimetableImporter.updateMergedUsers(event.body().getArray("results"));
							}
						}
						message.reply(event.body());
					}
				});
			} catch (TransactionException e) {
				message.reply(error.putString("message", "invalid.transaction"));
			}
		}
	}

	public void mergeBykeys(final Message<JsonObject> message) {
		final JsonObject error = new JsonObject().putString("status", "error");
		final String originalUserId = message.body().getString("originalUserId");
		if (originalUserId == null || originalUserId.isEmpty()) {
			message.reply(error.putString("message", "invalid.original.user"));
			return;
		}
		final JsonArray mergeKeys = message.body().getArray("mergeKeys");
		if (mergeKeys == null || mergeKeys.size() < 1) {
			message.reply(error.putString("message", "invalid.merge.keys"));
			return;
		}
		final JsonObject params = new JsonObject()
				.putString("userId", originalUserId);
		TransactionManager.getNeo4jHelper().execute(
				"MATCH (u:User {id: {userId}}) RETURN u.mergeKey as mergeKey", params, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonArray result = event.body().getArray("result");
						if ("ok".equals(event.body().getString("status")) && result.size() == 1) {
							String mergeKey = result.<JsonObject>get(0).getString("mergeKey");
							if (mergeKey != null && mergeKeys.contains(mergeKey)) {
								final JsonArray tmp = new JsonArray();
								for (Object o : mergeKeys) {
									if (!mergeKey.equals(o)) {
										tmp.add(o);
									}
								}
								if (tmp.size() > 0) {
									params.putArray("mergeKeys", tmp);
								} else {
									message.reply(error.putString("message", "invalid.merge.keys"));
									return;
								}
							} else {
								params.putArray("mergeKeys", mergeKeys);
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
								message.reply(error.putString("message", "transaction.error"));
							}
						} else {
							message.reply(error.putString("message", "user.not.found"));
						}
					}
				});

	}

	private int prioritySource(String source) {
		Integer priority = sourcePriority.get(source);
		return (priority != null) ? priority : 0;
	}

	private void searchDuplicatesByProfile(String last, final String profile, final VoidHandler handler) {
		String query =
				"MATCH (u:User) WHERE u.modified > {lastSearchDuplicate} AND HEAD(u.profiles) = {profile} AND NOT(HAS(u.deleteDate)) " +
				"RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, " +
						"u.birthDate as birthDate, u.email as email, u.source as source, u.disappearanceDate as disappearanceDate";
		JsonObject params = new JsonObject().putString("profile", profile).putString("lastSearchDuplicate", last);
		TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray result = event.body().getArray("result");
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

	private void scoreDuplicates(final String profile, final JsonArray search, final VoidHandler handler) {
		final String query =
				"START u=node:node_auto_index({luceneQuery}) " +
				"WHERE HEAD(u.profiles) = {profile} AND u.id <> {id} AND NOT(HAS(u.deleteDate)) " +
				"RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, " +
				"u.birthDate as birthDate, u.email as email, u.source as source, u.disappearanceDate as disappearanceDate";
		final JsonObject params = new JsonObject().putString("profile", profile);
		TransactionHelper tx;
		try {
			tx = TransactionManager.getTransaction(false);
		} catch (TransactionException e) {
			log.error("Error when find duplicate users.", e);
			return;
		}
		final JsonArray result = new JsonArray();
		for (int i = 0; i < search.size(); i++) {
			final JsonObject json = search.get(i);
			final String firstNameAttr = luceneAttribute("firstName", json.getString("firstName"), 0.6);
			final String lastNameAttr = luceneAttribute("lastName", json.getString("lastName"), 0.6);
			String luceneQuery;
			if (firstNameAttr != null && lastNameAttr != null &&
					!firstNameAttr.trim().isEmpty() && !lastNameAttr.trim().isEmpty()) {
				luceneQuery = firstNameAttr + " AND " + lastNameAttr;
				result.add(json);
				tx.add(query, params.copy().putString("luceneQuery", luceneQuery).putString("id", json.getString("id")));
			}
		}
		tx.commit(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray results = event.body().getArray("results");
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
						JsonArray findUsers = results.get(i);
						if (findUsers == null || findUsers.size() == 0) continue;
						JsonObject searchUser = result.get(i);
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
		String[] values = value.split("\\s+");
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
				"MERGE u-[:DUPLICATE {score:{score}}]-d ";
		JsonObject params = new JsonObject().putString("sId", searchUser.getString("id"));

		final String lastName = cleanAttribute(searchUser.getString("lastName"));
		final String firstName = cleanAttribute(searchUser.getString("firstName"));
		final String birthDate = cleanAttribute(searchUser.getString("birthDate"));
		final String email = cleanAttribute(searchUser.getString("email"));
		final String source = searchUser.getString("source");
		final Long disappearanceDate = searchUser.getLong("disappearanceDate");

		for (int i = 0; i < findUsers.size(); i++) {
			int score = 2;
			JsonObject fu = findUsers.get(i);
			score += exactMatch(lastName, cleanAttribute(fu.getString("lastName")));
			score += exactMatch(firstName, cleanAttribute(fu.getString("firstName")));
			score += exactMatch(birthDate, cleanAttribute(fu.getString("birthDate")));
			score += exactMatch(email, cleanAttribute(fu.getString("email")));
			if (score > 3 && (!notDeduplicateSource.contains(source) || !source.equals(fu.getString("source")) ||
					disappearanceDate != null || fu.getLong("disappearanceDate") != null)) {
				tx.add(query, params.copy().putString("dId", fu.getString("id")).putNumber("score", score));
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

	public void autoMergeDuplicatesInStructure(final AsyncResultHandler<JsonArray> handler) {
		final Handler<JsonObject> duplicatesHandler = new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject duplicatesRes) {
				JsonArray res = duplicatesRes.getArray("result");
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
							JsonObject u1 = j.getObject("user1");
							JsonObject u2 = j.getObject("user2");
							if (u1 != null && u2 != null) {
								mergeDuplicate(new ResultMessage(mergeHandler)
										.put("userId1", u1.getString("id"))
										.put("userId2", u2.getString("id")), tx);
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
					handler.handle(new DefaultAsyncResult<>(new JsonArray()));
				}
			}

			private void decrementCount(AtomicInteger count, TransactionHelper tx) {
				if (count.decrementAndGet() == 0) {
					tx.commit(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								if (updateCourses) {
									AbstractTimetableImporter.updateMergedUsers(event.body().getArray("results"));
								}
								handler.handle(new DefaultAsyncResult<>(event.body().getArray("results")));
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

}
