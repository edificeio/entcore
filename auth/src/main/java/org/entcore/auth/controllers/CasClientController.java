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

import java.util.Map;
import java.net.URLEncoder;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;

import edu.yale.tp.cas.AuthenticationFailureType;
import edu.yale.tp.cas.AuthenticationSuccessType;
import edu.yale.tp.cas.ServiceResponseType;

import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.webutils.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;

import org.entcore.auth.controllers.AuthController;

public class CasClientController extends BaseController
{
    public String loginURL = "";
    public String logoutURL = "";
    public String serviceValidateURL = "";

    public String serviceRedirection = "";
    public String loginRedirection = "";
    public String logoutRedirection = "";

    public JsonObject headers;

    public HttpClient httpClient;
    public AuthController authController;

    public CasClientController(AuthController authController)
    {
        this.authController = authController;
    }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions)
    {
        super.init(vertx, config, rm, securedActions);

        this.httpClient = vertx.createHttpClient(new HttpClientOptions());
    }

    protected void loadURLEndpoints(JsonObject configSnippet)
    {
        if(configSnippet != null)
        {
            this.loginURL = configSnippet.getString("login-url", "");
            this.logoutURL = configSnippet.getString("logout-url", "");
            this.serviceValidateURL = configSnippet.getString("serviceValidate-url", "");

            this.headers = configSnippet.getJsonObject("headers", new JsonObject());

            try
            {
                this.serviceRedirection = URLEncoder.encode(configSnippet.getString("service-redirection", ""), "UTF-8");
                this.logoutRedirection = URLEncoder.encode(configSnippet.getString("logout-redirection", ""), "UTF-8");
            }
            catch(UnsupportedEncodingException e) {}
            this.loginRedirection = configSnippet.getString("login-redirection", "");
        }
	}

    protected HttpClientRequest addHeaders(HttpClientRequest req)
    {
        if(this.headers != null) {
            for (String header : headers.fieldNames()) {
                req.putHeader(header, headers.getString(header));
            }
        }
        return req;
    }

    private void redirectToCas(HttpServerRequest request, String url, String redirection)
    {
		request.response().setStatusCode(302);
		request.response().putHeader("Location", url + "?service=" + redirection);
		request.response().end();
    }

    protected void login(HttpServerRequest request)
    {
        this.redirectToCas(request, this.loginURL, this.serviceRedirection);
    }

    protected void serviceValidate(HttpServerRequest request, Handler<AuthenticationSuccessType> handler) {
        request.pause();
        String validateURL = this.serviceValidateURL + "?service=" + this.serviceRedirection + "&ticket=" + request.getParam("ticket");
        this.httpClient.request(new RequestOptions()
            .setMethod(HttpMethod.GET)
            .setAbsoluteURI(validateURL)
        )
        .map(this::addHeaders)
        .flatMap(HttpClientRequest::send)
        .onSuccess(response -> {
            if (response.statusCode() == 200) {
                response.bodyHandler(bodyBuffer -> {
                    request.resume();
                    try {
                        JAXBContext context = JAXBContext.newInstance(ServiceResponseType.class);
                        Unmarshaller unmarshaller = context.createUnmarshaller();
                        StringReader bodyReader = new StringReader(bodyBuffer.toString());

                        ServiceResponseType serviceResponse = (ServiceResponseType) JAXBIntrospector.getValue(unmarshaller.unmarshal(bodyReader));
                        AuthenticationFailureType failure = serviceResponse.getAuthenticationFailure();
                        AuthenticationSuccessType success = serviceResponse.getAuthenticationSuccess();

                        if (failure == null)
                            handler.handle(success);
                        else {
                            String failureCode = failure.getCode();
                            String failureValue = failure.getValue();

                            log.error("CAS Client serviceValidate Error: " + failureCode + " (" + failureValue + ")");

                            request.response().setStatusCode(500);
                            request.response().end();
                        }
                    } catch (Exception e) {
                        log.error(e.toString());
                        request.response().setStatusCode(500);
                        request.response().end();
                    }
                });
            } else {
                log.error("CAS Client serviceValidate error: " + response.statusCode());
                request.response().setStatusCode(response.statusCode());
                request.response().end();
            }
        });
    }

    protected void loginUser(String userId, String login, HttpServerRequest request)
    {
        authController.loginUser(userId, login, request, this.loginRedirection);
    }

    protected void logout(HttpServerRequest request)
    {
        this.redirectToCas(request, this.logoutURL, this.logoutRedirection);
    }

}
