package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class EdumalinWidgetController extends BaseController {

    private String edumalinToken;    // token for edumalin that will be used to get widget
    private HttpClient httpClient;    // http client to get widget from edumalin
    private String edumalinUrl;    // url of edumalin domain
    private String usernameEdumalin;    // username for edumalin
    private String passwordEdumalin;    // password for edumalin
    private String referrerEdumalin;    // referrer for edumalin creation token

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);

        this.httpClient = vertx.createHttpClient(new HttpClientOptions());    // create http client
        JsonObject edumalinConfig = config.getJsonObject("edumalin-widget-config");    // get edumalin object from config
        this.edumalinUrl = edumalinConfig.getString("url", ""); // url of edumalin domain get from config
        this.usernameEdumalin = edumalinConfig.getString("username", "");    // username for edumalin get from config
        this.passwordEdumalin = edumalinConfig.getString("password", "");    // password for edumalin get from config
        this.referrerEdumalin = edumalinConfig.getString("referrer", "");   // referrer for edumalin creation token get from config
    }


    /**
     * @param request return response json with edumalin widget if success or error if not
     *                when token is null or empty then get new token and then get edumalin widget
     *                when token is not null or empty then get edumalin widget
     */
    @Get("/edumalin/widget")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void widgetEdumalin(final HttpServerRequest request) {
        if (edumalinToken == null || edumalinToken.isEmpty()) {
            authLoginEdumalin(request);
        } else {
            displayWidgetEdumalin(request, true);
        }

    }

    /**
     * @param request http request
     *                return response json with edumalin widget if success or error if not
     *                when response code 401 then get new token and retry
     *                retryEdumalinToken is true if we want to retry to get token if response code 401, it's used to avoid infinite loop
     */
    private void displayWidgetEdumalin(final HttpServerRequest request, boolean retryGetEdumalinToken) {

        UserUtils.getUserInfos(eb, request, infos ->
                httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(edumalinUrl + "/widget/getDisplay/" + (infos.getType().equals("Student") ? "Student" : "Teacher"))
                        .setHeaders(new HeadersMultiMap()
                                .add("Authorization", "Bearer " + edumalinToken)
                                .add("Content-Type", "application/json")))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("success") && json.getBoolean("success")) {
                                if(json.containsKey("data") && !json.getJsonArray("data").isEmpty()) {
                                    renderJson(request, json);
                                } else {
                                    renderJson(request, new JsonObject().put("data", new JsonArray()));
                                }
                            } else {
                                JsonObject error = new JsonObject().put("error", "edumalin.widget.missing");
                                badRequest(request, error.toString());
                            }
                        });
                    } else if (response.statusCode() == 401 && retryGetEdumalinToken) {
                        authLoginEdumalin(request);
                    } else {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("error")) {
                                badRequest(request, json.toString());
                            } else {
                                JsonObject error = new JsonObject().put("error", "edumalin.widget.unauthorized");
                                badRequest(request, error.toString());
                            }
                        });
                    }
                }));
    }

    /**
     * @param request http request
     *                return response json with edumalin token if success or error if not
     *                when response status code 200 then add token and then get edumalin widget
     */
    @SuppressWarnings("deprecation")
    private void authLoginEdumalin(final HttpServerRequest request) {

        String requestBody = new JsonObject().put("username", usernameEdumalin).put("password", passwordEdumalin).toString();
        httpClient.request(new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setAbsoluteURI(referrerEdumalin + "auth/login")
                .setHeaders(new HeadersMultiMap()
                        .add("Content-Type", "application/json")
                        .add("Content-Length", Integer.toString(requestBody.length()))))
                .flatMap(serviceRequest -> serviceRequest.send(requestBody))
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("data") && !json.getString("data").isEmpty()) {
                                edumalinToken = json.getString("data");
                                displayWidgetEdumalin(request, false);
                            } else {
                                JsonObject error = new JsonObject().put("error", "edumalin.widget.unauthorized");
                                badRequest(request, error.toString());
                            }
                        });
                    } else {
                        edumalinToken = null;
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("error")) {
                                badRequest(request, json.toString());
                            } else {
                                JsonObject error = new JsonObject().put("error", "edumalin.widget.unauthorized");
                                badRequest(request, error.toString());
                            }
                        });
                    }
                });
    }
}
