package org.entcore.registry.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.cache.CacheService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.services.EsidocService;
import org.entcore.registry.util.EsidocUtils;

import java.util.ArrayList;
import java.util.List;

public class DefaultEsidocService implements EsidocService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEsidocService.class);
    /** Seconds removed from the access token lifetime, so that a token is never sent at the very end of its validity. */
    private static final int ACCESS_TOKEN_EXPIRY_MARGIN = 60;
    /** Cache key prefix of the e-sidoc loans responses, followed by the UAI and the borrower id. */
    private static final String LOANS_CACHE_PREFIX = "esidoc-loans:";
    /** Seconds during which a loans response is cached, short since the loans change as soon as a book is returned. */
    private static final int DEFAULT_LOANS_CACHE_TTL = 300;
    /** Milliseconds after which a request to e-sidoc is abandoned, so that the homepage widget does not wait for it. */
    private static final long DEFAULT_TIMEOUT = 10000L;
    private static final String CHILDREN_QUERY =
            "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
            "WHERE u.id IN {childrenIds} " +
            "RETURN u.id as id, u.firstName as firstName, u.externalId as externalId, COLLECT(distinct s.UAI) as uais";

    private final HttpClient httpClient;
    private final CacheService cacheService;    // null when Redis is not configured: the loans are then not cached
    private final Neo4j neo4j = Neo4j.getInstance();
    private final String authUrl;
    private final String apiUrl;
    private final String clientId;
    private final String clientSecret;
    private final int loansCacheTtl;
    private final long timeout;
    private String accessToken;
    private long accessTokenExpiresAt;
    private Future<String> pendingAccessToken;    // access token request shared by the concurrent requests

    public DefaultEsidocService(Vertx vertx, JsonObject config) {
        this.httpClient = vertx.createHttpClient(new HttpClientOptions());
        this.cacheService = createCacheService(vertx);
        this.authUrl = config.getString("auth-url", "");
        this.apiUrl = config.getString("api-url", "").replaceAll("/+$", "");
        this.clientId = config.getString("client-id", "");
        this.clientSecret = config.getString("client-secret", "");
        this.loansCacheTtl = config.getInteger("loans-cache-ttl", DEFAULT_LOANS_CACHE_TTL);
        this.timeout = config.getLong("timeout", DEFAULT_TIMEOUT);
    }

    @Override
    public Future<JsonArray> getLoans(String borrowerId, List<String> uais) {
        if (StringUtils.isEmpty(borrowerId) || uais == null || uais.isEmpty()) {
            return Future.succeededFuture(new JsonArray());
        }
        final List<Future<JsonArray>> loansByStructure = new ArrayList<>();
        for (String uai : uais) {
            loansByStructure.add(fetchLoans(uai, borrowerId).map(response -> EsidocUtils.currentLoans(response, uai)));
        }
        return succeededResults(loansByStructure).map(EsidocUtils::mergeByDueDate);
    }

    @Override
    public Future<JsonArray> getChildrenLoansCount(List<String> childrenIds) {
        if (childrenIds == null || childrenIds.isEmpty()) {
            return Future.succeededFuture(new JsonArray());
        }
        return findChildren(childrenIds).compose(children -> {
            final List<Future<JsonObject>> childrenLoansCount = new ArrayList<>();
            for (Object o : children) {
                if (!(o instanceof JsonObject)) {
                    continue;
                }
                final JsonObject child = (JsonObject) o;
                childrenLoansCount.add(getLoans(child.getString("externalId"), toStringList(child.getJsonArray("uais")))
                        .map(loans -> new JsonObject()
                                .put("id", child.getString("id"))
                                .put("firstName", child.getString("firstName"))
                                .put("loansCount", loans.size())));
            }
            return succeededResults(childrenLoansCount).map(loansCount -> new JsonArray(new ArrayList<Object>(loansCount)));
        });
    }

    /**
     * e-sidoc response describing the loans of a borrower in a structure, empty when e-sidoc does not know him there.
     * The responses are cached since the library widget is displayed on the homepage.
     */
    private Future<JsonObject> fetchLoans(String uai, String borrowerId) {
        final String cacheKey = LOANS_CACHE_PREFIX + uai + ":" + borrowerId;
        return getCachedLoans(cacheKey)
                .compose(cachedLoans -> {
                    if (cachedLoans != null) {
                        return Future.succeededFuture(cachedLoans);
                    }
                    return requestLoans(uai, borrowerId, true).onSuccess(loans -> cacheLoans(cacheKey, loans));
                })
                .onFailure(err -> log.error("[e-sidoc] Failed to fetch the loans of borrower " + borrowerId +
                        " in structure " + uai + ": " + err.getMessage()));
    }

    /**
     * @param retryOnUnauthorized whether to retry once with a new access token when e-sidoc rejects the current one,
     *                            false on the retry itself to avoid an infinite loop
     */
    private Future<JsonObject> requestLoans(String uai, String borrowerId, boolean retryOnUnauthorized) {
        return getAccessToken().compose(token -> httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(apiUrl + EsidocUtils.loansPath(uai, borrowerId))
                        .putHeader("Authorization", "Bearer " + token)
                        .putHeader("Accept", "application/json")
                        .setConnectTimeout(timeout)
                        .setIdleTimeout(timeout))
                .compose(HttpClientRequest::send)
                .compose(response -> response.body().compose(body -> {
                    if (response.statusCode() == 401 && retryOnUnauthorized) {
                        // The token may be revoked before its expiry
                        invalidateAccessToken(token);
                        return requestLoans(uai, borrowerId, false);
                    }
                    if (response.statusCode() == 404) {
                        // e-sidoc does not know the borrower in this structure
                        return Future.succeededFuture(new JsonObject());
                    }
                    if (response.statusCode() != 200) {
                        return Future.failedFuture("e-sidoc responded " + response.statusCode() + ": " + body);
                    }
                    return Future.succeededFuture(EsidocUtils.parseLoansResponse(body));
                })));
    }

    /**
     * @return a valid access token, requested to e-sidoc when there is none or when it expired
     */
    private Future<String> getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpiresAt) {
            return Future.succeededFuture(accessToken);
        }
        if (pendingAccessToken == null || pendingAccessToken.isComplete()) {
            pendingAccessToken = requestAccessToken();
        }
        return pendingAccessToken;
    }

    private Future<String> requestAccessToken() {
        final String form = "grant_type=client_credentials" +
                "&client_id=" + EsidocUtils.encode(clientId) +
                "&client_secret=" + EsidocUtils.encode(clientSecret);
        return httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.POST)
                        .setAbsoluteURI(authUrl)
                        .putHeader("Content-Type", "application/x-www-form-urlencoded")
                        .putHeader("Accept", "application/json")
                        .setConnectTimeout(timeout)
                        .setIdleTimeout(timeout))
                .compose(request -> request.send(form))
                .compose(response -> response.body().compose(body -> {
                    if (response.statusCode() != 200) {
                        return Future.failedFuture("e-sidoc token request responded " + response.statusCode() + ": " + body);
                    }
                    final JsonObject token = body.toJsonObject();
                    final String value = token.getString("access_token");
                    if (StringUtils.isEmpty(value)) {
                        return Future.failedFuture("e-sidoc token response has no access_token");
                    }
                    accessToken = value;
                    accessTokenExpiresAt = System.currentTimeMillis() +
                            Math.max(token.getInteger("expires_in", 0) - ACCESS_TOKEN_EXPIRY_MARGIN, 0) * 1000L;
                    return Future.succeededFuture(value);
                }));
    }

    private void invalidateAccessToken(String token) {
        // A concurrent request may already have renewed it
        if (token.equals(accessToken)) {
            accessToken = null;
        }
    }

    private Future<JsonObject> getCachedLoans(String cacheKey) {
        if (cacheService == null) {
            return Future.succeededFuture();
        }
        final Promise<JsonObject> promise = Promise.promise();
        cacheService.get(cacheKey, cached -> {
            JsonObject cachedLoans = null;
            if (cached.succeeded() && cached.result().isPresent()) {
                try {
                    cachedLoans = new JsonObject(cached.result().get());
                } catch (DecodeException e) {
                    log.warn("[e-sidoc] Ignored unreadable cached loans " + cacheKey);
                }
            }
            // A cache failure only costs a call to e-sidoc
            promise.complete(cachedLoans);
        });
        return promise.future();
    }

    private void cacheLoans(String cacheKey, JsonObject loans) {
        if (cacheService == null) {
            return;
        }
        cacheService.upsert(cacheKey, loans.encode(), loansCacheTtl, cached -> {
            if (cached.failed()) {
                log.warn("[e-sidoc] Failed to cache loans " + cacheKey, cached.cause());
            }
        });
    }

    private Future<JsonArray> findChildren(List<String> childrenIds) {
        final Promise<JsonArray> promise = Promise.promise();
        neo4j.execute(CHILDREN_QUERY, new JsonObject().put("childrenIds", new JsonArray(childrenIds)),
                Neo4jResult.validResultHandler(children -> {
                    if (children.isRight()) {
                        promise.complete(children.right().getValue());
                    } else {
                        promise.fail(children.left().getValue());
                    }
                }));
        return promise.future();
    }

    /**
     * Results of the futures that succeeded, so that a structure where e-sidoc fails does not hide the loans
     * found in the other ones. Fails only when every future failed.
     */
    private static <T> Future<List<T>> succeededResults(List<Future<T>> futures) {
        return Future.join(futures).transform(joined -> {
            final List<T> results = new ArrayList<>();
            Throwable cause = null;
            for (Future<T> future : futures) {
                if (future.succeeded()) {
                    results.add(future.result());
                } else {
                    cause = future.cause();
                }
            }
            if (results.isEmpty() && cause != null) {
                return Future.failedFuture(cause);
            }
            return Future.succeededFuture(results);
        });
    }

    private static List<String> toStringList(JsonArray values) {
        final List<String> strings = new ArrayList<>();
        if (values != null) {
            for (Object value : values) {
                if (value instanceof String) {
                    strings.add((String) value);
                }
            }
        }
        return strings;
    }

    private static CacheService createCacheService(Vertx vertx) {
        try {
            return CacheService.create(vertx);
        } catch (IllegalStateException e) {
            log.warn("[e-sidoc] Redis is not configured: the loans will not be cached");
            return null;
        }
    }
}
