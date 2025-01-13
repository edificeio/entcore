/* Copyright © "Open Digital Education", 2014
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
 */

package org.entcore.communication.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.validation.StringValidation;
import org.entcore.communication.services.CommunicationService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultCommunicationService implements CommunicationService {

	protected final Neo4j neo4j = Neo4j.getInstance();
	private static final String CAN_COMMUNICATE_VERIFY = "canCommunicate";

	protected static final Logger log = LoggerFactory.getLogger(DefaultCommunicationService.class);

	private final TimelineHelper notifyTimeline;
	final JsonArray discoverVisibleExpectedProfile = new JsonArray();

	public DefaultCommunicationService(TimelineHelper notifyTimeline, JsonArray discoverVisibleExpectedProfile) {
		this.notifyTimeline = notifyTimeline;
		this.discoverVisibleExpectedProfile.addAll(discoverVisibleExpectedProfile);
	}

	@Override
	public void addLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (g1:Group {id : {startGroupId}}), (g2:Group {id : {endGroupId}}) " +
						"SET g1.communiqueWith = coalesce(g1.communiqueWith, []) + {endGroupId} " +
						"CREATE UNIQUE g1-[:COMMUNIQUE { source: 'MANUAL'}]->g2 " +
						"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject()
				.put("startGroupId", startGroupId)
				.put("endGroupId", endGroupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (g1:Group {id : {startGroupId}})-[r:COMMUNIQUE]->(g2:Group {id : {endGroupId}}) " +
				"SET g1.communiqueWith = FILTER(gId IN g1.communiqueWith WHERE gId <> {endGroupId}), " +
						"g2.communiqueWith = coalesce(g2.communiqueWith, []) " +
						"DELETE r " +
						"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject()
				.put("startGroupId", startGroupId)
				.put("endGroupId", endGroupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void addLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		if (Direction.NONE.equals(direction)) {
			// With a direction of NONE, only update the 'users' property.
			String query =
					"MATCH (g:Group { id : {groupId}}) " +
							"SET g.users = {direction} " +
							"RETURN 0 as number ";
			JsonObject params = new JsonObject().put("groupId", groupId).put("direction", direction.name());
			neo4j.execute(query, params, validUniqueResultHandler(handler));
		} else {
			String createRelationship;
			switch (direction) {
				case INCOMING:
					createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u ";
					break;
				case OUTGOING:
					createRelationship = "g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
					break;
				default:
					createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u, g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
			}
			String query =
					"MATCH (g:Group { id : {groupId}}) " +
							"SET g.users = {direction} " +
							"WITH g " +
							"MATCH g<-[:IN]-(u:User) " +
							"CREATE UNIQUE " + createRelationship +
							"RETURN COUNT(*) as number ";
			JsonObject params = new JsonObject().put("groupId", groupId).put("direction", direction.name());
			neo4j.execute(query, params, validUniqueResultHandler(handler));
		}
	}

	@Override
	public void addLinkWithUsers(Map<String, Direction> params, Handler<Either<String, JsonObject>> handler) {
		if (params != null) {
			StatementsBuilder sb = new StatementsBuilder();

			params.forEach((groupId, direction) -> {
				if (Direction.NONE.equals(direction)) {
					// With a direction of NONE, only update the 'users' property.
					String query =
							"MATCH (g:Group { id : {groupId}}) " +
									"SET g.users = {direction} ";
					sb.add(query, new JsonObject().put("groupId", groupId).put("direction", direction.name()));
				} else {
					String createRelationship = null;

					switch (direction) {
						case INCOMING:
							createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u ";
							break;
						case OUTGOING:
							createRelationship = "g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
							break;
						default:
							createRelationship = "g<-[:COMMUNIQUE { source: 'MANUAL'}]-u, g-[:COMMUNIQUE { source: 'MANUAL'}]->u ";
							break;
					}
					String query =
							"MATCH (g:Group { id : {groupId}}) " +
									"SET g.users = {direction} " +
									"WITH g " +
									"MATCH g<-[:IN]-(u:User) " +
									"CREATE UNIQUE " + createRelationship;
					sb.add(query, new JsonObject().put("groupId", groupId).put("direction", direction.name()));
				}
			});

			neo4j.executeTransaction(sb.build(), null, true, validUniqueResultHandler(handler));
		} else {
			handler.handle(new Either.Left<>("Error addLinkWithUsers: params can't be null"));
		}
	}

	@Override
	public void removeLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
		String relationship;
		String set;
		switch (direction) {
			case NONE:
				handler.handle(new Either.Right<>(new JsonObject().put("number", 0)));
				return;    // Nothing more to do.
			case INCOMING:
				relationship = "g<-[r:COMMUNIQUE]-(u:User) ";
				set = "SET g.users = CASE WHEN g.users = 'INCOMING' THEN null ELSE 'OUTGOING' END ";
				break;
			case OUTGOING:
				relationship = "g-[r:COMMUNIQUE]->(u:User) ";
				set = "SET g.users = CASE WHEN g.users = 'OUTGOING' THEN null ELSE 'INCOMING' END ";
				break;
			default:
				relationship = "g-[r:COMMUNIQUE]-(u:User) ";
				set = "REMOVE g.users ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
						set +
						"WITH g " +
						"MATCH " + relationship +
						"DELETE r " +
						"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().put("groupId", groupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void communiqueWith(String groupId, Handler<Either<String, JsonObject>> handler) {
//		String optional;
//		switch (filter) {
//			case GROUPS:
//				optional = "OPTIONAL MATCH g-[:COMMUNIQUE]->(vg:Group) ";
//				break;
//			case USERS:
//				optional = "OPTIONAL MATCH g-[:COMMUNIQUE]->(vu:User) ";
//				break;
//			default:
//				optional =
//						"OPTIONAL MATCH g-[:COMMUNIQUE]->(vg:Group) " +
//						"OPTIONAL MATCH g-[:COMMUNIQUE]->(vu:User) ";
//
//		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
						"OPTIONAL MATCH g-[:COMMUNIQUE]->(g1:Group) " +
						"RETURN g as group, COLLECT(g1) as communiqueWith ";
		JsonObject params = new JsonObject().put("groupId", groupId);
		neo4j.execute(query, params, fullNodeMergeHandler("group", handler, "communiqueWith"));
	}

	@Override
	public void addLinkBetweenRelativeAndStudent(String groupId, Direction direction,
												 Handler<Either<String, JsonObject>> handler) {
		String createRelationship;
		switch (direction) {
			case INCOMING:
				createRelationship = "u<-[:COMMUNIQUE_DIRECT]-s ";
				break;
			case OUTGOING:
				createRelationship = "u-[:COMMUNIQUE_DIRECT]->s ";
				break;
			default:
				createRelationship = "u<-[:COMMUNIQUE_DIRECT]-s, u-[:COMMUNIQUE_DIRECT]->s ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}})<-[:IN]-(u:User)<-[:RELATED]-(s:User) " +
						"SET g.relativeCommuniqueStudent = {direction} " +
						"CREATE UNIQUE " + createRelationship +
						"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().put("groupId", groupId).put("direction", direction.name());
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void removeLinkBetweenRelativeAndStudent(String groupId, Direction direction,
													Handler<Either<String, JsonObject>> handler) {
		String relationship;
		String set;
		switch (direction) {
			case INCOMING:
				relationship = "g<-[:IN]-(u:User)<-[r:COMMUNIQUE_DIRECT]-(s:User) ";
				set = "SET g.relativeCommuniqueStudent = " +
						"CASE WHEN g.relativeCommuniqueStudent = 'INCOMING' THEN null ELSE 'OUTGOING' END ";
				break;
			case OUTGOING:
				relationship = "g<-[:IN]-(u:User)-[r:COMMUNIQUE_DIRECT]->(s:User) ";
				set = "SET g.relativeCommuniqueStudent = " +
						"CASE WHEN g.relativeCommuniqueStudent = 'OUTGOING' THEN null ELSE 'INCOMING' END ";
				break;
			default:
				relationship = "g<-[:IN]-(u:User)-[r:COMMUNIQUE_DIRECT]-(s:User) ";
				set = "REMOVE g.relativeCommuniqueStudent ";
		}
		String query =
				"MATCH (g:Group { id : {groupId}}) " +
						"WHERE HAS(g.relativeCommuniqueStudent) " +
						set +
						"WITH g " +
						"MATCH " + relationship +
						"DELETE r " +
						"RETURN COUNT(*) as number ";
		JsonObject params = new JsonObject().put("groupId", groupId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, final Integer transactionId,
								 final Boolean commit, final Handler<Either<String, JsonObject>> handler) {
		final StatementsBuilder s1 = new StatementsBuilder();
		final StatementsBuilder s2 = new StatementsBuilder();
		final StatementsBuilder s3 = new StatementsBuilder();
		s3.add(
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup) " +
						"WHERE NOT(HAS(g.communiqueWith)) " +
						"SET g.communiqueWith = [] "
		).add(
				"MATCH (fg:FunctionGroup) " +
						"WHERE fg.name ENDS WITH 'AdminLocal' " +
						"SET fg.users = 'BOTH' "
		).add(
				"MATCH (ag:FunctionalGroup) " +
						"SET ag.users = 'BOTH' "
		);
		for (String attr : defaultRules.fieldNames()) {
			initDefaultRules(structureIds, attr, defaultRules.getJsonObject(attr), s1, s2);
		}
		neo4j.executeTransaction(s1.build(), transactionId, false, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					Integer transactionId = event.body().getInteger("transactionId");
					neo4j.executeTransaction(s2.build(), transactionId, false, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								Integer transactionId = event.body().getInteger("transactionId");
								neo4j.executeTransaction(s3.build(), transactionId, commit.booleanValue(),
										new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> message) {
												if ("ok".equals(message.body().getString("status"))) {
													handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
													log.info("Default communication rules initialized.");
												} else {
													handler.handle(new Either.Left<String, JsonObject>(
															message.body().getString("message")));
													log.error("Error init default com rules : " +
															message.body().getString("message"));
												}
											}
										});
							} else {
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
								log.error("Error init default com rules : " + event.body().getString("message"));
							}
						}
					});
				} else {
					handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
					log.error("Error init default com rules : " + event.body().getString("message"));
				}
			}
		});
	}

	@Override
	public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules,
								 final Handler<Either<String, JsonObject>> handler) {
		initDefaultRules(structureIds, defaultRules, null, true, handler);
	}

	private void initDefaultRules(JsonArray structureIds, String attr, JsonObject defaultRules,
								  final StatementsBuilder existingGroups, final StatementsBuilder newGroups) {
		final String[] a = attr.split("\\-");
		final String c = "Class".equals(a[0]) ? "*2" : "";
		String relativeStudent = defaultRules.getString("Relative-Student"); // TODO check type in enum
		if (relativeStudent != null && "Relative".equals(a[1])) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup) " +
							"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) " +
							"AND cg.name =~ {profile} " +
							"SET cg.relativeCommuniqueStudent = {direction} ";
			JsonObject params = new JsonObject()
					.put("structures", structureIds)
					.put("direction", relativeStudent)
					.put("profile", "^.*?" + a[1] + "$");
			newGroups.add(query, params);
		}
		String users = defaultRules.getString("users"); // TODO check type in enum
		if (users != null) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup) " +
							"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) " +
							"AND cg.name =~ {profile} " +
							"SET cg.users = {direction} ";
			JsonObject params = new JsonObject()
					.put("structures", structureIds)
					.put("direction", users)
					.put("profile", "^.*?" + a[1] + "$");
			newGroups.add(query, params);
		}
		JsonArray communiqueWith = defaultRules.getJsonArray("communiqueWith", new JsonArray());
		Set<String> classes = new HashSet<>();
		Set<String> structures = new HashSet<>();
		StringBuilder groupLabelSB = new StringBuilder("g:ProfileGroup");
		for (Object o : communiqueWith) {
			if (!(o instanceof String)) continue;
			String[] s = ((String) o).split("\\-");
			if ("Class".equals(s[0]) && "Structure".equals(a[0])) {
				log.warn("Invalid default configuration " + attr + "->" + o.toString());
			} else if ("Class".equals(s[0])) {
				if ("HeadTeacher".equals(s[1])) {
					groupLabelSB.append(" OR g:HTGroup");
				}
				classes.add(s[1]);
			} else {
				if ("Func".equals(s[1]) || "Discipline".equals(s[1])) {
					groupLabelSB.append(" OR g:FunctionGroup");
				} else if ("HeadTeacher".equals(s[1])) {
					groupLabelSB.append(" OR g:HTGroup");
				} else if ("Direction".equals(s[1])) {
					groupLabelSB.append(" OR g:DirectionGroup");
				}
				structures.add(s[1]);
			}
		}
		final String groupLabel = groupLabelSB.toString();
		JsonObject params = new JsonObject()
				.put("structures", structureIds)
				.put("profile", "^.*?" + a[1] + "$");
		if (!classes.isEmpty()) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
					"WHERE s.id IN {structures} AND HAS(cg.communiqueWith) AND cg.name =~ {profile} " +
					"WITH cg, c " +
					"MATCH c<-[:DEPENDS]-(g) " +
					"WHERE (" + groupLabel + ") AND NOT(HAS(g.communiqueWith)) AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = FILTER(gId IN cg.communiqueWith WHERE gId <> g.id) + g.id ";
			String query2 =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
							"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) AND cg.name =~ {profile} " +
							"WITH cg, c, s " +
							"MATCH c<-[:DEPENDS]-(g) " +
							"WHERE  (" + groupLabel + ") AND g.name =~ {otherProfile} " +
							"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + g.id ";
			if (!structures.isEmpty()) {
				query2 +=
						"WITH DISTINCT s, cg " +
								"MATCH s<-[:DEPENDS]-(g) " +
								"WHERE (" + groupLabel + ") AND g.name =~ {structureProfile} " +
								"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + g.id ";
			}
			JsonObject p = params.copy();
			p.put("otherProfile", "^.*?(" + Joiner.on("|").join(classes) + ")$");
			p.put("structureProfile", "^.*?(" + Joiner.on("|").join(structures) + ")$");
			existingGroups.add(query, p);
			newGroups.add(query2, p);
		}
		if (!structures.isEmpty() && "Structure".equals(a[0])) {
			String query =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup), s<-[:DEPENDS]-(g) " +
					"WHERE s.id IN {structures} AND HAS(cg.communiqueWith) AND cg.name =~ {profile} " +
					"AND  (" + groupLabel + ") AND NOT(HAS(g.communiqueWith)) AND g.name =~ {otherProfile} " +
					"SET cg.communiqueWith = FILTER(gId IN cg.communiqueWith WHERE gId <> g.id) + g.id ";
			String query2 =
					"MATCH (s:Structure)<-[:DEPENDS" + c + "]-(cg:ProfileGroup), s<-[:DEPENDS]-(g) " +
							"WHERE s.id IN {structures} AND NOT(HAS(cg.communiqueWith)) AND cg.name =~ {profile} " +
							"AND (" + groupLabel + ") AND g.name =~ {otherProfile} " +
							"SET cg.communiqueWith = coalesce(cg.communiqueWith, []) + g.id ";
			params.put("otherProfile", "^.*?(" + Joiner.on("|").join(structures) + ")$");
			existingGroups.add(query, params);
			newGroups.add(query2, params);
		}
	}

	@Override
	public void applyDefaultRules(JsonArray structureIds, final Integer transactionId, final Boolean commit,
								  Handler<Either<String, JsonObject>> handler) {
		StatementsBuilder s = new StatementsBuilder();
		JsonObject params = new JsonObject().put("structures", structureIds);
		String query =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup) " +
						"WHERE s.id IN {structures} AND HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 " +
						"WITH DISTINCT g " +
						"MATCH (pg:Group) " +
						"WHERE pg.id IN g.communiqueWith " +
						"MERGE g-[:COMMUNIQUE]->pg ";
		s.add(query, params);
		String usersIncoming =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN]-(u:User) " +
						"WHERE s.id IN {structures} AND HAS(g.users) AND (g.users = 'INCOMING' OR g.users = 'BOTH') " +
						"MERGE g<-[:COMMUNIQUE]-u ";
		s.add(usersIncoming, params);
		String usersOutgoing =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN]-(u:User) " +
						"WHERE s.id IN {structures} AND HAS(g.users) AND (g.users = 'OUTGOING' OR g.users = 'BOTH') " +
						"MERGE g-[:COMMUNIQUE]->u ";
		s.add(usersOutgoing, params);
		String relativeIncoming =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
						"WHERE s.id IN {structures} AND HAS(g.relativeCommuniqueStudent) " +
						"AND (g.relativeCommuniqueStudent = 'INCOMING' OR g.relativeCommuniqueStudent = 'BOTH') " +
						"MERGE r<-[:COMMUNIQUE_DIRECT]-u ";
		s.add(relativeIncoming, params);
		String relativeOutgoing =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
						"WHERE s.id IN {structures} AND HAS(g.relativeCommuniqueStudent) " +
						"AND (g.relativeCommuniqueStudent = 'OUTGOING' OR g.relativeCommuniqueStudent = 'BOTH') " +
						"MERGE r-[:COMMUNIQUE_DIRECT]->u ";
		s.add(relativeOutgoing, params);
		String setVisible =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:Group)<-[:IN*0..1]-(v), " +
						"v-[:COMMUNIQUE|COMMUNIQUE_DIRECT]-() " +
						"WHERE s.id IN {structures} AND NOT(v:Visible) " +
						"WITH DISTINCT v " +
						"SET v:Visible ";
		s.add(setVisible, params);
		String setVisible2 =
				"MATCH (s:Structure)<-[:DEPENDS]-(g:Group)<-[:COMMUNIQUE]-(), " +
						"g<-[:DEPENDS]-(v)" +
						"WHERE s.id IN {structures} AND NOT(v:Visible) " +
						"WITH DISTINCT v " +
						"SET v:Visible ";
		s.add(setVisible2, params);
		neo4j.executeTransaction(s.build(), transactionId, commit.booleanValue(), event -> {
			if ("ok".equals(event.body().getString("status"))) {
				handler.handle(new Either.Right<>(event.body()));
			} else {
				handler.handle(new Either.Left<>(event.body().getString("message")));
			}
		});
	}

	@Override
	public void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler) {
		applyDefaultRules(structureIds, null, true, handler);
	}

	@Override
	public void applyRules(String groupId, Handler<Either<String, JsonObject>> handler) {
		StatementsBuilder s = new StatementsBuilder();
		JsonObject params = new JsonObject().put("groupId", groupId);
		String query =
				"MATCH (g:Group {id : {groupId}}) " +
						"WHERE HAS(g.communiqueWith) AND LENGTH(g.communiqueWith) <> 0 " +
						"WITH g " +
						"MATCH (pg:Group) " +
						"WHERE pg.id IN g.communiqueWith " +
						"MERGE g-[:COMMUNIQUE]->pg ";
		s.add(query, params);
		String usersIncoming =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(u:User) " +
						"WHERE HAS(g.users) AND (g.users = 'INCOMING' OR g.users = 'BOTH') " +
						"MERGE g<-[:COMMUNIQUE]-u ";
		s.add(usersIncoming, params);
		String usersOutgoing =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(u:User) " +
						"WHERE HAS(g.users) AND (g.users = 'OUTGOING' OR g.users = 'BOTH') " +
						"MERGE g-[:COMMUNIQUE]->u ";
		s.add(usersOutgoing, params);
		String relativeIncoming =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
						"WHERE HAS(g.relativeCommuniqueStudent) " +
						"AND (g.relativeCommuniqueStudent = 'INCOMING' OR g.relativeCommuniqueStudent = 'BOTH') " +
						"MERGE r<-[:COMMUNIQUE_DIRECT]-u ";
		s.add(relativeIncoming, params);
		String relativeOutgoing =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(r:User)<-[:RELATED]-(u:User) " +
						"WHERE HAS(g.relativeCommuniqueStudent) " +
						"AND (g.relativeCommuniqueStudent = 'OUTGOING' OR g.relativeCommuniqueStudent = 'BOTH') " +
						"MERGE r-[:COMMUNIQUE_DIRECT]->u ";
		s.add(relativeOutgoing, params);
		String setVisible =
				"MATCH (g:Group {id : {groupId}})<-[:IN]-(v), " +
						"v-[:COMMUNIQUE|COMMUNIQUE_DIRECT]-() " +
						"WHERE NOT(v:Visible) " +
						"WITH DISTINCT v " +
						"SET v:Visible ";
		s.add(setVisible, params);
		neo4j.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
	}

	@Override
	public void removeRules(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query;
		JsonObject params = new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			query = "MATCH (s:Structure)<-[:DEPENDS*1..2]-(g:ProfileGroup)-[r:COMMUNIQUE]-() " +
					"WHERE s.id = {schoolId} " +
					"OPTIONAl MATCH s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]" +
					"-(u:User)-[r1:COMMUNIQUE_DIRECT]->() " +
					"DELETE r, r1";
			params.put("schoolId", structureId);
		} else {
			query = "MATCH ()-[r:COMMUNIQUE]->() " +
					"OPTIONAL MATCH ()-[r1:COMMUNIQUE_DIRECT]->() " +
					"DELETE r, r1 ";
		}
		neo4j.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf,
							 boolean myGroup, boolean profile, String preFilter, String customReturn, JsonObject additionnalParams,
							 final Handler<Either<String, JsonArray>> handler) {
		visibleUsers(userId, structureId, expectedTypes, itSelf, myGroup, profile, preFilter, customReturn, additionnalParams, null, handler);
	}

	@Override
	public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf,
							 boolean myGroup, boolean profile, String preFilter, String customReturn, JsonObject additionnalParams, String userProfile,
							 final Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject();
		String condition = itSelf ? "" : "AND m.id <> {userId} ";
		StringBuilder union = null;
		String conditionUnion = itSelf ? "" : "AND m.id <> {userId} ";
		if (structureId != null && !structureId.trim().isEmpty()) {
			query.append("MATCH (n:User)-[:COMMUNIQUE*1..3]->m-[:DEPENDS*1..2]->(s:Structure {id:{schoolId}}) "); //TODO manage leaf
			params.put("schoolId", structureId);
		} else {
			String myGroupQuery = (myGroup) ? "COLLECT(CASE WHEN g.users = 'BOTH' THEN g.id ELSE '' END)" : "[]";
			query.append(" MATCH (n:User {id: {userId}})-[:COMMUNIQUE]->(g:Group) ");
			query.append("WITH (REDUCE(acc=[], groups IN COLLECT(COALESCE(g.communiqueWith, [])) | acc+groups) + ")
					.append(myGroupQuery).append(") as comGroups ");
			query.append("MATCH p=(g:Group)<-[:DEPENDS*0..1]-cg-[:COMMUNIQUE*0..1]->m ");
			if (userProfile == null || "Student".equals(userProfile) || "Relative".equals(userProfile) || discoverVisibleExpectedProfile.contains(userProfile) ) {
				union = new StringBuilder("MATCH p=(n:User)-[:COMMUNIQUE_DIRECT]->m " +
						"WHERE n.id = {userId} AND (NOT(HAS(m.blocked)) OR m.blocked = false) ");
			}
		}
		query.append("WHERE  g.id IN comGroups " +
				"AND (length(p) < 1 OR (length(p) < 2 AND g.id <> cg.id) OR (length(p) < 2 AND m:User)) " +
				"AND (NOT(HAS(m.blocked)) OR m.blocked = false) " +
				"AND (NOT(HAS(m.nbUsers)) OR m.nbUsers > 0) ");
		if (preFilter != null) {
			query.append(preFilter);
			if (union != null) {
				union.append(preFilter);
				union.append(conditionUnion);
			}
		}
		query.append(condition);
		if (expectedTypes != null && expectedTypes.size() > 0) {
			query.append("AND (");
			StringBuilder types = new StringBuilder();
			for (Object o: expectedTypes) {
				if (!(o instanceof String)) continue;
				String t = (String) o;
				types.append(" OR m:").append(t);
			}
			query.append(types.substring(4)).append(") ");
			if (union != null) {
				union.append("AND (").append(types.substring(4)).append(") ");
			}
		}
		String pcr = " ";
		String pr = "";
		if (profile) {
			query.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			pcr = ", profile ";
			pr = "profile.name as type, ";
			if (union != null) {
				union.append("OPTIONAL MATCH m-[:IN*0..1]->pgp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) ");
			}
		}
		pcr += ", COLLECT(position.id) as positionIds, COLLECT(position.name) as positionNames ";
		pr += " COLLECT(position.id) as positionIds, COLLECT(position.name) as positionNames, ";
		query.append(" OPTIONAL MATCH (m)-[:HAS_POSITION]->(position:UserPosition) ");
		if(union != null) {
			union.append(" OPTIONAL MATCH (m)-[:HAS_POSITION]->(position:UserPosition) ");
		}
		query.append("OPTIONAL MATCH (sub:Subject)<-[:TEACHES]-m ");
		if (union != null) {
			union.append("OPTIONAL MATCH (sub:Subject)<-[:TEACHES]-m ");
		}
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			query.append("WITH DISTINCT m as visibles, COLLECT(DISTINCT sub.label) AS subjects ").append(pcr);
			query.append(customReturn);
			if (union != null) {
				union.append("WITH DISTINCT m as visibles, COLLECT(DISTINCT sub.label) AS subjects ").append(pcr);
				union.append(customReturn);
			}
		} else {
			query.append("RETURN distinct m.id as id, m.name as name, "
							+ "m.login as login, m.displayName as username, ").append(pr)
					.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
							+ "ORDER BY name, username ");
			if (union != null) {
				union.append("RETURN distinct m.id as id, m.name as name, "
								+ "m.login as login, m.displayName as username, ").append(pr)
						.append("m.lastName as lastName, m.firstName as firstName, m.profiles as profiles "
								+ "ORDER BY name, username ");
			}
		}
		params.put("userId", userId);
		if (additionnalParams != null) {
			params.mergeIn(additionnalParams);
		}
		String q;
		if (union != null) {
			q = query.append(" union ").append(union.toString()).toString();
		} else {
			q = query.toString();
		}
		neo4j.execute(q, params, validResultHandler(handler));
	}

	@Override
	public void usersCanSeeMe(String userId, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH p=(n:User)<-[:COMMUNIQUE*0..2]-t<-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]-(m:User) " +
						"WHERE n.id = {userId} AND ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
						"XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) AND m.id <> {userId} " +
						"RETURN distinct m.id as id, m.login as login, " +
						"m.displayName as username, HEAD(m.profiles) as type " +
						"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo4j.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams,
									 String preFilter, Handler<Either<String, JsonArray>> handler) {
		String r;
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			r = "WITH gp as profileGroup, profile " + customReturn;
		} else {
			r = "RETURN distinct gp.id as id, gp.name as name, profile.name as type, " +
					"gp.groupDisplayName as groupDisplayName " +
					"ORDER BY type DESC, name ";
		}
		JsonObject params =
				(additionnalParams != null) ? additionnalParams : new JsonObject();
		params.put("userId", userId);
		boolean excludeEmptyGroups = true;
		if (additionnalParams != null) {
			excludeEmptyGroups = additionnalParams.getBoolean("excludeEmptyGroups", excludeEmptyGroups);
		}
		String query =
				"MATCH p=(n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(gp:Group) " +
						"WHERE n.id = {userId} " +
						(excludeEmptyGroups ? "AND (NOT(HAS(gp.nbUsers)) OR gp.nbUsers > 0)" : "") +
						"AND (length(p) > 1 OR gp.users <> 'INCOMING') " + (preFilter != null ? preFilter : "") +
						"OPTIONAL MATCH gp-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
						r;
		neo4j.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams,
									Handler<Either<String, JsonArray>> handler) {
		String r;
		if (customReturn != null && !customReturn.trim().isEmpty()) {
			r = "WITH mg as manualGroup " + customReturn;
		} else {
			r = "RETURN distinct mg.id as id, mg.name as name, " +
					"mg.groupDisplayName as groupDisplayName " +
					"ORDER BY type DESC, name ";
		}
		JsonObject params =
				(additionnalParams != null) ? additionnalParams : new JsonObject();
		params.put("userId", userId);
		String query =
				"MATCH p=(n:User)-[:COMMUNIQUE*1..2]->l<-[:DEPENDS*0..1]-(mg:ManualGroup) " +
						"WHERE n.id = {userId} AND (length(p) > 1 OR mg.users <> 'INCOMING') " +
						r;
		neo4j.execute(query, params, validResultHandler(handler));
	}

	private static String relationQuery = "OPTIONAL MATCH (sg:Structure)<-[:DEPENDS]-(g) "
			+ "OPTIONAL MATCH (sc:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(g) "
			+ "WITH COALESCE(sg, sc) as s, c, g "
			+ "WITH s, c, g, "
			+ "collect( distinct {name: c.name, id: c.id}) as classes, "
			+ "collect( distinct {name: s.name, id: s.id}) as structures, "
			+ "HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type "
			+ "RETURN DISTINCT "
			+ "g.id as id, "
			+ "g.name as name, "
			+ "g.filter as filter, "
			+ "g.displayName as displayName, "
			+ "g.users as internalCommunicationRule, "
			+ "type, "
			+ "CASE WHEN any(x in classes where x <> {name: null, id: null}) THEN classes END as classes, "
			+ "CASE WHEN any(x in structures where x <> {name: null, id: null}) THEN structures END as structures, "
			+ "CASE WHEN (g: ProfileGroup)-[:DEPENDS]->(:Structure) THEN 'StructureGroup' "
			+ "     WHEN (g: ProfileGroup)-[:DEPENDS]->(:Class) THEN 'ClassGroup' "
			+ "     WHEN HAS(g.subType) THEN g.subType END as subType";

	@Override
	public void getOutgoingRelations(String id, Handler<Either<String, JsonArray>> results) {
		String query = "MATCH (g:Group)<-[:COMMUNIQUE]-(ug: Group { id: {id} }) WHERE exists(g.id) "
				+ relationQuery;
		JsonObject params = new JsonObject().put("id", id);
		neo4j.execute(query, params, validResultHandler(results));
	}

	@Override
	public void getIncomingRelations(String id, Handler<Either<String, JsonArray>> results) {
		String query = "MATCH (g:Group)-[:COMMUNIQUE]->(ug: Group { id: {id} }) WHERE exists(g.id) "
				+ relationQuery;
		JsonObject params = new JsonObject().put("id", id);
		neo4j.execute(query, params, validResultHandler(results));
	}

	@Override
	public void safelyRemoveLinkWithUsers(String groupId, Handler<Either<String, JsonObject>> handler) {
		getRelationsOfGroup(groupId).whenComplete((result, err) -> {
			if (err != null) {
				handler.handle(new Either.Left<>(err.getMessage()));
			} else {
				int numberOfSendingGroups = result.getInteger("numberOfSendingGroups");
				int numberOfReceivingGroups = result.getInteger("numberOfReceivingGroups");

				Direction directionToRemove = computeDirectionToRemove(numberOfSendingGroups > 0, numberOfReceivingGroups > 0);

				if (directionToRemove == null) {
					handler.handle(new Either.Left<>(CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION));
				} else {
					Direction nextDirection = computeNextDirection(directionToRemove);
					removeLinkWithUsers(groupId, directionToRemove,
							t -> handler.handle(new Either.Right<>(new JsonObject().put("users", nextDirection))));
				}
			}
		});
	}

	@Override
	public void getDirections(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (startGroup: Group {id: {startGroupId}}), (endGroup: Group {id: {endGroupId}}) " +
				"RETURN startGroup.users as startDirection, endGroup.users as endDirection";

		JsonObject params = new JsonObject()
				.put("startGroupId", startGroupId)
				.put("endGroupId", endGroupId);

		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	/**
	 * Get useful data of groups : id, name, filter, displayName, internalCommunicationRule (users), type, subType
	 *
	 * @param groupIds An ordered list of group identifiers.
	 * @return an array of Group data, in the same order as @param groupIds
	 */
	protected Future<JsonObject[]> getGroupInfos(String... groupIds) {
		final Promise<JsonObject[]> promise = Promise.promise();
		String query = "UNWIND {groupIds} AS sortedId "
				+ "WITH sortedId MATCH (g:Group {id: sortedId}) "
				+ "WITH g, HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type "
				+ "RETURN DISTINCT "
				+ "g.id as id, "
				+ "g.name as name, "
				+ "g.filter as filter, "
				+ "g.displayName as displayName, "
				+ "g.users as internalCommunicationRule, "
				+ "type, "
				+ "CASE WHEN (g: ProfileGroup)-[:DEPENDS]->(:Structure) THEN 'StructureGroup' "
				+ "     WHEN (g: ProfileGroup)-[:DEPENDS]->(:Class) THEN 'ClassGroup' "
				+ "     WHEN HAS(g.subType) THEN g.subType END as subType";

		JsonArray groups = new JsonArray(Arrays.asList(groupIds));
		JsonObject params = new JsonObject().put("groupIds", groups);

		neo4j.execute(query, params, validResultHandler(results -> {
			if (results.isLeft()) {
				promise.fail(results.left().getValue());
			} else {
				List rows = results.right().getValue().getList();
				final JsonObject[] result = new JsonObject[rows.size()];
				for (int idx = 0; idx < rows.size(); idx++)
					result[idx] = (JsonObject) rows.get(idx);
				promise.complete(result);
			}
		}));

		return promise.future();
	}

	protected Direction computeDirectionForAddLinkCheck(JsonObject groupInfos, boolean isStartGroup) {
		final Direction direction = Direction.fromString(groupInfos.getString("internalCommunicationRule"));

		if (isStartGroup) {
			// Starting group of a new link will require the INCOMING communication rule.
			return Direction.fromBitmask(direction.bitmask | Direction.INCOMING.bitmask);
		} else {
			// Ending group of a new link will require the OUTGOING communication rule.
			// EXCEPT broadcast lists (previously knwown as "automatic manual groups")
			// Broadcast lists must keep a NONE rule because no user should see members of those lists (too big !)
			if ("ManualGroup".equals(groupInfos.getString("type", ""))
					&& "BroadcastGroup".equals(groupInfos.getString("subType", ""))) {
				// Broadcast lists
				return Direction.NONE;
			} else {
				return Direction.fromBitmask(direction.bitmask | Direction.OUTGOING.bitmask);
			}
		}
	}

	protected Future<String> computeWarningMessageForAddLinkCheck(
			UserInfos userInfos,
			JsonObject startInfos, Direction toStartDirection,
			JsonObject endInfos, Direction toEndDirection
	) {
		final Direction fromStartDirection = Direction.fromString(startInfos.getString("internalCommunicationRule"));
		final Direction fromEndDirection = Direction.fromString(endInfos.getString("internalCommunicationRule"));

		// Check if any rule was added to the start group
		Direction addedStartRule = Direction.fromBitmask(toStartDirection.bitmask & ~fromStartDirection.bitmask);
		// Check if any rule was added to the end group
		Direction addedEndRule = Direction.fromBitmask(toEndDirection.bitmask & ~fromEndDirection.bitmask);

		// Check if we are trying to make members of a broadcast list visibles by another group.
		if (!toEndDirection.equals(Direction.NONE) && "BroadcastGroup".equals(endInfos.getString("subType", ""))) {
			// This is forbidden.
			return Future.failedFuture(CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION);
		}

		String warningMessage = null;
		if (addedStartRule.equals(Direction.INCOMING)) {
			// Check if the user can do this.
			if (isImpossibleToChangeDirectionGroupForAddLink(
					startInfos.getString("filter", ""), startInfos.getString("subType", ""), userInfos
			)) {
				return Future.failedFuture(CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION);
			}
			if (toStartDirection.equals(Direction.BOTH)) {
				warningMessage = CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE;
			}
		}

		if (addedEndRule.equals(Direction.OUTGOING)) {
			// Check if the user can do this.
			if (isImpossibleToChangeDirectionGroupForAddLink(
					endInfos.getString("filter", ""), endInfos.getString("subType", ""), userInfos
			)) {
				return Future.failedFuture(CommunicationService.IMPOSSIBLE_TO_CHANGE_DIRECTION);
			}
			if (toEndDirection.equals(Direction.BOTH)) {
				warningMessage = warningMessage == null
						? CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE
						: CommunicationService.WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE;
			}
		}

		return Future.succeededFuture(warningMessage);
	}

	@Override
	public void addLinkCheckOnly(String startGroupId, String endGroupId, UserInfos userInfos, Handler<Either<String, JsonObject>> handler) {
		// 1. Get group info (Direction, Type and Subtype)
		getGroupInfos(startGroupId, endGroupId)
				// 2. Compute next directions
				// 3. Check for impossible direction changes
				.compose(groupsInfos -> computeWarningMessageForAddLinkCheck(
						userInfos,
						groupsInfos[0],
						computeDirectionForAddLinkCheck(groupsInfos[0], true),
						groupsInfos[1],
						computeDirectionForAddLinkCheck(groupsInfos[1], false)
				))
				// 4. Compute warning message
				.onSuccess(msg -> handler.handle(new Either.Right<>(new JsonObject().put("warning", msg))))
				.onFailure(err -> handler.handle(new Either.Left<String, JsonObject>(err.getMessage())));
	}

	private CompletableFuture<JsonObject> getRelationsOfGroup(String groupId) {
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		String query = "MATCH (ug: Group { id: {id} }) "
				+ "OPTIONAL MATCH (ug)<-[:COMMUNIQUE]-(sg:Group) "
				+ "WITH ug, count(sg) as csg "
				+ "RETURN ug.users as users, csg as numberOfSendingGroups, size(coalesce(ug.communiqueWith, [])) as numberOfReceivingGroups";
		JsonObject params = new JsonObject().put("id", groupId);
		neo4j.execute(query, params, message -> {
			Either<String, JsonObject> either = validUniqueResult(message);
			if (either.isLeft()) {
				result.completeExceptionally(new RuntimeException(either.left().getValue()));
			} else {
				result.complete(either.right().getValue());
			}
		});
		return result;
	}

	@Override
	public void removeRelations(String sendingGroupId, String receivingGroupId, Handler<Either<String, JsonObject>> handler) {
		this.removeLink(sendingGroupId, receivingGroupId, r -> {
			if (r.isLeft()) {
				handler.handle(r);
			} else {
				List<CompletableFuture<Direction>> futures = new ArrayList<>();
				futures.add(getRelationsOfGroup(sendingGroupId)
						.thenCompose(result -> {
							CompletableFuture<Direction> future = new CompletableFuture<>();
							Direction currentDirection = Direction.valueOf(result.getString("users"));
							int numberOfReceivingGroups = result.getInteger("numberOfReceivingGroups");

							if (currentDirection.equals(Direction.INCOMING) && numberOfReceivingGroups == 0) {
								this.removeLinkWithUsers(sendingGroupId, currentDirection, either -> {
									if (either.isLeft()) {
										future.completeExceptionally(new RuntimeException(either.left().getValue()));
									} else {
										future.complete(null);
									}
								});
							} else {
								future.complete(currentDirection);
							}
							return future;
						}));
				futures.add(getRelationsOfGroup(receivingGroupId)
						.thenCompose(result -> {
							CompletableFuture<Direction> future = new CompletableFuture<>();
							Direction currentDirection = Direction.valueOf(result.getString("users"));
							int numberOfSendingGroups = result.getInteger("numberOfSendingGroups");

							if (currentDirection.equals(Direction.OUTGOING) && numberOfSendingGroups == 0) {
								this.removeLinkWithUsers(receivingGroupId, currentDirection, either -> {
									if (either.isLeft()) {
										future.completeExceptionally(new RuntimeException(either.left().getValue()));
									} else {
										future.complete(null);
									}
								});
							} else {
								future.complete(currentDirection);
							}
							return future;
						}));

				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
						.thenApply(a -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
						.whenComplete((re, err) -> {
							if (err != null) {
								handler.handle(new Either.Left<>(err.getMessage()));
							} else {
								Direction senderDirection = re.get(0);
								Direction receiverDirection = re.get(1);

								handler.handle(new Either.Right<>(new JsonObject()
										.put("sender", senderDirection != null ? senderDirection.toString() : null)
										.put("receiver", receiverDirection != null ? receiverDirection.toString() : null)
								));
							}
						});
			}
		});
	}

	@Override
	public void processChangeDirectionAfterAddingLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
		// 1. Get group info (Direction, Type and Subtype)
		getGroupInfos(startGroupId, endGroupId)
				// 2. Compute next directions
				// 3. Apply direction changes
				.compose(groupsInfos -> {
					final Direction fromStartDirection = Direction.fromString(groupsInfos[0].getString("internalCommunicationRule"));
					final Direction fromEndDirection = Direction.fromString(groupsInfos[1].getString("internalCommunicationRule"));
					final Direction toStartDirection = computeDirectionForAddLinkCheck(groupsInfos[0], true);
					final Direction toEndDirection = computeDirectionForAddLinkCheck(groupsInfos[1], false);

					// Check if any rule was added to the start group
					Direction addedStartRule = Direction.fromBitmask(toStartDirection.bitmask & ~fromStartDirection.bitmask);
					// Check if any rule was added to the end group
					Direction addedEndRule = Direction.fromBitmask(toEndDirection.bitmask & ~fromEndDirection.bitmask);

					if (addedStartRule.equals(Direction.NONE) && addedEndRule.equals(Direction.NONE)) {
						return Future.succeededFuture(new JsonObject().put("ok", "no direction to change"));
					} else {
						final Map<String, Direction> newDirection = new HashMap<>();
						if (!addedStartRule.equals(Direction.NONE)) {
							newDirection.put(startGroupId, toStartDirection);
						}
						if (!addedEndRule.equals(Direction.NONE)) {
							newDirection.put(endGroupId, toEndDirection);
						}

						final Promise<JsonObject> promise = Promise.promise();
						this.addLinkWithUsers(newDirection, addLinkWithUsers -> {
							if (addLinkWithUsers.isLeft()) {
								promise.fail(addLinkWithUsers.left().getValue());
							} else {
								JsonObject response = new JsonObject();
								newDirection.forEach((groupId, direction) -> response.put(groupId, direction));
								promise.complete(response);
							}
						});
						return promise.future();
					}
				})
				// 4. Compute warning message
				.onSuccess(result -> handler.handle(new Either.Right<String, JsonObject>(result)))
				.onFailure(err -> handler.handle(new Either.Left<String, JsonObject>(err.getMessage())));
	}

	public Direction computeDirectionToRemove(boolean hasIncomingRelationship, boolean hasOutgoingRelationship) {
		if (hasIncomingRelationship && hasOutgoingRelationship) {
			return null;
		}

		if (hasIncomingRelationship) {
			return Direction.INCOMING;
		}

		if (hasOutgoingRelationship) {
			return Direction.OUTGOING;
		}

		return Direction.BOTH;
	}

	public Direction computeNextDirection(Direction directionToRemove) {
		if (directionToRemove == null) {
			return Direction.BOTH;
		}

		if (directionToRemove.equals(Direction.INCOMING)) {
			return Direction.OUTGOING;
		}

		if (directionToRemove.equals(Direction.OUTGOING)) {
			return Direction.INCOMING;
		}
		return null;
	}

	public boolean isImpossibleToChangeDirectionGroupForAddLink(String filter, String subType, UserInfos userInfos) {
		if (userInfos.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN)) {
			return false;
		}

		if (userInfos.getFunctions().containsKey(DefaultFunctions.ADMIN_LOCAL)) {
			if ("StructureGroup".equals(subType) && ("Relative".equals(filter) || "Student".equals(filter))) {
				return true;
			} else if ("ClassGroup".equals(subType) && ("Relative".equals(filter))) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void verify(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler) {

		String query = "MATCH (s:User), (r:User) "
				+ "where s.id = {senderId} and r.id = {recipientId} "
				+ "return exists((r)<-[:COMMUNIQUE*1..2]-()<-[:COMMUNIQUE]-(s)) OR "
				+ "exists((r)<-[:COMMUNIQUE_DIRECT]-(s)) as " + CAN_COMMUNICATE_VERIFY;

		JsonObject params = new JsonObject()
				.put("senderId", senderId)
				.put("recipientId", recipientId);

		neo4j.execute(query, params, validUniqueResultHandler(result -> {

			if (result.isRight() && !result.right().getValue().isEmpty()) {
				handler.handle(new Either.Right<>(result.right().getValue()));
			} else {

				String queryGroup = "MATCH (s:User), (r:Group) "
						+ "where s.id = {senderId} and r.id = {recipientId} "
						+ "return exists((s)-[:COMMUNIQUE*1..2]->()<-[:DEPENDS]-(r)) OR "
						+ "exists((r)<-[:COMMUNIQUE]-()<-[:COMMUNIQUE]-(s)) OR "
						+ "(exists((r)<-[:COMMUNIQUE]-(s)) AND r.users <> 'INCOMING') as " + CAN_COMMUNICATE_VERIFY;

				JsonObject paramsGroup = new JsonObject()
						.put("senderId", senderId)
						.put("recipientId", recipientId);

				neo4j.execute(queryGroup, paramsGroup, validUniqueResultHandler(resultGroup -> {

					if (resultGroup.isRight() && !resultGroup.right().getValue().isEmpty()) {
						handler.handle(new Either.Right<>(resultGroup.right().getValue()));
					} else {
						if (resultGroup.isLeft()) {
							log.error("Error when checking communication rights : " + resultGroup.left().getValue());
						}
						handler.handle(new Either.Right<>(new JsonObject().put(CAN_COMMUNICATE_VERIFY, false)));
					}
				}));
			}

		}));

	}

	/**
	 * Return the list of users, with filtering on the structures, profiles and search
	 * */
	@Override
	public void getDiscoverVisibleUsers(String userId, JsonObject filters, final Handler<Either<String, JsonArray>> handler) {

		JsonObject params = new JsonObject().put("userId", userId);
		StringBuilder query = new StringBuilder("MATCH (m:Visible) "
				+ "WHERE (NOT(HAS(m.blocked)) OR m.blocked = false) AND m.id <> {userId} ");

		if (filters != null && !filters.isEmpty()) {
			for (String key : filters.fieldNames()) {
				switch (key) {
					case "structures":
						JsonArray structures = filters.getJsonArray(key);
						if (structures != null && !structures.isEmpty()) {
							query.append("AND ANY(x IN m.structures WHERE x IN {structures}) ");
							params.put("structures", structures);
						}
						break;
					case "profiles":
						JsonArray profile = filters.getJsonArray(key);
						boolean allowProfileFilter = true;

						if(profile != null && !profile.isEmpty()) {
							for (Object p : profile) {
								if (!discoverVisibleExpectedProfile.contains(p)) {
									allowProfileFilter = false;
									break;
								}
							}
						} else {
							allowProfileFilter = false;
						}

						if (allowProfileFilter) {
							query.append("AND HEAD(m.profiles) IN {profiles} ");
							params.put("profiles", profile);
						} else {
							query.append("AND HEAD(m.profiles) IN {discoverVisibleExpectedProfile} ");
							params.put("discoverVisibleExpectedProfile", discoverVisibleExpectedProfile);
						}
						break;
					case "search":
						final String search = filters.getString(key);
						if (search != null && !search.trim().isEmpty()) {
							query.append("AND m.displayNameSearchField CONTAINS {search} ");
							String sanitizedSearch = StringValidation.sanitize(search);
							params.put("search", sanitizedSearch);
						}
						break;
				}
			}
		}

		if(filters == null || filters.isEmpty() || !filters.containsKey("profiles")) {
			query.append("AND HEAD(m.profiles) in {discoverVisibleExpectedProfile} ");
			params.put("discoverVisibleExpectedProfile", discoverVisibleExpectedProfile);
		}

		query.append("WITH DISTINCT m as visibles "
				+ "OPTIONAL MATCH (u:User {id: {userId}})-[:COMMUNIQUE_DIRECT {source:'MANUAL'}]-(visibles) "
				+ "return DISTINCT visibles.id as id, visibles.name as name, visibles.displayName as displayName, "
				+ "visibles.groupDisplayName as groupDisplayName, HEAD(visibles.profiles) as profile, visibles.structures as structures, u IS NOT NULL as hasCommunication");

		neo4j.execute(query.toString(), params, validResultHandler(handler));
	}

	/**
	 * Return the list of structures
	 * */
	@Override
	public void getDiscoverVisibleStructures(final Handler<Either<String, JsonArray>> handler) {
		String query = "MATCH (s:Structure) "
				+ "RETURN s.id as id, s.externalId as type, s.name as label, 'false' as checked";

		neo4j.execute(query, new JsonObject(), validResultHandler(handler));
	}


	/**
	 * Add communication between two users, using the COMMUNIQUE_DIRECT relation and source 'MANUAL'
	 * */
	@Override
	public void discoverVisibleAddCommuteUsers(UserInfos user, String recipientId, HttpServerRequest request, Handler<Either<String, JsonObject>> handler){

		String query = "MATCH (s:User {id : {senderId}}), (r:User {id : {recipientId}}) WHERE HEAD(s.profiles) IN {discoverVisibleExpectedProfile} AND HEAD(r.profiles) IN {discoverVisibleExpectedProfile} "
				+ "OPTIONAL MATCH (s)-[rel:COMMUNIQUE_DIRECT]->(r) WITH s, r, COUNT(rel) AS relCount "
				+ "WHERE relCount = 0 CREATE (s)-[:COMMUNIQUE_DIRECT {source: 'MANUAL'}]->(r) RETURN COUNT(*) AS number ";

		JsonObject params = new JsonObject().put("senderId", user.getUserId()).put("recipientId", recipientId).put("discoverVisibleExpectedProfile", discoverVisibleExpectedProfile);
		neo4j.execute(query, params, validUniqueResultHandler(result -> {
			if (result.isRight() && !result.right().getValue().isEmpty()) {
				if(result.right().getValue().getInteger("number") > 0) {
					sendNotificationTimeline(request, user, new JsonArray().add(recipientId), "");
				}
				handler.handle(new Either.Right<>(result.right().getValue()));
			} else {
				if (result.isLeft()) {
					handler.handle(new Either.Left<>(result.left().getValue()));

				}
			}
		}));
	}

	/**
	 * Remove communication between two users, using the COMMUNIQUE_DIRECT relation and source 'MANUAL'
	 * */
	@Override
	public void discoverVisibleRemoveCommuteUsers(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler){

		String query = "MATCH (s:User {id : {senderId}})-[r:COMMUNIQUE_DIRECT { source: 'MANUAL'}]-(u:User {id : {recipientId}}) " +
						"DELETE r " +
						"RETURN COUNT(*) as number";

		JsonObject params = new JsonObject().put("senderId", senderId).put("recipientId", recipientId);

		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	/**
	 * Return the list of groups, that the user is IN or has COMMUNIQUE rights, with group type 'manager'
	 * */
	@Override
	public void discoverVisibleGetGroups(String userId, Handler<Either<String, JsonArray>> handler) {
		String query = "MATCH (g:CommunityGroup:Group:Visible {type: 'manager'})<-[r:IN|COMMUNIQUE]-(u:User {id: {userId}}) " +
						"RETURN DISTINCT g.id as id, g.name as name, g.displayNameSearchField as displayName, g.nbUsers as nbUsers ORDER BY name";

		JsonObject params = new JsonObject().put("userId", userId);

		neo4j.execute(query, params, validResultHandler(handler));
	}

	/**
	 * Return the list of users, that are IN the group, with group type 'manager'
	 * */
	@Override
	public void discoverVisibleGetUsersInGroup(String userId, String groupId,  Handler<Either<String, JsonArray>> handler) {
		String query = "MATCH (g:CommunityGroup:Group:Visible {id: {groupId}, type: 'manager'})<-[r:IN|COMMUNIQUE]-(u:User) "+
						"OPTIONAL MATCH (m:User {id: {userId}})-[:COMMUNIQUE_DIRECT {source:'MANUAL'}]-(u) " +
						"RETURN DISTINCT HEAD(u.profiles) as type, u.id as id, u.displayName as displayName, m IS NOT NULL as hasCommunication, u.login as login ORDER BY type DESC, displayName";

		JsonObject param = new JsonObject().put("userId", userId).put("groupId", groupId);

		neo4j.execute(query, param, validResultHandler(handler));
	}

	/**
	 * Create a new group, with group type 'manager'
	 * */
	@Override
	public void createDiscoverVisibleGroup(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler){


		String query = "CREATE (g:CommunityGroup:Group:Visible {name : {name}, type : 'manager', users : 'BOTH', displayNameSearchField: {name}}) " +
						"SET g.id = id(g) +'-'+timestamp() " +
						"WITH g " +
						"MATCH (u:User {id : {userId}}) " +
						"CREATE u-[:IN]->g, u-[:COMMUNIQUE]->g " +
						"RETURN g.id as id";

		final String name = body.getString("name");

		if (name == null || name.trim().isEmpty()) {
			handler.handle(new Either.Left<>("Invalid name"));
			return;
		}

		JsonObject params = new JsonObject().put("userId", userId).put("name", name);

		neo4j.execute(query, params, validUniqueResultHandler(handler));

	}

	/**
	 * Update the name of a group, with group type 'manager' and groupId
	 * */
	@Override
	public void updateDiscoverVisibleGroup(String userId, String groupId, JsonObject body, Handler<Either<String, JsonObject>> handler){

		final String name = body.getString("name");

		if (name != null && !name.trim().isEmpty()) {
			String query = "MATCH (g:CommunityGroup:Group:Visible {id : {groupId}}) " +
							"SET g.name = {name}, g.displayNameSearchField = {name} " +
							"RETURN g.id as id";

			JsonObject params = new JsonObject().put("groupId", groupId).put("name", name);

			neo4j.execute(query, params, validUniqueResultHandler(handler));

		} else {
			handler.handle(new Either.Left<>("Invalid name"));
		}
	}

	/**
	 * Update the group members, using oldUsers and newUsers array, if users was in oldUsers and not in newUsers, remove the user from the group
	 * if users are in newUsers and not in oldUsers, add the user to the group and send a notification
	 * Update the number of users in the group
	 * */
	@Override
	public void addDiscoverVisibleGroupUsers(UserInfos user, String groupId, JsonObject body, HttpServerRequest request, Handler<Either<String, JsonObject>> handler) {
		StatementsBuilder statementsBuilder = new StatementsBuilder();
		JsonArray oldUsers = body.getJsonArray("oldUsers");
		JsonArray newUsers = body.getJsonArray("newUsers");
		JsonObject params = new JsonObject().put("groupId", groupId);

		if (newUsers == null || newUsers.isEmpty()) {
			handler.handle(new Either.Left<>("Invalid users"));
			return;
		}

		if (newUsers.size() > 100) {
			handler.handle(new Either.Left<>("Too many users"));
			return;
		}

		if(!newUsers.contains(user.getUserId()) && oldUsers.isEmpty()){
			newUsers.add(user.getUserId());
		}

		JsonArray usersToRemove = new JsonArray();
		JsonArray usersToAdd = new JsonArray();

		if (!oldUsers.isEmpty()) {
			for (int i = 0; i < oldUsers.size(); i++) {
				if (!newUsers.contains(oldUsers.getString(i))) {
					usersToRemove.add(oldUsers.getString(i));
				}
			}
		}

		for (int i = 0; i < newUsers.size(); i++) {
			if (!oldUsers.contains(newUsers.getString(i))) {
				usersToAdd.add(newUsers.getString(i));
			}
		}

		final String deleteQuery = "MATCH (g:CommunityGroup:Group:Visible {id : {groupId}})<-[r:IN|COMMUNIQUE]-(u:User) " +
									"WHERE u.id IN {users} " +
									"DELETE r;";
		statementsBuilder.add(deleteQuery, params.copy().put("users", usersToRemove));

		final String addQuery = "MATCH (g:CommunityGroup:Group:Visible {id : {groupId}}) " +
								"MATCH (u:User) " +
								"WHERE u.id IN {users} AND HEAD(u.profiles) IN {discoverVisibleExpectedProfile} " +
								"CREATE UNIQUE u-[:IN]->g, u-[:COMMUNIQUE]->g;";

		statementsBuilder.add(addQuery, params.copy().put("users", usersToAdd).put("discoverVisibleExpectedProfile", discoverVisibleExpectedProfile));

		final String queryNb =
				"MATCH (g:CommunityGroup:Group:Visible {id : {groupId}})<-[:IN]-(u:User) " +
						"WITH g, count(distinct u) as cu " +
						"SET g.nbUsers = cu;";

		statementsBuilder.add(queryNb, params);

		neo4j.executeTransaction(statementsBuilder.build(), null, true, event -> {
			if ("ok".equals(event.body().getString("status"))) {
				if(!usersToAdd.isEmpty()){
					if(usersToAdd.contains(user.getUserId())) {
						usersToAdd.remove(user.getUserId());
					}
					sendNotificationTimeline(request, user, usersToAdd, groupId);
				}
				handler.handle(new Either.Right<>(event.body()));
			} else {
				handler.handle(new Either.Left<>(event.body().getString("message")));
			}
		});
	}

	/**
	 * Get the group name, to be use in the notification
	 * */
	private void getGroupName(String groupId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (g:CommunityGroup:Group:Visible {id : {groupId}}) " +
				"RETURN g.name as name";

		JsonObject params = new JsonObject().put("groupId", groupId);

		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	/**
	 * Send notification, to the users that was added to the group, using the timelineHelper to send the notification.
	 * The notification is 'userbook_discoverVisibleGroups.userbook_discoverVisibleGroups' and will be using 'userbook' type in the timeline and will have the annuaire icon
	 * The notification will have <i18N>'Explore le réseau'</i18N> as title and will have the group name as groupName
	 * */
	private void sendNotificationTimeline(HttpServerRequest request, UserInfos user, JsonArray receivers, String groupId) {

		if(user == null || receivers == null || receivers.isEmpty()){
			return;
		}

		JsonObject params = new JsonObject()
				.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("username", user.getUsername())
				.put("uriUserbookAnnuaire", "/userbook/annuaire#/search");

		if(!groupId.isEmpty()) {
			getGroupName(groupId, event -> {
				if (event.isLeft()) {
					log.error("Error when getting group name : " + event.left().getValue());
					return;
				}
				String groupName = event.right().getValue().getString("name");

				params.put("groupName", groupName);

				notifyTimeline.notifyTimeline(request, "userbook_discoverVisibleGroups.userbook_discoverVisibleGroups", user, receivers.getList(),
						user.getUserId() + System.currentTimeMillis() + "userbook_discoverVisibleGroups", params);
			});
		} else {

			notifyTimeline.notifyTimeline(request, "userbook_discoverVisibleGroups.userbook_discoverVisibleGroups", user, receivers.getList(),
					user.getUserId() + System.currentTimeMillis() + "userbook_discoverVisibleGroups", params);
		}
	}

	@Override
	public void getDiscoverVisibleAcceptedProfile(Handler<Either<String, JsonArray>> handler) {
		handler.handle(new Either.Right<>(discoverVisibleExpectedProfile));
	}

}
