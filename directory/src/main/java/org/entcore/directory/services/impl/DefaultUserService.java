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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.common.validation.ValidationException;
import org.entcore.directory.Directory;
import org.entcore.directory.pojo.TransversalSearchQuery;
import org.entcore.directory.pojo.TransversalSearchType;
import org.entcore.directory.services.UserBookService;
import org.entcore.directory.services.UserService;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.*;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.*;

public class DefaultUserService implements UserService {

	private static final Set<String> VALID_SORT_FIELDS = Sets.newHashSet("displayName", "email");
	private static final Set<String> VALID_SORT_ORDERS = Sets.newHashSet("ASC", "DESC");
	private static final int LIMIT = 1000;
	private final Neo4j neo = Neo4j.getInstance();
	private final EmailSender notification;
	private final EventBus eb;
	private final JsonObject userBookData;
	private Logger logger = LoggerFactory.getLogger(DefaultUserService.class);


	public DefaultUserService(EmailSender notification, EventBus eb, JsonObject aUserBookData) {
		this.userBookData = aUserBookData;
		this.notification = notification;
		this.eb = eb;
	}

	@Override
	public void createInStructure(String structureId, JsonObject user, final UserInfos caller, Handler<Either<String, JsonObject>> result) {
		user.put("profiles", new JsonArray().add(user.getString("type")));
		JsonObject action = new JsonObject()
				.put("action", "manual-create-user")
				.put("structureId", structureId)
				.put("profile", user.getString("type"))
				.put("data", user)
				.put("callerId", caller == null ? null : caller.getUserId());
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void createInClass(String classId, JsonObject user, final UserInfos caller, Handler<Either<String, JsonObject>> result) {
		user.put("profiles", new JsonArray().add(user.getString("type")));
		JsonObject action = new JsonObject()
				.put("action", "manual-create-user")
				.put("classId", classId)
				.put("profile", user.getString("type"))
				.put("data", user)
				.put("callerId", caller == null ? null : caller.getUserId());
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void update(final String id, final JsonObject user, final UserInfos caller, final Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-user")
				.put("userId", id)
				.put("data", user)
				.put("callerId", caller == null ? null : caller.getUserId());
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void updateLogin(final String id, final String newLogin, final Handler<Either<String, JsonObject>> result)
	{
		JsonObject action = new JsonObject()
				.put("action", "manual-update-user-login")
				.put("userId", id)
				.put("login", newLogin);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void sendUserCreatedEmail(final HttpServerRequest request, String userId,
			final Handler<Either<String, Boolean>> result) {
		String query =
				"MATCH (u:`User` { id : {id}}) WHERE NOT(u.email IS NULL) AND NOT(u.activationCode IS NULL) " +
				"RETURN u.login as login, u.email as email, u.activationCode as activationCode ";
		JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				Either<String, JsonObject> r = validUniqueResult(m);
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					String email = j.getString("email");
					String login = j.getString("login");
					String activationCode = j.getString("activationCode");
					if (email == null || login == null || activationCode == null ||
							email.trim().isEmpty() || login.trim().isEmpty() || activationCode.trim().isEmpty()) {
						result.handle(new Either.Left<String, Boolean>("user.invalid.values"));
						return;
					}
					JsonObject json = new JsonObject()
							.put("activationUri", notification.getHost(request) +
									"/auth/activation?login=" + login +
									"&activationCode=" + activationCode)
							.put("host", notification.getHost(request))
							.put("login", login);
					logger.debug(json.encode());
					notification.sendEmail(request, email, null, null,
							"email.user.created.info", "email/userCreated.html", json, true,
							new Handler<AsyncResult<Message<JsonObject>>>() {

								@Override
								public void handle(AsyncResult<Message<JsonObject>> ar) {
									if (ar.succeeded()) {
										result.handle(new Either.Right<String, Boolean>(true));
									} else {
										result.handle(new Either.Left<String, Boolean>(ar.cause().getMessage()));
									}
								}
							});
				} else {
					result.handle(new Either.Left<String, Boolean>(r.left().getValue()));
				}
			}
		});
	}

	@Override
	public void getForExternalService(String id, Handler<Either<String, JsonObject>> handler) {
		final JsonArray filter = createExternalFilter();
		get(id, true, filter, true, result -> {
			if (result.isRight()) {
				JsonObject resultJson = result.right().getValue();
				JsonArray structuresInfos = new JsonArray().add("UAI").add("name").add("externalId");
				extractInfosFromStructures(resultJson, structuresInfos);
				handler.handle(new Either.Right<>(resultJson));
			} else {
				handler.handle(new Either.Left<>("Problem with get in DefaultUserService : " + result.left().getValue()));
			}
		});
	}

	private void extractInfosFromStructures(JsonObject resultJson, JsonArray structuresInfos) {
		JsonArray structures = resultJson.getJsonArray("structureNodes");
		JsonArray reformatStructures = new JsonArray();
		for(Object structure : structures){
			JsonObject structureJson = (JsonObject) structure;
			JsonObject infos = new JsonObject();
			for(Object info : structuresInfos){
				infos.put((String) info,structureJson.getString((String) info));
			}
			reformatStructures.add(infos);
		}
		resultJson.put("structures",reformatStructures);
		resultJson.remove("structureNodes");
	}

	private JsonArray createExternalFilter() {
		return new JsonArray()
				.add("activationCode").add("mobile").add("mobilePhone").add("surname").add("lastLogin").add("created")
				.add("modified").add("ine").add("workPhone").add("homePhone").add("country").add("zipCode")
				.add("address").add("postbox").add("city").add("otherNames").add("title").add("functions")
				.add("lastDomain").add("displayName").add("source").add("login").add("teaches").add("headTeacher")
				.add("externalId").add("joinKey").add("birthDate").add("modules").add("lastScheme").add("addressDiffusion")
				.add("isTeacher").add("structures").add("type").add("children").add("parents").add("functionalGroups")
				.add("administrativeStructures").add("subjectCodes").add("fieldOfStudyLabels").add("startDateClasses")
				.add("scholarshipHolder").add("attachmentId").add("fieldOfStudy").add("module").add("transport")
				.add("accommodation").add("status").add("relative").add("moduleName").add("sector").add("level")
				.add("relativeAddress").add("classCategories").add("subjectTaught").add("needRevalidateTerms")
				.add("email").add("emailAcademy").add("emailInternal").add("lastName").add("firstName");
	}

	@Override
	public void getForETude(String id, Handler<Either<String, JsonObject>> handler) {
		final JsonArray filter = createExternalFilter();
		filter.remove("children");
		filter.remove("parents");
		filter.remove("functionalGroups");
		filter.remove("level");
		filter.remove("lastName");
		filter.remove("firstName");
		get(id, true, filter, true, result -> {
			if (result.isRight()) {
				JsonObject resultJson = result.right().getValue();
				JsonArray structuresInfos = new JsonArray().add("UAI");
				extractInfosFromStructures(resultJson, structuresInfos);
				handler.handle(new Either.Right<>(resultJson));
			} else {
				handler.handle(new Either.Left<>("Problem with get in DefaultUserService : " + result.left().getValue()));
			}
		});
	}

