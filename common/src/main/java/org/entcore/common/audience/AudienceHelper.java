package org.entcore.common.audience;

import fr.wseduc.rest.IRestClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AudienceHelper {
    private final IRestClient restClient;
    private final String platformId;
    private final String module;
    private final String resourceType;
    public AudienceHelper(final Vertx vertx, final JsonObject config) {
        final JsonObject audienceConfig = config.getJsonObject("audience", new JsonObject());
        this.platformId = (String) vertx.sharedData().getLocalMap("server").get("platformId");
        this.module = audienceConfig.getString("module", "na");
        this.resourceType = audienceConfig.getString("resource-type", "na");
        restClient = RestClientFactoryProvider.init();
    }
    public Future<ViewIncrementationResponse> onView(final ViewIncrementationRequest request) {
        final Map<String, String> params = createCommonParameters();
        return restClient.post("/viewscounters/{platformId}/{module}/{resourceType}", request, params, ViewIncrementationResponse.class);
    }

    public Future<ViewsResponse> getViews(final String... resources) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceIds", String.join(",", resources));
        return restClient.get("/viewscounters/{platformId}/{module}/{resourceType}?resourceIds={resourceIds}", params, ViewsResponse.class);
    }

    public Future<ViewDetailsResponse> getViewDetails(final String resourceId) {
        final Map<String, String> params = createCommonParameters();
        params.put("resourceId", resourceId);
        return restClient.get("/viewscounters/{platformId}/{module}/{resourceType}/{resourceId}", params, ViewDetailsResponse.class);
    }

    private Map<String, String> createCommonParameters() {
        final Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        params.put("module", module);
        params.put("resourceType", resourceType);
        return params;
    }
}
