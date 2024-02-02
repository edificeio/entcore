package org.entcore.common.audience;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.entcore.common.user.UserInfos;

import fr.wseduc.rest.IRestClient;
import fr.wseduc.rest.RestClientProvider;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AudienceHelper {
    private final IRestClient restClient;
    private final String platformId;
    private final String module;
    private final String resourceType;
    private static final Set<String> headersToForward = new HashSet<>();
    static {
        headersToForward.add("x-app");
        headersToForward.add("x-app-name");
        headersToForward.add("x-app-version");
        headersToForward.add("user-agent");
    }

    public AudienceHelper(final String appName, final Vertx vertx, final JsonObject config) {
        final JsonObject audienceConfig = config.getJsonObject("audience", new JsonObject());
        this.platformId = (String) vertx.sharedData().getLocalMap("server").get("platformId");
        this.module = audienceConfig.getString("module", "na");
        this.resourceType = audienceConfig.getString("resource-type", "na");
        restClient = RestClientProvider.getClient(appName, "audience", vertx);
    }

    public Future<ViewIncrementationResponse> incrementView(
            final ViewIncrementationRequest request,
            final HttpServerRequest httpRequest,
            final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        return restClient.post("/view/counters/{platformId}/{module}/{resourceType}", request, params,
                getHeadersToForward(httpRequest),
                ViewIncrementationResponse.class);
    }

    public Future<ViewsResponse> getViews(final UserInfos user,
            final HttpServerRequest httpRequest, final String... resources) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceIds", String.join(",", resources));
        return restClient.get("/view/counters/{platformId}/{module}/{resourceType}?resourceIds={resourceIds}", params,
                getHeadersToForward(httpRequest), ViewsResponse.class);
    }

    public Future<ViewDetailsResponse> getViewDetails(final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", resourceId);
        return restClient.get("/view/details/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ViewDetailsResponse.class);
    }

    public Future<ViewDetailsResponse> getReactionsSummary(final Set<String> resourceIds,
            final HttpServerRequest httpRequest, final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", String.join(",", resourceIds);
        return restClient.get("/view/details/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ViewDetailsResponse.class);
    }

    public Future<ReactionDetailsResponse> upsertReaction(final String reactionType, final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", resourceId);
        final ReactionCreationRequest request = new ReactionCreationRequest();
        return restClient.post("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest),
                ReactionDetailsResponse.class);
    }

    public Future<ReactionDetailsResponse> deleteReaction(final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", resourceId);
        final ReactionCreationRequest request = new ReactionCreationRequest();
        return restClient.delete("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest),
                ReactionDetailsResponse.class);
    }

    public Future<ReactionDetailsResponse> onDeletedResource(final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", resourceId);
        final ReactionCreationRequest request = new ReactionCreationRequest();
        return restClient.delete("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ReactionDetailsResponse.class);
    }

    public Future<ReactionDetailsResponse> onDeletedUser(final String userId,
            final HttpServerRequest httpRequest) {
        final Map<String, String> params = createCommonParameters();
        params.put("userId", userId);
        final ReactionCreationRequest request = new ReactionCreationRequest();
        return restClient.delete("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ReactionDetailsResponse.class);
    }

    private Map<String, String> createCommonParameters() {
        final Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        params.put("module", module);
        params.put("resourceType", resourceType);
        return params;
    }

    private Map<String, String> getHeadersToForward(HttpServerRequest httpRequest) {
        final Map<String, String> headersToFroward = new HashMap<>();
        httpRequest.headers().forEach(header -> {
            final String headerName = header.getKey();
            if (headersToForward.contains(headerName)) {
                headersToFroward.put("x-forwarded-" + headerName, header.getValue());
            }
        });
        return headersToFroward;
    }
}