	@Override
	public Future<JsonObject> getForSaooti(String id) {
		Promise<JsonObject> promise = Promise.promise();

		final JsonArray filter = createExternalFilter();
		filter.remove("lastName");
		filter.remove("firstName");
		filter.add("classes");
		filter.add("directionManual");
		filter.add("groups");
		filter.add("removedFromStructures");
		get(id, false, filter, true, infos -> {
			if (infos.isLeft()) {
				promise.fail("Problem with get in DefaultUserService : " + infos.left().getValue());
				return;
			}

			JsonObject userInfos = infos.right().getValue();
			JsonArray profiles = userInfos.getJsonArray("profiles", new JsonArray());
			JsonObject finalResult = new JsonObject();

			// Maps structureExternalId with UAI
			Map<String, String> mapExternalIdWithUai = new HashMap<>();
			userInfos.getJsonArray("structureNodes").stream()
				.filter(Objects::nonNull)
				.map(JsonObject.class::cast)
				.forEach(structureNode -> mapExternalIdWithUai.put(structureNode.getString("externalId"), structureNode.getString("UAI")));

            JsonArray structures = new JsonArray();

            mapExternalIdWithUai.forEach( (externalId,uai) -> {
                JsonObject structure = new JsonObject()
                        .put("UAI", uai)
                        .put("functions", new JsonArray());
				//iterate over all user functions
				userInfos.getJsonObject("functions").getMap().values().stream()
						.filter(Objects::nonNull)
						.map(JsonObject.class::cast)
						.filter(function -> function.getJsonArray("structureExternalIds").contains(externalId)) //Filter functions on externalIds
						.forEach(function -> function.getJsonObject("subjects").getMap().values().stream()
                            .map(JsonObject.class::cast)
                            .forEach(subject -> {
                                String functionName = function.getString("functionName");
                                String subjectName = subject.getString("subjectName");
                                structure.getJsonArray("functions").add(functionName + " / " + subjectName);
                            }));
				structures.add(structure);
            });


            // Fill finalResult with expected data
			finalResult.put("id", userInfos.getString("id"));
			finalResult.put("lastName", userInfos.getString("lastName"));
			finalResult.put("firstName", userInfos.getString("firstName"));
			finalResult.put("profiles", profiles.size() > 0 ? profiles.getString(0) : "");
			finalResult.put("structures", structures);
			promise.complete(finalResult);
		});

		return promise.future();
	}

	@Override
	public void get(String id, boolean getManualGroups, JsonArray filterAttributes, boolean filterNullReturn, Handler<Either<String, JsonObject>> result) {
		get(id, getManualGroups, filterAttributes, filterNullReturn, false, result);
	}

	@Override
	public void get(String id, boolean getManualGroups, boolean filterNullReturn, Handler<Either<String, JsonObject>> result) {
		get(id, getManualGroups, new JsonArray(), filterNullReturn, false, result);
	}

	@Override
	public void get(String id, boolean getManualGroups, boolean filterNullReturn, boolean withClasses, Handler<Either<String, JsonObject>> result) {
		get(id, getManualGroups, new JsonArray(), filterNullReturn, withClasses, result);
	}

	/**
	 * This method is used to get user 
	 * @param id : user id
	 * @param getManualGroups : if true, add manualGroups to result
	 * @param filterAttributes : attributes to filter in result
	 * @param filterNullReturn : if true, filter null attributes in result
	 * @param withClasses : if true, add classes2D to result, else not, classes2D is a re-construction of classes with subject to make all classes uniform using a precise format : structure.externalId$class.name 
	 * 
	*/
	@Override
	public void get(String id, boolean getManualGroups, JsonArray filterAttributes, boolean filterNullReturn, boolean withClasses,
					Handler<Either<String, JsonObject>> result) {

		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) WITH COLLECT(distinct s) as structureNodes, u " +
				"OPTIONAL MATCH u-[rf:HAS_FUNCTION]->(f:Function) WITH COLLECT(distinct [f.externalId, rf.scope]) as functions, u, structureNodes " +
				"OPTIONAL MATCH u<-[:RELATED]-(child: User) WITH COLLECT(distinct {id: child.id, displayName: child.displayName, externalId: child.externalId}) as children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:RELATED]->(parent: User) WITH COLLECT(distinct {id: parent.id, displayName: parent.displayName, externalId: parent.externalId}) as parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:IN]->(fgroup: FunctionalGroup) WITH COLLECT(distinct {id: fgroup.id, name: fgroup.name}) as admGroups, parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[:ADMINISTRATIVE_ATTACHMENT]->(admStruct: Structure) WITH COLLECT(distinct {id: admStruct.id}) as admStruct, admGroups, parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[r:TEACHES]->(s:Subject) WITH COLLECT(distinct s.code) as subjectCodes, admStruct, admGroups, parents, children, functions, u, structureNodes " +
				"OPTIONAL MATCH u-[h:HAS_POSITION]->(p:UserPosition)-[:IN]->(struct:Structure) WITH CASE WHEN p IS NOT NULL THEN COLLECT(distinct {id: p.id, name: p.name, source: p.source, structureId: struct.id}) ELSE [] END as userPositions, subjectCodes, admStruct, admGroups, parents, children, functions, u, structureNodes ";

		if (getManualGroups)
			query += "OPTIONAL MATCH u-[:IN]->(mgroup: ManualGroup)-[:DEPENDS]->(mStruct:Structure) WITH COLLECT(distinct {id: mgroup.id, name: mgroup.name, structureId: mStruct.id, structureUai: mStruct.UAI}) as manualGroups, userPositions, subjectCodes, admStruct, admGroups, parents, children, functions, u, structureNodes ";

		if(withClasses)
			query += "OPTIONAL MATCH s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) WHERE u.classes IS NOT NULL ";
		
		if(filterNullReturn){
			query += "RETURN DISTINCT u.profiles as type, structureNodes, " +
					"filter(x IN functions WHERE filter(y IN x WHERE y IS NOT NULL)) as functions, u.functions as aafFunctions," +
					"filter(x IN coalesce(children, []) WHERE x.id IS NOT NULL) as children, " +
					"filter(x IN coalesce(parents, []) WHERE x.id IS NOT NULL) as parents, " +
					"filter(x IN coalesce(admGroups, []) WHERE x.id IS NOT NULL) as functionalGroups, " +
					"filter(x IN coalesce(admStruct, []) WHERE x.id IS NOT NULL) as administrativeStructures, " +
					"filter(x IN coalesce(subjectCodes, []) WHERE x IS NOT NULL) as subjectCodes, " +
					"filter(x IN coalesce(userPositions, []) WHERE x IS NOT NULL) as userPositions, ";
		} else {
			query += "RETURN DISTINCT u.profiles as type, structureNodes, functions, " +
					"CASE WHEN children IS NULL THEN [] ELSE children END as children, " +
					"CASE WHEN parents IS NULL THEN [] ELSE parents END as parents, " +
					"CASE WHEN admGroups IS NULL THEN [] ELSE admGroups END as functionalGroups, " +
					"CASE WHEN admStruct IS NULL THEN [] ELSE admStruct END as administrativeStructures, " +
					"CASE WHEN subjectCodes IS NULL THEN [] ELSE subjectCodes END as subjectCodes, " +
					"CASE WHEN userPositions IS NULL THEN [] ELSE userPositions END as userPositions, ";
		}

		if (getManualGroups)
			query += "CASE WHEN manualGroups IS NULL THEN [] ELSE manualGroups END as manualGroups, ";

		if(withClasses)
			query += "CASE WHEN c IS NULL THEN [] ELSE COLLECT(s.externalId + '$' + c.name) END as classes2D, ";

		query += "u";

