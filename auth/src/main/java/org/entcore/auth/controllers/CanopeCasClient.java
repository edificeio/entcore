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

package org.entcore.auth.controllers;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.xml.bind.JAXBElement;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.AsyncResult;

import edu.yale.tp.cas.AttributesType;
import edu.yale.tp.cas.AuthenticationSuccessType;

import org.vertx.java.core.http.RouteMatcher;
import org.w3c.dom.Node;

import fr.wseduc.rs.Get;
import fr.wseduc.webutils.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.Sha1;

import org.entcore.common.neo4j.Neo4j;

public class CanopeCasClient extends CasClientController
{
    public String eligibiliteTneUrl;
    public String eligibiliteTneVerbe;
    public Integer eligibiliteTneNoticeId;
    public String userCreationStructureId;

    private Neo4j neo4j = Neo4j.getInstance();

    public CanopeCasClient(AuthController authController)
    {
        super(authController);
    }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions)
    {
		super.init(vertx, config, rm, securedActions);

        JsonObject canopeConf = config.getJsonObject("canope-cas");
        this.loadURLEndpoints(canopeConf);

        if(canopeConf != null)
        {
            this.eligibiliteTneUrl = canopeConf.getString("eligibilite-tne-url");
            this.eligibiliteTneVerbe = canopeConf.getString("eligibilite-tne-verbe");
            this.eligibiliteTneNoticeId = canopeConf.getInteger("eligibilite-tne-notice-id");
            this.userCreationStructureId = canopeConf.getString("user-creation-structure-id");
        }
    }

    private static class CanopeUser
    {
        private String id;
        public String email;
        public String firstName;
        public String lastName;
        public String profile;

        private static final Map<String, String> ROLE_CODE_TO_PROFILE = new HashMap<String , String>() {{
            put("MEE", "Teacher");
            put("GEST", "Personnel");
            put("PART", "Guest");
            put("ASSO", "Guest");
        }};

        public CanopeUser(AuthenticationSuccessType authData)
        {
            List<Node> userProps = authData.getAttributes().getAny();
            JsonObject userData = new JsonObject();
            for(Node o : userProps)
                userData.put(o.getLocalName(), o.getTextContent());

            this.id = userData.getString("idAbonne");
            this.email = authData.getUser();
            this.firstName = userData.getString("prenomUser");
            this.lastName = userData.getString("nomUser");

            this.profile = ROLE_CODE_TO_PROFILE.get(userData.getString("roleCode"));
            if(this.profile == null)
                this.profile = "Guest";
        }

        public String getExternalId()
        {
            return "CANOPE-" + this.id;
        }

        public JsonObject toJson()
        {
            return new JsonObject()
                        .put("externalId", this.getExternalId())
                        .put("email", this.email)
                        .put("emailAcademy", this.email)
                        .put("firstName", this.firstName)
                        .put("lastName", this.lastName)
                        .put("profile", this.profile)
                        .put("profiles", new JsonArray().add(this.profile));
        }

        @Override
        public String toString()
        {
            return this.toJson().toString();
        }
    }

    @Get("/canope/login")
    public void login(HttpServerRequest request)
    {
        super.login(request);
    }

    @Get("/canope/serviceValidate")
    public void serviceValidate(HttpServerRequest request)
    {
        super.serviceValidate(request, new Handler<AuthenticationSuccessType>()
        {
            @Override
            public void handle(AuthenticationSuccessType authData)
            {
                CanopeUser user = new CanopeUser(authData);
                checkTneEligibilty(request, user);
            }
        });
    }

    private void loginOrCreateUser(HttpServerRequest request, CanopeUser user)
    {
        String query = "MATCH (u:User) " +
                        "WHERE (u.email = {email} OR u.emailAcademy = {email} OR u.externalId = {externalId}) AND HEAD(u.profiles) = {profile} " +
                        "RETURN u.id AS id, u.login AS login";
        JsonObject params = user.toJson();

        request.pause();
        neo4j.execute(query, params, new Handler<Message<JsonObject>>()
        {
            @Override
            public void handle(Message<JsonObject> result)
            {
                String status = result.body().getString("status");
                JsonArray resultArray = result.body().getJsonArray("result");

                if("ok".equals(status) == false || resultArray == null)
                {
                    Renders.renderError(request, new JsonObject().put("error", "canope.user.find.error"));
                    return;
                }

                if(resultArray.size() == 1)
                {
                    JsonObject neo4jRes = resultArray.getJsonObject(0);
                    loginUser(neo4jRes.getString("id"), neo4jRes.getString("login"), request);
                }
                else if(resultArray.size() == 0)
                {
                    if(userCreationStructureId == null)
                        Renders.unauthorized(request, "canope.user.find.none");
                    else
                    {
                        JsonObject action = new JsonObject()
                                .put("action", "manual-create-user")
                                .put("structureId", userCreationStructureId)
                                .put("profile", user.profile)
                                .put("data", user.toJson());

                        eb.send("entcore.feeder", action, new Handler<AsyncResult<Message<JsonObject>>>()
                        {
                            @Override
                            public void handle(AsyncResult<Message<JsonObject>> result)
                            {
                                if(result.succeeded())
                                {
                                    JsonObject creationBody = result.result().body();
                                    JsonArray creationResult = creationBody.getJsonArray("result");

                                    if("ok".equals(creationBody.getString("status")) && creationResult != null && creationResult.size() == 1)
                                    {
                                        JsonObject creationUser = creationResult.getJsonObject(0);
                                        loginUser(creationUser.getString("id"), creationUser.getString("login"), request);
                                    }
                                    else
                                    {
                                        String error = creationBody.getString("error", creationBody.getString("message", "canope.user.creation.error"));
                                        Renders.renderError(request, new JsonObject().put("error", error));
                                    }
                                }
                                else
                                    Renders.renderError(request, new JsonObject().put("error", result.cause().getMessage()));
                            }
                        });
                    }
                }
                else
                    Renders.renderError(request, new JsonObject().put("error", "canope.user.find.multiple"));
            }
        });
    }

    private void checkTneEligibilty(HttpServerRequest request, CanopeUser user)
    {
        String canopeId = user.email;

        String inputHash = Sha1.hash(canopeId + "CANOP3" + eligibiliteTneNoticeId);
        String outputHash = Sha1.hash(eligibiliteTneNoticeId + "CANOP3" + canopeId);

        HttpClientRequest tneRequest = httpClient.postAbs(eligibiliteTneUrl, new Handler<HttpClientResponse>()
        {
            @Override
            public void handle(final HttpClientResponse response)
            {
                response.bodyHandler(new Handler<Buffer>()
                {
                    @Override
                    public void handle(Buffer tneBuffer)
                    {
                        request.resume();
                        JsonObject tneResult = new JsonObject(tneBuffer.toString());

                        switch(tneResult.getInteger("errorCode"))
                        {
                            case 0:
                                if(outputHash.equals(tneResult.getString("hash")))
                                    loginOrCreateUser(request, user);
                                else
                                    Renders.renderError(request, new JsonObject().put("error", "tne.mismatch"));
                                break;
                            case 1:
                            case 3:
                                Renders.renderError(request, new JsonObject().put("error", "tne.request.error"));
                                break;
                            case 4:
                            case 5:
                                Renders.forbidden(request, "tne.ineligible");
                                break;
                            default:
                                Renders.renderError(request, new JsonObject().put("error", "tne.request.unknown"));
                                break;
                        }
                    }
                });
            }
        });

        JsonObject tneBody = new JsonObject()
                                .put("tx_cndpusager_usagercndp[verbe]", eligibiliteTneVerbe)
                                .put("login", canopeId)
                                .put("noticeid", eligibiliteTneNoticeId)
                                .put("hash", inputHash);

        addHeaders(tneRequest);
        tneRequest.putHeader("Content-Type", "application/json");
        tneRequest.putHeader("Accept", "application/json");
        tneRequest.end(tneBody.toString());
    }

    @Get("/canope/logout")
    public void logout(HttpServerRequest request)
    {
        super.logout(request);
    }

}
