package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;
import org.entcore.registry.services.EsidocService;
import org.entcore.registry.services.impl.DefaultEsidocService;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class EsidocController extends BaseController {

    private EsidocService esidocService;

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
        this.esidocService = new DefaultEsidocService(vertx, config.getJsonObject("esidoc-config"));
    }

    /**
     * Loans of the connected user, displayed by the library widget of the homepage.
     * A student gets the detail of his own loans, a relative only gets the number of books borrowed by each child.
     * Any other profile gets no loan, as does a user unknown to e-sidoc.
     */
    @Get("/esidoc/loans")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLoans(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            final Future<JsonObject> loans;
            if ("Student".equals(user.getType())) {
                loans = esidocService.getLoans(user.getExternalId(), user.getUai())
                        .map(studentLoans -> loansResponse(studentLoans, new JsonArray()));
            } else if ("Relative".equals(user.getType())) {
                loans = esidocService.getChildrenLoansCount(user.getChildrenIds())
                        .map(childrenLoansCount -> loansResponse(new JsonArray(), childrenLoansCount));
            } else {
                loans = Future.succeededFuture(loansResponse(new JsonArray(), new JsonArray()));
            }
            loans.onSuccess(response -> renderJson(request, response))
                    .onFailure(err -> {
                        log.error("[e-sidoc] Failed to fetch the loans of user " + user.getUserId(), err);
                        renderJson(request, new JsonObject().put("error", "esidoc.loans.unavailable"), 502);
                    });
        });
    }

    private static JsonObject loansResponse(JsonArray loans, JsonArray children) {
        return new JsonObject().put("loans", loans).put("children", children);
    }
}