		final Handler<Either<String, JsonObject>> filterResultHandler = event -> {
			if (event.isRight()) {
				final JsonObject r = event.right().getValue();
				filterAttributes.add("password").add("resetCode").add("lastNameSearchField").add("firstNameSearchField")
						.add("displayNameSearchField").add("checksum").add("emailSearchField")
						.add("emailInternal").add("resetDate").add("lastScheme").add("lastDomain")
						.add("mfaState").add("emailState").add("mobileState").add("oldPasswords").add("oldPassword");
				for (Object o : filterAttributes) {
					r.remove((String) o);
				}

				//put administrative attachment first in structureNodes
				final JsonArray jaAdm = r.getJsonArray("administrativeStructures");
				if (jaAdm != null && !jaAdm.isEmpty()) {
					final JsonObject jAdm = jaAdm.getJsonObject(0);
					if (jAdm!=null) {
						final String idAdm = StringUtils.trimToBlank(jAdm.getString("id"));
						if (r.getJsonArray("structureNodes") != null && !r.getJsonArray("structureNodes").isEmpty()) {
							final JsonArray newJaStruct = new JsonArray();
							for (Object o : r.getJsonArray("structureNodes")) {
								if (o == null || !(o instanceof JsonObject)) continue;
								if (idAdm.equals(((JsonObject) o).getString("id", ""))) {
									newJaStruct.add((JsonObject) o);
									break;
								}
							}
							for (Object o : r.getJsonArray("structureNodes")) {
								if (o == null || !(o instanceof JsonObject)) continue;
								if (!idAdm.equals(((JsonObject) o).getString("id", ""))) {
									newJaStruct.add((JsonObject) o);
								}
							}

							r.put("structureNodes", newJaStruct);
						}
					}
				}
				if(r.containsKey("aafFunctions")) {
					extractReformatUserFunctions(r);
				}
			}
			result.handle(event);
		};
		neo.execute(query, new JsonObject().put("id", id), fullNodeMergeHandler("u", filterResultHandler, "structureNodes"));
	}

	@Override
	public void getUserStructuresGroup(String id,
			Handler<Either<String, JsonObject>> result) {
		try {
			final String query = "MATCH (u:`User` {id: {id}}) " +
					"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
					"WITH COLLECT(DISTINCT s) AS sn, u " +
					"OPTIONAL MATCH (u)-[rf:HAS_FUNCTION]->(f:Function) " +
					"WITH COLLECT(DISTINCT [f.externalId, rf.scope]) AS functions, u, sn " +
					"OPTIONAL MATCH (u)-[:IN]->(fgroup:FunctionalGroup)-[:DEPENDS]->(s:Structure) " +
					"WITH COLLECT(DISTINCT {functionalGroup: fgroup, structureExternalId: s.externalId}) AS admGroups, functions, u, sn "
					+
					"OPTIONAL MATCH (u)-[:ADMINISTRATIVE_ATTACHMENT]->(admStruct:Structure) " +
					"WITH COLLECT(DISTINCT {id: admStruct.id}) AS admStruct, admGroups, functions, u, sn " +
					"OPTIONAL MATCH (u)-[r:TEACHES]->(s:Subject) " +
					"WITH COLLECT(DISTINCT s.code) AS subjectCodes, admStruct, admGroups, functions, u, sn " +
					"OPTIONAL MATCH u<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup {filter:'Student'})-[:DEPENDS]->(cs:Structure) " +
					"WITH COLLECT(distinct {id: child.id, displayName: child.displayName, externalId: child.externalId, UAI: cs.UAI}) as children, subjectCodes, admStruct, admGroups, functions, u, sn " +
					"OPTIONAL MATCH u-[:RELATED]->(parent: User)-[:IN]->(:ProfileGroup {filter:'Relative'})-[:DEPENDS]->(ps:Structure) WHERE ps IN sn " +
					"WITH COLLECT(distinct {id: parent.id, displayName: parent.displayName, externalId: parent.externalId, UAI: ps.UAI}) as parents, children, subjectCodes, admStruct, admGroups, functions, u, sn " +
					"OPTIONAL MATCH (st:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
					"RETURN DISTINCT " +
					"{ " +
					"   structureNodes: [s in sn | {created: s.created, name: s.name, externalId: s.externalId, id: s.id, UAI: s.UAI}], "
					+
					"   lastLogin: u.lastLogin, " +
					"   displayName: u.displayName, " +
					"   classes: u.classes, " +
					"   login: u.login, " +
					"   id: u.id, " +
					"   email: u.email, " +
					"   structures: u.structures, " +
					"   externalId: u.externalId, " +
					"   birthDate: u.birthDate, " +
					"   lastName: u.lastName, " +
					"   firstName: u.firstName, " +
					"   type: u.profiles, " +
					"   functionalGroups: CASE WHEN admGroups IS NULL THEN [] ELSE admGroups END, " +
					"   administrativeStructures: CASE WHEN admStruct IS NULL THEN [] ELSE admStruct END, " +
					"   subjectCodes: CASE WHEN subjectCodes IS NULL THEN [] ELSE subjectCodes END, " +
					"   childs: CASE WHEN children IS NULL THEN [] ELSE children END, " +
					"   parents: CASE WHEN parents IS NULL THEN [] ELSE parents END, " +
					"   classes2D: CASE WHEN (c) IS NULL THEN [] ELSE COLLECT(st.externalId + '$' + c.name) END " +
					"} AS data, u.profiles as profiles";

			neo.execute(query, new JsonObject().put("id", id), validUniqueResultHandler(result));
		} catch (Exception e) {
			logger.error("Error exception", e);
		}

	}

	private void extractReformatUserFunctions(JsonObject r) {
		//reformat functions
		JsonObject functions = new JsonObject();
		for (Object o : getOrElse(r.getJsonArray("aafFunctions"), new JsonArray())) {
			if (o == null) continue;
			String[] sf = o.toString().split("\\$");
			if (sf.length == 5) {
				JsonObject jo = functions.getJsonObject(sf[1]);
				if (jo == null) {
					jo = new JsonObject().put("code", sf[1])
							.put("functionName", sf[2])
							.put("scope", new JsonArray())
							.put("structureExternalIds", new JsonArray())
							.put("subjects", new JsonObject());
					functions.put(sf[1], jo);
				}
				JsonObject subject = jo.getJsonObject("subjects").getJsonObject(sf[3]);
				if (subject == null) {
					subject = new JsonObject()
							.put("subjectCode", sf[3])
							.put("subjectName", sf[4])
							.put("scope", new JsonArray())
							.put("structureExternalIds", new JsonArray());
					jo.getJsonObject("subjects").put(sf[3], subject);
				}
				jo.getJsonArray("structureExternalIds").add(sf[0]);
				subject.getJsonArray("structureExternalIds").add(sf[0]);
			}
		}
		r.remove("aafFunctions");
		for (Object o : getOrElse(r.getJsonArray("functions"), new JsonArray())) {
			if (!(o instanceof JsonArray)) continue;
			JsonArray a = (JsonArray) o;
			String code = a.getString(0);
			if (code != null) {
				functions.put(code, new JsonObject()
						.put("code", code)
						.put("scope", a.getJsonArray(1))
				);
			}
		}
		r.put("functions", functions);
	}

    @Override
    public void getClasses(String id, Handler<Either<String, JsonObject>> handler) {
        final StringBuilder query = new StringBuilder();
        query.append("MATCH (user:User {id: {userId} }) ");
        query.append("OPTIONAL MATCH (user)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(clazz:Class)-[:BELONGS]->(struct1:Structure) ");
        query.append("OPTIONAL MATCH (user)-[:IN]->(:Group)-[:DEPENDS]->(struct2:Structure) ");
        query.append("WITH struct1, struct2, COLLECT(DISTINCT {name: clazz.name, id: clazz.id}) as classes ");
        query.append("WITH COLLECT(DISTINCT {name: struct1.name, id: struct1.id, classes: classes}) as schools1,");
        query.append("COLLECT(DISTINCT {name: struct2.name, id: struct2.id}) as schools2 ");
        query.append("RETURN (schools1 + schools2) AS schools");
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", id);
        neo.execute(query.toString(), params, validUniqueResultHandler(res-> {
            if (res.isRight()) {
                final JsonObject results = res.right().getValue();
                handler.handle(new Either.Right<>(results));
            } else {
                handler.handle(new Either.Left<>(res.left().getValue()));
            }
        }));
	}


	@Override
    public void getGroups(String id, Handler<Either<String, JsonArray>> results) {
        String query = ""
				+ "MATCH (g:Group)<-[:IN]-(u:User { id: {id} }) WHERE exists(g.id) "
				+ "OPTIONAL MATCH (sg:Structure)<-[:DEPENDS]-(g) "
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
			JsonObject params = new JsonObject().put("id", id);
        neo.execute(query, params, validResultHandler(results));
    }

	@Override
	public void list(String structureId, String classId, JsonArray expectedProfiles,
			Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		String filterProfile = "";
		String filterStructure = "";
		String filterClass = "";
		if (expectedProfiles != null && expectedProfiles.size() > 0) {
			filterProfile = "WHERE p.name IN {expectedProfiles} ";
			params.put("expectedProfiles", expectedProfiles);
		}
		if (classId != null && !classId.trim().isEmpty()) {
			filterClass = "(g:ProfileGroup)-[:DEPENDS]->(n:Class {id : {classId}}), ";
			params.put("classId", classId);
		} else if (structureId != null && !structureId.trim().isEmpty()) {
			filterStructure = "(pg:ProfileGroup)-[:DEPENDS]->(n:Structure {id : {structureId}}), ";
			params.put("structureId", structureId);
		}
		String query =
				"MATCH " +filterClass + filterStructure +
				"(u:User)-[:IN]->g-[:DEPENDS*0..1]->pg-[:HAS_PROFILE]->(p:Profile) " +
				filterProfile +
				"RETURN DISTINCT u.id as id, p.name as type, u.externalId as externalId, u.IDPN as IDPN, " +
				"u.activationCode as code, u.login as login, u.firstName as firstName, " +
				"u.lastName as lastName, u.displayName as displayName " +
				"ORDER BY type DESC, displayName ASC ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void listIsolated(
			String structureId, 
			List<String> profile, 
			final String sortingField,
			final String sortingOrder,
			final Integer fromIndex,
			final Integer limitResult,
			final String searchType,
			String searchTerm,
			Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		String query;

		String condition = "";
		searchTerm = normalize( searchTerm );
		if(searchTerm != null && searchTerm.length()>0 ){
			if ("email".equals(searchType)) {
				condition += "AND u.emailSearchField CONTAINS {searchTerm} ";
			} else {
				condition += "AND u.displayNameSearchField CONTAINS {searchTerm} ";
				// Remove accents when searching for a display name.
				searchTerm = StringUtils.stripAccents(searchTerm);
			}
			params.put("searchTerm", searchTerm);
		}

		// users without class
		if (structureId != null && !structureId.trim().isEmpty()) {
			query = "MATCH  (s:Structure { id : {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), " +
					"g-[:HAS_PROFILE]->(p:Profile) " +
					"WHERE  NOT(u-[:IN]->()-[:DEPENDS]->(:Class)-[:BELONGS]->s) "+ condition;
			params.put("structureId", structureId);
			if (profile != null && !profile.isEmpty()) {
				query += "AND p.name IN {profile} ";
				params.put("profile", new JsonArray(profile));
			}
		} else { // users without structure
			query = "MATCH (u:User)" +
					"WHERE NOT(u-[:IN]->()-[:DEPENDS]->(:Structure)) "+ condition +
					"OPTIONAL MATCH u-[:IN]->(dpg:DefaultProfileGroup)-[:HAS_PROFILE]->(p:Profile) ";
		}
		
		query += "RETURN DISTINCT u.id as id, p.name as type, " +
				"u.activationCode as code, u.firstName as firstName," +
				"u.lastName as lastName, u.displayName as displayName ";

		// Apply sort order
		if(isValidSortField(sortingField) && isValidSortOrder(sortingOrder)) {
			query += "ORDER BY "+ sortingField +" "+ sortingOrder +" ";
		} else {
			// Default sort order, historical behaviour.
			query += "ORDER BY type DESC, displayName ASC ";
		}		

		if( fromIndex != null && fromIndex.intValue() > 0 ) {
			query += " SKIP {skip}";
			params.put( "skip", fromIndex );
		}
		if( limitResult != null && limitResult.intValue() > 0 ) {
			query += " LIMIT {limit}";
			params.put( "limit", limitResult );
		}
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void listAdmin(String structureId, boolean includeSubStructure, String classId, String groupId,
						  JsonArray expectedProfiles, UserInfos userInfos, io.vertx.core.Handler<fr.wseduc.webutils.Either<String,JsonArray>> results) {
		listAdmin(structureId, includeSubStructure, classId, groupId, expectedProfiles, null, TransversalSearchQuery.EMPTY, userInfos, results);
	};

	@Override
	public void listAdmin(String structureId, boolean includeSubStructure,
						  String classId,
						  String groupId,
						  JsonArray expectedProfiles,
						  String filterActivated,
						  final TransversalSearchQuery searchQuery,
						  final UserInfos userInfos,
						  final Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject();
		// Truthy when the query MUST be limited to the current user's scope (i.e. when an ADML is doing a transversal search)
		boolean restrictResultsToFunction = false;
		String filterUser = "";
		if (classId != null && !classId.trim().isEmpty()) {
			filterUser = "(n:Class {id : {classId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
			params.put("classId", classId);
		} else if (structureId != null && !structureId.trim().isEmpty()) {
			filterUser = "(n:Structure {id : {structureId}})" + (includeSubStructure ? "<-[:HAS_ATTACHMENT*0..]-(:Structure)" : "") +
					"<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-";
			params.put("structureId", structureId);
		} else if (groupId != null && !groupId.trim().isEmpty()) {
			filterUser = "(n:Group {id : {groupId}})<-[:IN]-";
			params.put("groupId", groupId);
		} else {
			// WB-2077, if we have no filter on the user we at least must only return users attached to structures
			filterUser = "(s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]-";
			restrictResultsToFunction = true;
		}

		String conditionUser = "WHERE 1=1 ";
		if (expectedProfiles != null && !expectedProfiles.isEmpty()) {
			conditionUser += "AND head(u.profiles) IN {expectedProfiles} ";
			params.put("expectedProfiles", expectedProfiles);
		}
		if(TransversalSearchType.EMAIL.equals(searchQuery.getSearchType()) 
				&& isNotEmpty(searchQuery.getEmail())
				) {
			final String searchTerm = normalize(searchQuery.getEmail());
			conditionUser += " AND u.emailSearchField CONTAINS {email} ";
			params.put("email", searchTerm);
		} else if(TransversalSearchType.FULL_NAME.equals(searchQuery.getSearchType())
				&& (isNotEmpty(searchQuery.getFirstName()) || isNotEmpty(searchQuery.getLastName())) 
				) {
			final String firstNameSearchTerm = StringUtils.stripAccentsAndNotCharToLowerCase(searchQuery.getFirstName());
			final String lastNameSearchTerm = StringUtils.stripAccentsAndNotCharToLowerCase(searchQuery.getLastName());

			final StringBuilder sbuilder = new StringBuilder();
			sbuilder.append(" AND ");
			final boolean hasLastName;
			if(isEmpty(lastNameSearchTerm)) {
				hasLastName = false;
			} else {
				sbuilder.append(" u.lastNameSearchField STARTS WITH {lastName} ");
				params.put("lastName", lastNameSearchTerm);
				hasLastName = true;
			}
			if(isNotEmpty(firstNameSearchTerm)) {
				if(hasLastName) {
					sbuilder.append(" AND ");
				}
				sbuilder.append(" u.firstNameSearchField STARTS WITH {firstName} ");
				params.put("firstName", firstNameSearchTerm);
			}
			conditionUser += sbuilder.toString();
		} else if(TransversalSearchType.DISPLAY_NAME.equals(searchQuery.getSearchType()) 
				&& isNotEmpty(searchQuery.getDisplayName())
				) {
			final String searchTerm = StringUtils.stripAccentsAndNotCharToLowerCase(searchQuery.getDisplayName());
			conditionUser += " AND u.displayNameSearchField CONTAINS {displayName} ";
			params.put("displayName", searchTerm);
		}
		if(filterActivated != null){
			if("inactive".equals(filterActivated)){
				conditionUser += "AND NOT(u.activationCode IS NULL) ";
			} else if("active".equals(filterActivated)){
				conditionUser += "AND u.activationCode IS NULL ";
			}
		}
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN)) {
			conditionUser += "AND " + DefaultSchoolService.EXCLUDE_ADMC_QUERY_FILTER;
		}

		// This second level of filtering ensures the data is in the scope of the connected user
		// excepted when the query is already restricted to a class, group or structure.
		String filterFunction = "WITH u ";
		String conditionFunction = "WHERE 1=1 ";

		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL) &&
				!userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				filterFunction += "MATCH (fs:Structure)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]-(u) ";
				conditionFunction += "AND (fs.id IN {scope}) ";
				params.put("scope", new JsonArray(scope));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				filterFunction += "MATCH (c:Class)<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), u-[:IN]->pg ";
				conditionFunction += "AND c.id IN {scope} ";
				params.put("scope", new JsonArray(scope));
			}
		}
		String query =
				"MATCH " + filterUser + "(u:User) " + conditionUser
				+ filterFunction + conditionFunction +
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class:Class)-[:BELONGS]->(s) " +
				"OPTIONAL MATCH u-[:RELATED]->(parent: User) " +
				"OPTIONAL MATCH (child: User)-[:RELATED]->u " +
				"OPTIONAL MATCH (childClass:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(child) " +
				"OPTIONAL MATCH u-[rf:HAS_FUNCTION]->(f:Function) " +
				"OPTIONAL MATCH u-[:TEACHES]->(sub:Subject) " +
				"RETURN DISTINCT u.id as id, head(u.profiles) as type, u.externalId as externalId, " +
				"u.activationCode as code, " +
				"CASE WHEN u.loginAlias IS NOT NULL THEN u.loginAlias ELSE u.login END as login, " +
				"u.login as originalLogin, " +
				"u.firstName as firstName, " +
				"u.lastName as lastName, u.displayName as displayName, u.source as source, u.attachmentId as attachmentId, " +
				"u.birthDate as birthDate, u.blocked as blocked, u.created as creationDate, u.lastLogin as lastLogin, " +
				"u.email as email, u.homePhone as phone, u.mobile as mobile, u.zipCode as zipCode, u.address as address, " +
				"u.city as city, u.country as country, " +
				"extract(function IN u.functions | last(split(function, \"$\"))) as aafFunctions, " +
				"CASE WHEN s IS NULL THEN [] ELSE collect(distinct {id: s.id, name: s.name}) END as structures, " +
				"collect(distinct {id: class.id, name: class.name}) as allClasses, " +
				"collect(distinct [f.externalId, rf.scope]) as functions, " +
				"CASE WHEN parent IS NULL THEN [] ELSE collect(distinct {id: parent.id, firstName: parent.firstName, lastName: parent.lastName}) END as parents, " +
				"CASE WHEN child IS NULL THEN [] ELSE collect(distinct {id: child.id, firstName: child.firstName, lastName: child.lastName, attachmentId : child.attachmentId, childExternalId : child.externalId, displayName : child.displayName, childClass : coalesce(childClass.name, \"\")}) END as children, " +
				"HEAD(COLLECT(distinct parent.externalId)) as parent1ExternalId, " + // Hack for GEPI export
				"HEAD(TAIL(COLLECT(distinct parent.externalId))) as parent2ExternalId, " + // Hack for GEPI export
				"COUNT(distinct class.id) > 0 as hasClass, " + // Hack for Esidoc export
				"CASE WHEN head(u.profiles) = 'Teacher' THEN 'PROFS' ELSE 'ELEVES' END as chamiloProfile, " + // Hack for chamilo export
				"CASE WHEN head(u.profiles) = 'Teacher' THEN collect(distinct {name: sub.label}) ELSE collect(distinct {name: class.name}) END as allClassesSubject, " + // Hack for chamilo export
				"split(u.birthDate, '-')[0] as birthYear, " + // Hack for Pmb export
				"REPLACE(u.address,';',' ') as safeAddress " + // Hack for Pmb export
				"ORDER BY type DESC, displayName ASC ";

		neo.execute(query, params, validResultHandler(results));
	}


	private String normalize(String str) {
		if (str != null ) {
			str = str.toLowerCase().replaceAll("\\s+", "").trim();
			if( str.isEmpty() ) {
				return null;
			}
		}
		return str;
	}

	private boolean isValidSortField(final String sortingField) {
		return sortingField!=null && VALID_SORT_FIELDS.contains(sortingField);
	}
	private boolean isValidSortOrder(final String sortingOrder) {
		return sortingOrder!=null && VALID_SORT_ORDERS.contains(sortingOrder);
	}


	@Override
	public void delete(List<String> users, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-delete-user")
				.put("users", new JsonArray(users));
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void restore(List<String> users, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-restore-user")
				.put("users", new JsonArray(users));
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void addFunction(String id, String functionCode, JsonArray scope, String inherit,
			Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-user-function")
				.put("userId", id)
				.put("function", functionCode)
				.put("inherit", inherit)
				.put("scope", scope);
		eb.request(Directory.FEEDER, action, ar -> {
			if (ar.succeeded()) {
				JsonArray res = ((JsonObject) ar.result().body()).getJsonArray("results");
				JsonObject json = new JsonObject();
				if (res.size() == 4) {
					JsonArray r = res.getJsonArray(1);
					if (r.size() == 1) {
						json = r.getJsonObject(0);
					}
				}
				result.handle(new Either.Right<>(json));
			} else {
				result.handle(new Either.Left<>(ar.cause().getMessage()));
			}
		});
	}

	@Override
	public void addHeadTeacherManual(String id,String structureExternalId, String classExternalId,
							Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-head-teacher")
				.put("userId", id)
				.put("classExternalId", classExternalId)
				.put("structureExternalId", structureExternalId);
		eb.request(Directory.FEEDER, action,handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void updateHeadTeacherManual(String id,String structureExternalId, String classExternalId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-head-teacher")
				.put("userId", id)
				.put("classExternalId", classExternalId)
				.put("structureExternalId", structureExternalId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void addDirectionManual(String id,String structureExternalId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-direction")
				.put("userId", id)
				.put("structureExternalId", structureExternalId);
		eb.request(Directory.FEEDER, action,handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void removeDirectionManual(String id,String structureExternalId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-direction")
				.put("userId", id)
				.put("structureExternalId", structureExternalId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void removeFunction(String id, String functionCode, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user-function")
				.put("userId", id)
				.put("function", functionCode);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	public void listFunctions(String userId, Handler<Either<String, JsonArray>> result) {
		String query =
				"MATCH (u:User{id: {userId}})-[rf:HAS_FUNCTION]->(f:Function) " +
				"RETURN COLLECT(distinct [f.externalId, rf.scope]) as functions";
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void addGroup(String id, String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-user-group")
				.put("userId", id)
				.put("groupId", groupId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void removeGroup(String id, String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user-group")
				.put("userId", id)
				.put("groupId", groupId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void listAdml(String scopeId, Handler<Either<String, JsonArray>> result) {
		String query =
				"MATCH (n)<-[:DEPENDS]-(g:FunctionGroup)<-[:IN]-(u:User) " +
				"WHERE (n:Structure OR n:Class) AND n.id = {scopeId} AND g.name =~ '^.*-AdminLocal$' " +
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.put("scopeId", scopeId);
		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void getInfos(String userId, Handler<Either<String, JsonObject>> result) {
		String query =
				"MATCH (n:User {id : {id}}) " +
				"OPTIONAL MATCH n-[:IN]->(gp:Group) " +
				"OPTIONAL MATCH n-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[rf:HAS_FUNCTION]->(f:Function) " +
				"OPTIONAL MATCH n-[:IN]->()-[:HAS_PROFILE]->(p:Profile) " +
				"OPTIONAL MATCH n-[:ADMINISTRATIVE_ATTACHMENT]->(sa:Structure) " +
				"RETURN distinct " +
				"n, n.functions as ufunctions, COLLECT(distinct c) as classes, HEAD(COLLECT(distinct p.name)) as type, " +
				"COLLECT(distinct s) as structures, COLLECT(distinct [f.externalId, rf.scope]) as functions, " +
				"COLLECT(distinct gp) as groups, COLLECT(distinct sa) as administratives";
		neo.execute(query, new JsonObject().put("id", userId),
				fullNodeMergeHandler("n", result, "structures", "classes","groups", "administratives"));
	}

	@Override
	public void getUserStructuresClasses(String userId, Handler<Either<String, JsonObject>> result) {
		final String query =
				"MATCH (u:User {id:{id}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
				"WITH DISTINCT u, s, COLLECT(DISTINCT c.name) as classes " +
				"RETURN u.id as id, u.externalId as externalId, u.firstName as firstName, u.lastName as lastName, " +
				"u.email as email, u.emailAcademy as emailAcademy, head(u.profiles) as profile, u.login as login, " +
				"COLLECT([s.UAI, s.name, classes]) as structuresWithClasses, u.profiles as profiles ";
		neo.execute(query, new JsonObject().put("id", userId), validUniqueResultHandler(result));
	}

	@Override
	public void relativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-relative-student")
				.put("relativeId", relativeId)
				.put("studentId", studentId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, eitherHandler)));
	}

	@Override
	public void unlinkRelativeStudent(String relativeId, String studentId, Handler<Either<String, JsonObject>> eitherHandler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-unlink-relative-student")
				.put("relativeId", relativeId)
				.put("studentId", studentId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(eitherHandler)));
	}

	@Override
	public void ignoreDuplicate(String userId1, String userId2, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "ignore-duplicate")
				.put("userId1", userId1)
				.put("userId2", userId2);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void listDuplicates(JsonArray structures, boolean inherit, Handler<Either<String, JsonArray>> results) {
		JsonObject action = new JsonObject()
				.put("action", "list-duplicate")
				.put("structures", structures)
				.put("inherit", inherit);
		eb.request(Directory.FEEDER, action, new DeliveryOptions().setSendTimeout(600000l), handlerToAsyncHandler(validResultHandler(results)));
	}

	@Override
	public void mergeDuplicate(final String userId1, final String userId2, final boolean keepRelations, final Handler<Either<String, JsonObject>> handler) {
		final JsonObject action = new JsonObject()
				.put("action", "merge-duplicate")
				.put("userId1", userId1)
				.put("userId2", userId2)
				.put("keepRelations", keepRelations);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(handler)));
	}

	@Override
	public void listByUAI(List<String> UAI, JsonArray expectedTypes, boolean isExportFull, JsonArray fields, Handler<Either<String, JsonArray>> results) {
		if (UAI == null || UAI.isEmpty()) {
			results.handle(new Either.Left<String, JsonArray>("missing.uai"));
			return;
		} else {
			for (String uaiCode: UAI) {
				if (!StringValidation.isUAI(uaiCode)) {
					results.handle(new Either.Left<String, JsonArray>("invalid.uai"));
					return;
				}
			}
		}

		if (fields == null || fields.size() == 0) {
			fields = new JsonArray().add("id").add("externalId").add("lastName").add("firstName").add("login");
		}

		//user's fields for Full Export
		if(isExportFull){
			fields.add("email");
			fields.add("emailAcademy");
			fields.add("mobile");
			fields.add("deleteDate");
			fields.add("functions");
			fields.add("displayName");
		}

		// Init params and filter for all type of queries
		String  filter =  "WHERE s.UAI IN {uai} ";

		JsonObject params = new JsonObject().put("uai", new JsonArray(UAI));

		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure)<-[:DEPENDS]-(cpg:ProfileGroup)");

		// filter by types if needed OR full export
		if( isExportFull || (expectedTypes != null && expectedTypes.size() > 0)) {
			query.append("-[:HAS_PROFILE]->(p:Profile)");
		}
		// filter by types if needed
		if (expectedTypes != null && expectedTypes.size() > 0) {

			filter += "AND p.name IN {expectedTypes} ";
			params.put("expectedTypes", expectedTypes);
		}

		query.append(", cpg<-[:IN]-(u:User) ")
				.append(filter);

		if (fields.contains("administrativeStructure")) {
			query.append("OPTIONAL MATCH u-[:ADMINISTRATIVE_ATTACHMENT]->sa ");
		}

		query.append("RETURN DISTINCT ");

		for (Object field : fields) {
			if ("type".equals(field) || "profile".equals(field)) {
				query.append(" HEAD(u.profiles)");
			} else if ("administrativeStructure".equals(field)) {
				query.append(" sa.externalId ");
			} else {
				query.append(" u.").append(field);
			}
			query.append(" as ").append(field).append(",");
		}
		query.deleteCharAt(query.length() - 1);

		//Full Export : profiles and Structure
		if(isExportFull){
			query.append(", p.name as profiles");
			query.append(", s.externalId as structures")
					.append(" , CASE WHEN size(u.classes) > 0  THEN  last(collect(u.classes)) END as classes");
		}

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public void generateMergeKey(String userId, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsError(handler, userId)) return;
		final String query = "MATCH (u:User {id: {id}}) SET u.mergeKey = {mergeKey} return u.mergeKey as mergeKey";
		final JsonObject params = new JsonObject().put("id", userId).put("mergeKey", UUID.randomUUID().toString());
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void mergeByKey(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsNull(handler, userId, body)) return;
		JsonObject action = new JsonObject()
				.put("action", "merge-by-keys")
				.put("originalUserId", userId)
				.put("mergeKeys", body.getJsonArray("mergeKeys"));
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(13, handler)));
	}

	@Override
	public void unmergeByLogins(JsonObject body, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsNull(handler, body)) return;
		JsonObject action = new JsonObject()
				.put("action", "unmerge-by-logins")
				.put("originalUserId", body.getString("originalUserId"))
				.put("mergedLogins", body.getJsonArray("mergedLogins"));
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(handler)));
	}

	@Override
	public void listChildren(String userId, Handler<Either<String, JsonArray>> handler) {
		final String query =
				"MATCH (n:User {id : {id}})<-[:RELATED]-(child:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH (child)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WITH COLLECT(distinct c.name) as classesNames, s, child " +
				"RETURN s.name as structureName, COLLECT(distinct {id: child.id, displayName: child.displayName, externalId: child.externalId, classesNames : classesNames}) as children ";
		final JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void list(String groupId, boolean itSelf, String userId,
			final Handler<Either<String, JsonArray>> handler) {
		String condition = (itSelf || userId == null) ? "" : "AND u.id <> {userId} ";
		String query =
				"MATCH (n:Group)<-[:IN]-(u:User) " +
				"WHERE n.id = {groupId} " + condition +
				"OPTIONAL MATCH (n)-[:DEPENDS*0..1]->(:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"OPTIONAL MATCH (u)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH (pg)-[:HAS_PROFILE]->(pro:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				"u.displayName as username, u.firstName as firstName, u.lastName as lastName, profile.name as type," +
				"CASE WHEN s IS NULL THEN [] ELSE COLLECT(DISTINCT {id: s.id, name: s.name}) END as structures," +
				"CASE WHEN pro IS NULL THEN NULL ELSE HEAD(COLLECT(DISTINCT pro.name)) END as profile " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.put("groupId", groupId);
		if (!itSelf && userId != null) {
			params.put("userId", userId);
		}
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void list(JsonArray groupIds, JsonArray userIds, boolean itSelf, String userId,
			final Handler<Either<String, JsonArray>> handler) {
		String condition = (itSelf || userId == null) ? "" : "AND u.id <> {userId} ";
		String query =
				"MATCH (n:Group)<-[:IN]-(u:User) " +
				"WHERE n.id IN {groupIds} " + condition +
				"OPTIONAL MATCH n-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username " +
				"UNION " +
				"MATCH (u:User) " +
				"WHERE u.id IN {userIds} " + condition +
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(profile:Profile) " +
				"RETURN distinct u.id as id, u.login as login," +
				" u.displayName as username, profile.name as type " +
				"ORDER BY username ";
		JsonObject params = new JsonObject();
		params.put("groupIds", groupIds);
		params.put("userIds", userIds);
		if (!itSelf && userId != null) {
			params.put("userId", userId);
		}
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void getUserInfos(String userId, final Handler<Either<String,JsonObject>> handler) {
		String query;
		try {
			query = "MATCH (u:`User` { id : {userId}}) " +
				"OPTIONAL MATCH u-[:USERBOOK]->(ub: UserBook) WITH ub.motto as motto, ub.health as health, ub.mood as mood, u,  "+
				UserBookService.selectHobbies(userBookData, "ub")+
				"OPTIONAL MATCH s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(Profile), cpg<-[:IN]-u-[:IN]->spg WITH s, COLLECT(distinct {name: c.name, id: c.id}) as c, motto, health, mood, hobbies, u " +
				"WITH COLLECT(distinct {name: s.name, id: s.id, classes: c, source: s.source}) as schools, motto, health, mood, hobbies, u " +
				"OPTIONAL MATCH u-[:RELATED]-(u2: User) WITH COLLECT(distinct {relatedName: u2.displayName, relatedId: u2.id, relatedType: u2.profiles}) as relativeList, schools, motto, health, mood, hobbies, u " +
				"RETURN DISTINCT u.profiles as profiles, u.id as id, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, "+
				"u.email as email, u.homePhone as homePhone, u.mobile as mobile, u.birthDate as birthDate, u.login as originalLogin, relativeList, " +
				"motto, health, mood, hobbies, " +
				"CASE WHEN schools IS NULL THEN [] ELSE schools END as schools ";
		} catch (ValidationException exception) {
			logger.error("Select hobbies exception", exception);
			handler.handle(new Either.Left<>("invalid.hobby"));
			return;
		}
		JsonObject params = new JsonObject();
		params.put("userId", userId);
		neo.execute(query, params, validUniqueResultHandler(res->{
			if(res.isRight()){
				final JsonObject result = res.right().getValue();
				result.put("hobbies", UserBookService.extractHobbies(userBookData, result, true));
				// Add an information about this user's email being updatable or not
				// As of 2022-10-07, ADML emails cannot be changed except by the ADML himself.
				listFunctions(userId, funcs -> {
					if( funcs.isRight() ) {
						final JsonArray functions = funcs.right().getValue();
						if( functions!=null && functions.encode().contains(DefaultFunctions.ADMIN_LOCAL)) {
							result.put("lockedEmail", true);
						}
					}
					handler.handle(new Either.Right<>(result));
				});
			}else{
				handler.handle(res);
			}
		}));
	}

	@Override
	public void listByLevel(String levelContains, String levelNotContains, String profile, String structureId, boolean stream,
			Handler<Either<String, JsonArray>> handler) {
		final JsonObject params = new JsonObject();
		params.put("level", levelContains);
		String levelFilter = "";
		String structureMatcher = "";
		if (isNotEmpty(levelNotContains)) {
			levelFilter = "AND NOT(u.level contains {notLevel}) ";
			params.put("notLevel", levelNotContains);
		}
		if(isNotEmpty(structureId)) {
			structureMatcher = "-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id:{structureId}})";
			params.put("structureId", structureId);
		}

		String query;
		if ("Student".equals(profile)) {
			query = "MATCH (u:User)" + structureMatcher + " " +
					"WHERE has(u.password) and u.level contains {level} " + levelFilter +
					"RETURN u.id as id, u.ine as ine, head(u.profiles) as profile, u.lastName as lastName, u.firstName as firstName, " +
					"u.login as login, u.loginAlias as loginAlias, u.email as email, u.mobile AS mobile, u.password as password ";
		} else if ("Relative".equals(profile)) {
			query = "MATCH (u:User)-[:RELATED]->(r:User)" + structureMatcher + " " +
					"WHERE has(r.password) and u.level contains {level} " + levelFilter +
					"RETURN r.id as id, u.ine as ine, head(r.profiles) as profile, r.lastName as lastName, r.firstName as firstName, " +
					"r.login as login, r.loginAlias as loginAlias, r.email as email, r.mobile AS mobile, r.password as password ";
		}
		else if ("Teacher".equals(profile) || "Personnel".equals(profile))
		{
			query = "MATCH (u:User)" + structureMatcher + " " +
							(levelContains != null ? "MATCH (u)-[:IN]->(:ProfileGroup {filter: {profile}})-[:DEPENDS]->(:Class {name:{level}}) " : "") +
							"WHERE HAS(u.password) AND u.firstName IS NOT NULL AND u.lastName IS NOT NULL AND u.birthDate IS NOT NULL AND {profile} IN u.profiles " +
							(levelNotContains != null ? "OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup {filter: {profile}})-[:DEPENDS]->(c:Class {name:{notLevel}}) WITH u, c WHERE c = null " : "") +
							"RETURN u.id as id, head(u.profiles) as profile, u.lastName as lastName, u.firstName as firstName, u.birthDate as birthDate, " +
							"u.login as login, u.loginAlias as loginAlias, u.email as email, u.mobile AS mobile, u.password as password ";
			params.put("profile", profile);
		}
		else
		{
			handler.handle(new Either.Right<>(new JsonArray()));
			return;
		}
		if (stream) {
			query += "ORDER BY login ASC SKIP {skip} LIMIT {limit} ";
			params.put("limit", LIMIT);
			streamList(query, params, 0, LIMIT, handler);
		} else {
			neo.execute(query, params, validResultHandler(handler));
		}
	}

	private void streamList(String query, JsonObject params, int skip, int limit, Handler<Either<String, JsonArray>> handler) {
		neo.execute(query, params.copy().put("skip", skip), res -> {
			Either<String, JsonArray> r = Neo4jResult.validResult(res);
			handler.handle(r);
			if (r.isRight()) {
				if (r.right().getValue().size() == limit) {
					streamList(query, params, skip + limit, limit, handler);
				} else {
					handler.handle(new Either.Left<>(""));
				}
			}
		});
	}

	public void getMainStructure(String userId, JsonArray structuresToExclude, Handler<Either<String, JsonObject>> handler) {
	    String query = "MATCH (u:User {id : {userId}})-[:IN]->(Group)-[:DEPENDS]->(s:Structure) " +
                "WHERE NOT s.id IN {structuresIds} " +
                "OPTIONAL MATCH (u)-[:ADMINISTRATIVE_ATTACHMENT]->(s2:Structure) WHERE NOT s2.id IN {structuresIds} " +
                "RETURN CASE WHEN s2 IS NULL THEN s.name ELSE s2.name END AS name LIMIT 1";
	    JsonObject params = new JsonObject().put("userId", userId).put("structuresIds", structuresToExclude);
	    neo.execute(query, params, validUniqueResultHandler(res->{
	        if (res.isRight()) {
	            final JsonObject result = res.right().getValue();
	            handler.handle(new Either.Right<>(result));
	        } else {
	            handler.handle(res);
	        }
	    }));
	}


	public void getUsersStructures(JsonArray userIds, JsonArray fields, Handler<Either<String, JsonArray>> handler) {
		if (fields == null || fields.size() == 0) {
			fields = new JsonArray().add("id");
		}
		final StringBuilder query = new StringBuilder(
				"MATCH (u:User)-[:IN]->(ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"WHERE u.id IN {userIds} " +
				"RETURN u.id as userId, COLLECT(distinct {");
		for (Object field : fields) {
			query.append(field).append(": s.").append(field).append(",");
		}
		query.deleteCharAt(query.length() - 1).append("}) as structures ");

	    JsonObject params = new JsonObject().put("userIds", userIds);
	    neo.execute(query.toString(), params, validResultHandler(res->{
	        if (res.isRight()) {
	            final JsonArray result = res.right().getValue();
	            handler.handle(new Either.Right<>(result));
	        } else {
	            handler.handle(res);
	        }
	    }));
	}

	public void getAttachmentSchool(String userId, JsonArray structuresToExclude, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (u:User {id : {userId}})-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure) WHERE NOT s.id IN {structuresIds} " +
				"RETURN s.id AS id, s.name AS name";

		JsonObject params = new JsonObject()
				.put("userId", userId)
				.put("structuresIds", structuresToExclude);

		neo.execute(query, params, validUniqueResultHandler(res->{
			if (res.isRight()) {
				final JsonObject result = res.right().getValue();
				handler.handle(new Either.Right<>(result));
			} else {
				handler.handle(res);
			}
		}));
	}

	@Override
	public Future<JsonObject> getUsersDisplayNames(JsonArray userIds) {
		Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:User) WHERE u.id IN {userIds} " +
				"RETURN u.id AS userId, u.displayName AS displayName";
		JsonObject params = new JsonObject();
		params.put("userIds", userIds);

		neo.execute(query, params, validResultHandler(results -> {
			if (results.isRight()) {
				promise.complete(new JsonObject(results.right().getValue().stream()
						.map(r -> (JsonObject) r)
						.collect(Collectors.toMap(result -> result.getString("userId"), result -> result.getString("displayName")))));
			} else {
				promise.fail(results.left().getValue());
			}
		}));
		return promise.future();
	}
	
    /**
	 * 
     * List users for one or more structure using uai, with fields on cycle of the user like mergeIneDate, mergedIds, deletedDate...
     * @param structures List of structures UAI to process
     * @param results final handler
	 * 					- Left if error
	 * 					- Right if success with a JsonArray of users
	 * 						- Each user is a JsonObject with fields
	 * 							- externalId: the external id of the user
	 * 							- lastName: the last name of the user
	 * 							- firstName: the first name of the user
	 * 							- login: the login of the user
	 * 							- email: email address
	 * 							- emailAcademy: email académique
	 * 							- mobile: mobile phone number
	 * 							- deleteDate: date of deletion
	 * 							- functions: list of functions
	 * 							- displayName: the display name of the user
	 * 							- id: the id of the user
	 * 							- blocked: true if the user is blocked
	 * 							- emailInternal: internal email
	 * 							- profiles: user profile
	 * 							- UAI: the uai of the structure
	 * 							- isActive: true if the user is active
	 * 							- structures: id of the structures
	 * 							- groups: list of groups
	 * 							- createdDate: date of creation
	 * 							- duplicated: login of the duplicated user
	 * 							- isMergedWithINE: true if the user has been merged with an INE
	 * 							- automaticMergedIds: list of ids of users that have been automatically merged with this user
	 * 							- isMergedManuel: true if the user has been manually merged
	 * 
     */
	public void listUsersByStructure(List<String> structures, Handler<Either<String, JsonArray>> results) {
		if (structures == null || structures.isEmpty()) {
			results.handle(new Either.Left<>("missing.uai"));
			return;
		}

		JsonArray fields = new JsonArray().add("externalId").add("lastName").add("firstName").add("login");
		fields.add("email").add("emailAcademy").add("mobile").add("deleteDate").add("functions").add("displayName");
		fields.add("id").add("blocked").add("emailInternal");

		JsonArray specificFields = new JsonArray().add(" head(u.profiles) as profiles").add(", s.UAI as UAI")
				.add(", NOT EXISTS(u.activationCode) as isActive").add(", s.externalId as structures")
				.add(", collect(distinct {groupName:g.name, groupId:g.id}) as groups").add(", u.created as createdDate")
				.add(", u2.login as duplicated").add(", EXISTS(u.mergeIneDate) as isMergedWithINE")
				.add(", u.mergedIds as automaticMergedIds");

		StringBuilder query = new StringBuilder();
		JsonObject params = new JsonObject().put("uai", new JsonArray(structures));

		query.append("MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)")
				.append("<-[:IN]-(u:User) ");

		String filter = "WHERE s.UAI IN {uai} ";

		query.append(filter);
		query.append(" OPTIONAL MATCH (g:Group)<-[:IN]-u");
		query.append(" OPTIONAL MATCH u-[r:DUPLICATE]-(u2:User)");

		query.append(" RETURN DISTINCT ");
		for (Object field : fields) {
			query.append(" u.").append(field);
			query.append(" as ").append(field).append(",");
		}

		for (Object specificField : specificFields) {
			query.append(specificField);
		}
		query.append(", EXISTS(u.mergedLogins) as isMergedManuel");

		// Union with the backup to get the deleted users that are not in the structure anymore
		query.append(" UNION MATCH (p:Structure) WHERE p.UAI IN {uai} WITH p.id as id ");
		query.append(" MATCH (b:Backup) WHERE id IN b.structureIds WITH id, b ");
		query.append(" MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b) WHERE (EXISTS(u.deleteDate) OR EXISTS(u.mergedWith)) ");
		query.append(" MATCH (s:Structure ) WHERE s.id=id ");
		query.append(" OPTIONAL MATCH (g:Group) WHERE g.id IN b.IN_OUTGOING");
		query.append(" OPTIONAL MATCH u-[r:DUPLICATE]-(u2:User)");

		query.append(" RETURN DISTINCT ");
		for (Object field : fields) {
			query.append(" u.").append(field);
			query.append(" as ").append(field).append(",");
		}

		for (Object specificField : specificFields) {
			query.append(specificField);
		}
		query.append(", EXISTS(u.mergedWith) as isMergedManuel");


		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public Future<JsonArray> getAttachmentInfos(JsonArray userIds, JsonArray structuresSources) {
		final Promise<JsonArray> promise = Promise.promise();
		final String query =
				"MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"WHERE u.id IN {userIds} and s.source IN {structuresSources} and has(s.UAI) " +
				"RETURN u.id AS userId, u.externalId AS externalId, COLLECT(DISTINCT s.UAI) as structuresUAI";
		final JsonObject params = new JsonObject();
		params.put("userIds", userIds);
		params.put("structuresSources", structuresSources);

		neo.execute(query, params, validResultHandler(results -> {
			if (results.isRight()) {
				promise.complete(results.right().getValue());
			} else {
				promise.fail(results.left().getValue());
			}
		}));
		return promise.future();
	}

}
