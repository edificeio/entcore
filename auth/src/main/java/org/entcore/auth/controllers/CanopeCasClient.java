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
                String userIdentifier = authData.getUser();
                List<Node> userProps = authData.getAttributes().getAny();
                JsonObject userData = new JsonObject();
                for(Node o : userProps)
                    userData.put(o.getLocalName(), o.getTextContent());

                log.info("CANOPE USER " + userIdentifier + " >>> " + userData);

                String query = "MATCH (u:User) " +
                                "WHERE u.email = {email} AND HEAD(u.profiles) = 'Teacher' " +
                                "RETURN u.id AS id, u.login AS login";
                JsonObject params = new JsonObject().put("email", userIdentifier);

                request.pause();
                neo4j.execute(query, params, new Handler<Message<JsonObject>>()
                {
                    @Override
                    public void handle(Message<JsonObject> result)
                    {
                        String status = result.body().getString("status");
                        JsonArray resultArray = result.body().getJsonArray("result");

                        if("ok".equals(status) && resultArray != null && resultArray.size() == 1)
                        {
                            String inputHash = Sha1.hash(userIdentifier + "CANOP3" + eligibiliteTneNoticeId);
                            String outputHash = Sha1.hash(eligibiliteTneNoticeId + "CANOP3" + userIdentifier);

                            HttpClientRequest tneRequest = httpClient.postAbs(eligibiliteTneUrl, new Handler<HttpClientResponse>()
                            {
                                @Override
                                public void handle(final HttpClientResponse response)
                                {
                                    log.info("TNE RETURN >>> " + response.statusCode());
                                    if (response.statusCode() == 200)
                                    {
                                        response.bodyHandler(new Handler<Buffer>()
                                        {
                                            @Override
                                            public void handle(Buffer tneBuffer)
                                            {
                                                request.resume();
                                                log.info("TNE BODY >>> " + tneBuffer.toString());
                                                JsonObject tneResult = new JsonObject(tneBuffer.toString());

                                                switch(tneResult.getInteger("errorCode"))
                                                {
                                                    case 0:
                                                        if(outputHash.equals(tneResult.getString("hash")))
                                                            createSession(resultArray.getJsonObject(0).getString("id"), resultArray.getJsonObject(0).getString("login"), request);
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
                                    else
                                    {
                                        log.error("Canope TNE eligibility error: " + response.statusCode() + " " + response.headers());
                                        request.resume();
                                        request.response().setStatusCode(response.statusCode());
                                        request.response().end();
                                    }
                                }
                            });

                            JsonObject tneBody = new JsonObject()
                                                    .put("tx_cndpusager_usagercndp[verbe]", eligibiliteTneVerbe)
                                                    .put("login", userIdentifier)
                                                    .put("noticeid", eligibiliteTneNoticeId)
                                                    .put("hash", inputHash);

                            log.info("TNE REQUEST >>> " + tneBody);

                            addHeaders(tneRequest);
                            tneRequest.putHeader("Content-Type", "application/json");
                            tneRequest.putHeader("Accept", "application/json");
                            tneRequest.end(tneBody.toString());
                        }
                        else
                            Renders.unauthorized(request, "User not found");
                    }
                });
            }
        });
    }

    @Get("/canope/logout")
    public void logout(HttpServerRequest request)
    {
        super.logout(request);
    }

}
