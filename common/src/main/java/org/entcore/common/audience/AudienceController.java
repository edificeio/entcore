package org.entcore.common.audience;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AudienceController extends BaseController {

    private final AudienceAccessFilter accessFilter;
    private final AudienceHelper audienceHelper;
    private final String resourceType;

    public AudienceController(AudienceAccessFilter accessFilter, String appName, String resourceType, Vertx vertx,
            JsonObject config) {
        this.accessFilter = accessFilter;
        this.resourceType = resourceType;
        this.audienceHelper = new AudienceHelper(appName, vertx, config);
    }

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);

        get("/reaction/" + resourceType + "", "getReactionsSummary");
        get("/reaction/" + resourceType + "/:resourceId", "getReactionsDetails");
    }

    public void getReactionsSummary(final HttpServerRequest request) {
        final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceId", request);
        verify(resourceIds, request)
                .onSuccess(user -> audienceHelper.getReactionsSummary(resourceIds, request, user));
    }

    public void getReactionsDetails(final HttpServerRequest request) {
        final Set<String> resourceIds = new HashSet<>();
        resourceIds.add(request.getParam("resourceId"));
        verify(resourceIds, request)
                .onSuccess(user -> audienceHelper.getReactionsSummary(resourceIds, request, user));
    }

    public Future<UserInfos> verify(
            final Set<String> resourceIds,
            final HttpServerRequest request) {
        final Promise<UserInfos> promise = Promise.promise();
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            accessFilter.canAccess(resourceIds, resourceIds).onSuccess(canAccess -> {
                if (canAccess) {
                    promise.complete(user);
                } else {
                    Renders.forbidden(request);
                }
            }).onFailure(th -> {
                log.error("Error while doing sthing", th);
                Renders.renderError(request);
            });
            audienceHelper.getReactionsSummary(resourceIds, request, user);
        });
        return promise.future();
    }
}
