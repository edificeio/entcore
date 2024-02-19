/*
 * Copyright © "Open Digital Education", 2023
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

package org.entcore.auth.services.impl;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.validation.StringValidation;
import org.opensaml.saml2.core.Assertion;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.isNotEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;

public class SSOAzure extends AbstractSSOProvider {

	private static final Logger log = LoggerFactory.getLogger(SSOAzure.class);

	protected static final String EMAIL_ATTTRIBUTE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
	protected static final String ENTPERSONJOINTURE_ATTTRIBUTE = "ENTPersonJointure";
	protected static final String ID_ATTTRIBUTE = "ID";
	protected static final String LASTNAME_ATTTRIBUTE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";
	protected static final String FIRSTNAME_ATTTRIBUTE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";
	protected static final String PROFILE_ATTTRIBUTE = "ENTProfil";
	protected static final String BIRTHDATE_ATTTRIBUTE = "DateDeNaissance";
	protected static final String UAI_ATTTRIBUTE = "UAI";
	protected static final String CLASSES_ATTTRIBUTE = "Classes";
	protected static final String DEFAULT_NOT_EXISTS = "__NOT_EXISTS__";
	private static final Map<String, String> profilesMapping = Collections.unmodifiableMap(new HashMap<String, String>() {{
		put("Enseignant","Teacher");
		put("Eleve","Student");
	}});

	private static final int TX_AAF_QUERY_NB = 3;
	private static final int TX_NOAAF_QUERY_NB = 2;
	private static final String FEEDER = "entcore.feeder";
	private static final List<String> PROFILES = Arrays.asList("Personnel", "Teacher", "Student", "Relative", "Guest");

	private final Neo4j neo4j;
	private final EventBus eb;
	private final JsonObject issuerAcademiePrefix;
	private final JsonObject defaultStructureId;
	private final JsonObject profilesAllowedToDefaultStructure;

	public SSOAzure() {
		this(
				Vertx.currentContext().config().getJsonObject("azure-issuer-academy-prefix", new JsonObject()),
				Vertx.currentContext().config().getJsonObject("azure-default-structure-id"),
				Vertx.currentContext().config().getJsonObject("azure-profiles-allowed-to-default-structure")
		);
	}

	public SSOAzure(JsonObject issuerAcademiePrefix, JsonObject defaultStructureId, JsonObject profilesAllowedToDefaultStructure) {
		this.issuerAcademiePrefix = issuerAcademiePrefix;
		this.defaultStructureId = defaultStructureId;
		this.profilesAllowedToDefaultStructure = profilesAllowedToDefaultStructure;
		this.neo4j = Neo4j.getInstance();
		this.eb = this.neo4j.getEventBus();
	}

	@Override
	public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (!validConditions(assertion, handler)) return;

		createOrMergeUserIfNeeded(assertion, ar -> {
			if (ar.succeeded()) {
				final String entPersonJointure = getAttribute(assertion, ENTPERSONJOINTURE_ATTTRIBUTE);
				if (isNotEmpty(entPersonJointure)) {
					final String externalId = getExternalId(assertion, entPersonJointure);
					executeQuery("MATCH (u:User {externalId:{joinKey}}) ", new JsonObject().put("joinKey", externalId),
							assertion, handler);
				} else if ("Student".equals(getProfile(getAttribute(assertion, PROFILE_ATTTRIBUTE)))) {
					final String externalId = getAttribute(assertion, ID_ATTTRIBUTE);
					if (isEmpty(externalId)) {
						handler.handle(new Either.Left<>("invalid.externalId"));
						return;
					}
					executeQuery("MATCH (u:User {externalId:{externalId}}) ",
							new JsonObject().put("externalId", externalId),
							assertion, handler);
				} else {
					final String mail = getAttribute(assertion, EMAIL_ATTTRIBUTE);
					if (isEmpty(mail)) {
						handler.handle(new Either.Left<>("invalid.email"));
						return;
					}
					if (StringValidation.isEmail(mail)) {
						executeQuery("MATCH (u:User {emailAcademy:{email}}) ", new JsonObject().put("email", mail),
								assertion, handler);
					} else {
						handler.handle(new Either.Left<>("invalid.email"));
					}
				}
			} else {
				log.error("Error when try create or merge sso user : ", ar.cause());
				handler.handle(new Either.Left<>("invalid.user"));
			}
		});
	}

	protected String getExternalId(Assertion assertion, String entPersonJointure) {
		if (issuerAcademiePrefix == null || issuerAcademiePrefix.isEmpty()) {
			return entPersonJointure;
		}
		return issuerAcademiePrefix.getString(assertion.getIssuer().getValue(), "") + entPersonJointure;
	}

	private void createOrMergeUserIfNeeded(Assertion assertion, Handler<AsyncResult<Void>> handler) {
		final String queryStructure =
				"MATCH (s:Structure {UAI: {uai}}) " +
						"OPTIONAL MATCH s<-[:BELONGS]-(c:Class) " +
						"RETURN DISTINCT s.id as structureId, COLLECT(DISTINCT [c.id, c.name]) as classes ";
		final String queryUserAAF =
				"MATCH (u:User {externalId:{joinKey}}) " +
						"RETURN u.id as id, u.source as source, u.activationCode as activationCode ";
		final String queryUser =
				"MATCH (u:User {externalId:{externalId}}) " +
						"RETURN u.id as id, u.externalId as externalId, u.source as source ";

		final StatementsBuilder statements = new StatementsBuilder();
		statements
				.add(queryStructure, new JsonObject().put("uai", getOrElse(getAttribute(assertion, UAI_ATTTRIBUTE), DEFAULT_NOT_EXISTS)))
				.add(queryUser, new JsonObject().put("externalId", getOrElse(getAttribute(assertion, ID_ATTTRIBUTE), DEFAULT_NOT_EXISTS)));

		final String entPersonJointure = getAttribute(assertion, ENTPERSONJOINTURE_ATTTRIBUTE);
		if (isNotEmpty(entPersonJointure)) {
			statements.add(queryUserAAF, new JsonObject().put("joinKey", getExternalId(assertion, entPersonJointure)));
		}
		Neo4j.getInstance().executeTransaction(statements.build(), null, true, Neo4jResult.validResultsHandler(res -> {
			if (res.isRight()) {
				final JsonArray results = res.right().getValue();
				if (results == null) {
					handler.handle(Future.failedFuture("Empty transaction result."));
					return;
				}
				if (results.size() == TX_AAF_QUERY_NB) {
					mergeAccountsIfNeeded(results, handler);
				} else if (results.size() == TX_NOAAF_QUERY_NB) {
					createUserIfNeeded(assertion, results, handler);
				} else {
					handler.handle(Future.failedFuture("Invalid tx result number."));
				}
			} else {
				handler.handle(Future.failedFuture(res.left().getValue()));
			}
		}));
	}

	private void mergeAccountsIfNeeded(JsonArray results, Handler<AsyncResult<Void>> handler) {
		final JsonArray ssoUsers = results.getJsonArray(1);
		final JsonArray aafUsers = results.getJsonArray(2);
		if (ssoUsers != null && ssoUsers.size() == 1 && aafUsers != null && aafUsers.size() == 1) {
			final JsonObject ssoUser = ssoUsers.getJsonObject(0);
			final JsonObject aafUser = aafUsers.getJsonObject(0);
			if (aafUser != null && isNotEmpty(aafUser.getString("activationCode"))) {
				mergeUser(aafUser.getString("id"), ssoUser.getString("id"), handler);
			} else if (aafUser != null && isEmpty(aafUser.getString("activationCode"))) {
				predeleteUser(ssoUser.getString("id"), handler);
			} else {
				handler.handle(Future.succeededFuture());
			}
		} else {
			log.info("No user to merge.");
			handler.handle(Future.succeededFuture());
		}
	}

	private void mergeUser(String aafUserId, String ssoUserId, Handler<AsyncResult<Void>> handler) {
		if (isEmpty(aafUserId) || isEmpty(ssoUserId)) {
			handler.handle(Future.failedFuture("Invalid id for merge sso user."));
			return;
		}
		final String markDuplicates =
				"MATCH (u:User {id:{aafUserId}}), (d:User {id:{ssoUserId}}) " +
						"MERGE u-[:DUPLICATE {score:{score}}]-d";
		final JsonObject params = new JsonObject()
				.put("aafUserId", aafUserId)
				.put("ssoUserId", ssoUserId)
				.put("score", 4);
		Neo4j.getInstance().execute(markDuplicates, params, Neo4jResult.validEmptyHandler(res -> {
			if (res.isRight()) {
				final JsonObject action = new JsonObject()
						.put("action", "merge-duplicate")
						.put("userId1", aafUserId)
						.put("userId2", ssoUserId)
						.put("keepRelations", false);
				eb.request(FEEDER, action, res2 -> {
					if (res2.succeeded() && "ok".equals(((JsonObject) res2.result().body()).getString("status"))) {
						handler.handle(Future.succeededFuture());
					} else {
						handler.handle(Future.failedFuture("Error when merge-duplicate sso : " +
								((JsonObject) res2.result().body()).getString("message")));
					}
				});
			} else {
				log.error("Error when mark duplicate sso user : " + res.left().getValue());
				handler.handle(Future.failedFuture(res.left().getValue()));
			}
		}));
	}

	private void predeleteUser(String userId, Handler<AsyncResult<Void>> handler) {
		if (isEmpty(userId)) {
			handler.handle(Future.failedFuture("Invalid id for predelete sso user."));
			return;
		}
		final JsonObject action = new JsonObject()
				.put("action", "manual-delete-user")
				.put("users", new JsonArray().add(userId));
		eb.request(FEEDER, action, res2 -> {
			if (res2.succeeded() && "ok".equals(((JsonObject) res2.result().body()).getString("status"))) {
				handler.handle(Future.succeededFuture());
			} else {
				handler.handle(Future.failedFuture("Error when manual-delete-user sso : " +
						((JsonObject) res2.result().body()).getString("message")));
			}
		});
	}

	protected String getDefaultStructureId(Assertion assertion) {
		if (defaultStructureId != null) {
			return defaultStructureId.getString(assertion.getIssuer().getValue());
		} else {
			return null;
		}
	}

	protected JsonArray getProfilesAllowedToDefaultStructure(Assertion assertion) {
		if (profilesAllowedToDefaultStructure != null) {
			return profilesAllowedToDefaultStructure.getJsonArray(assertion.getIssuer().getValue(), new JsonArray());
		} else {
			return new JsonArray();
		}
	}

	private void createUserIfNeeded(Assertion assertion, JsonArray results, Handler<AsyncResult<Void>> handler) {
		final JsonArray ssoUsers = results.getJsonArray(1);
		if (ssoUsers != null && ssoUsers.size() == 1) {
			handler.handle(Future.succeededFuture());
			return;
		}

		final String profile = getProfile(getAttribute(assertion, PROFILE_ATTTRIBUTE));
		if (!PROFILES.contains(profile)) {
			handler.handle(Future.failedFuture("invalid profile to create sso user."));
			return;
		}

		JsonArray structures = results.getJsonArray(0);
		if (structures == null || structures.size() == 0) {
			final String defaultSId = getDefaultStructureId(assertion);
			if (isNotEmpty(defaultSId) && getProfilesAllowedToDefaultStructure(assertion).contains(profile)) {
				structures = new JsonArray().add(new JsonObject().put("structureId", defaultSId));
			} else {
				handler.handle(Future.failedFuture("no structure found to create sso user."));
				return;
			}
		}

		final String birthDate = getAttribute(assertion, BIRTHDATE_ATTTRIBUTE);
		if ("Student".equals(profile) && isEmpty(birthDate)) {
			handler.handle(Future.failedFuture("invalid birthDate to create sso student."));
			return;
		}

		final JsonObject structure = structures.getJsonObject(0);
		final JsonObject user = new JsonObject()
				.put("externalId", getAttribute(assertion, ID_ATTTRIBUTE))
				.put("email", getAttribute(assertion, EMAIL_ATTTRIBUTE))
				.put("emailAcademy", getAttribute(assertion, EMAIL_ATTTRIBUTE))
				.put("firstName", getAttribute(assertion, FIRSTNAME_ATTTRIBUTE))
				.put("lastName", getAttribute(assertion, LASTNAME_ATTTRIBUTE))
				.put("birthDate", birthDate)
				.put("profile", profile)
				.put("profiles", new JsonArray().add(profile))
				.put("source", "SSO");
		final JsonObject action = new JsonObject()
				.put("action", "manual-create-user")
				.put("structureId", structure.getString("structureId"))
				.put("profile", profile)
				.put("classesNames", getClassesNames(assertion))
				.put("data", user);
		eb.request(FEEDER, action, res2 -> {
			if (res2.succeeded() && "ok".equals(((JsonObject) res2.result().body()).getString("status"))) {
				handler.handle(Future.succeededFuture());
			} else {
				handler.handle(Future.failedFuture("Error when manual-create-user sso : " +
						((JsonObject) res2.result().body()).getString("message")));
			}
		});
	}

	private static String getProfile(String attribute) {
		if (profilesMapping.containsKey(attribute)) {
			return profilesMapping.get(attribute);
		}
		return attribute;
	}

	private JsonArray getClassesNames(Assertion assertion) {
		final String classes = getAttribute(assertion, CLASSES_ATTTRIBUTE);
		if (isNotEmpty(classes)) {
			final JsonArray classesNames = new JsonArray(Arrays.asList(classes.split(",")));
			return classesNames;
		}
		return null;
	}

}