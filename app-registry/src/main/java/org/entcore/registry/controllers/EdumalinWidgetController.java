package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
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

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);

        this.httpClient = vertx.createHttpClient(new HttpClientOptions());    // create http client
        JsonObject edumalinConfig = config.getJsonObject("edumalin-widget-config");    // get edumalin object from config
        this.edumalinUrl = edumalinConfig.getString("url", ""); // url of edumalin domain get from config
        this.usernameEdumalin = edumalinConfig.getString("username", "");    // username for edumalin get from config
        this.passwordEdumalin = edumalinConfig.getString("password", "");    // password for edumalin get from config
    }


    /**
     * @param request return response json with edumalin widget if success or error if not
     *                when token is null or empty then get new token and then get edumalin widget
     *                when token is not null or empty then get edumalin widget
     */
    @Get("/edumalin/widget")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @MfaProtected()
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

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos infos) {

                httpClient.getAbs(edumalinUrl + "/widget/getDisplay/" + (infos.getType().equals("ELEVE") ? "Student" : "Teacher"), response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("success") && json.getBoolean("success")
                                    && json.containsKey("data") && !json.getJsonArray("data").isEmpty()) {
                                renderJson(request, json);
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
                }).putHeader("Authorization", "Bearer " + edumalinToken).putHeader("Content-Type", "application/json").end();
            }
        });
    }

    /**
     * @param request http request
     *                return response json with edumalin token if success or error if not
     *                when response status code 200 then add token and then get edumalin widget
     */
    private void authLoginEdumalin(final HttpServerRequest request) {

        String requestBody = new JsonObject().put("username", usernameEdumalin).put("password", passwordEdumalin).toString();
        httpClient.postAbs(edumalinUrl + "/auth/login", response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("success") && json.getBoolean("success") && json.containsKey("data") && json.getJsonObject("data").containsKey("token")) {
                                edumalinToken = json.getJsonObject("data").getString("token");
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
                }).putHeader("Content-Type", "application/json")
                .putHeader("Content-Length", Integer.toString(requestBody.length()))
                .write(requestBody).end();
    }
}