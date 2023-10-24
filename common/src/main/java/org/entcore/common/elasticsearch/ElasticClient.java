package org.entcore.common.elasticsearch;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class ElasticClient {
    private final HttpClient httpClient;
    private final Optional<String> authorization;
    private Logger log = LoggerFactory.getLogger(ElasticClient.class);
    private Handler<Throwable> onErrorHandler = (e) -> {
        log.error("Elastic Query failed: ",e);
    };

    public ElasticClient(final HttpClient aClient) {
        this(aClient, Optional.empty());
    }

    public ElasticClient(final HttpClient aClient,final Optional<String> authorization) {
        this.httpClient = aClient;
        this.authorization = authorization;
    }

    public void setOnError(final Handler<Throwable> onError) {
        this.onErrorHandler = onError;
    }

    private <T> Handler<Throwable> getOnErrorHandler(final Promise<T> promise){
        return (e)->{
            promise.fail(e);
            this.onErrorHandler.handle(e);
        };
    }

    protected HttpClientRequest authorize(final HttpClientRequest req){
        if(authorization.isPresent()){
            req.putHeader("Authorization",authorization.get());
        }
        return req;
    }

    public Future<Void> createMapping(final String index, final Buffer payload) {
        final Promise<Void> promise = Promise.promise();
        httpClient.request(HttpMethod.PUT, "/" + index)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload))
            .onSuccess(res -> {
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    promise.complete();
                } else {
                    res.bodyHandler(resBody -> {
                        if(resBody.toString().contains("resource_already_exists_exception")){
                            promise.complete();
                        }else{
                            promise.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                        }
                    });
                }
            })
            .onFailure(getOnErrorHandler(promise));
        return promise.future();
    }

    public Future<Void> deleteMapping(final String index) {
        final Promise<Void> future = Promise.promise();
        httpClient.request(HttpMethod.DELETE, "/" + index)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(HttpClientRequest::send)
            .onSuccess(res -> {
                    if (res.statusCode() == 200 || res.statusCode() == 201) {
                        future.complete();
                    } else {
                        res.bodyHandler(resBody -> {
                            future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                        });
                    }
                })
            .onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<String> createDocument(final String index, final JsonObject payload, final ElasticOptions options) {
        final Promise<String> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_doc" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
                res.bodyHandler(resBody -> {
                    if (res.statusCode() == 200 || res.statusCode() == 201) {
                        final JsonObject body = new JsonObject(resBody.toString());
                        future.complete(body.getString("_id"));
                    } else {
                        future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                    }
                });
            })
            .onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<String> updateDocument(final String index, final String id, final JsonObject payload, final ElasticOptions options) {
        final Promise<String> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_update/" + id + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(new JsonObject().put("doc",payload).toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    future.complete(body.getString("_id"));
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<Void> deleteDocument(final String index, final String id, final ElasticOptions options) {
        final Promise<Void> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.DELETE, "/" + index + "/_doc/" + id + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(new JsonObject().toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200) {
                    future.complete();
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        })
        .onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<JsonObject> getDocument(final String index, final String id, final ElasticOptions options) {
        final Promise<JsonObject> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.GET, "/" + index + "/" + id + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(HttpClientRequest::send)
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    future.complete(body.getJsonObject("_source").put("_id", body.getString("_id")));
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<String> scriptedUpsert(final String index, final String id, final JsonObject payload, final ElasticOptions options) {
        final Promise<String> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_update/" + id + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200 || res.statusCode() == 201) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    future.complete(body.getString("_id"));
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<Void> updateDocument(final String index, final Set<String> id, final JsonObject payload, final ElasticOptions options) {
        if (id.isEmpty()) {
            return Future.succeededFuture();
        }
        if (id.size() == 1) {
            return updateDocument(index, id.iterator().next(), payload, options).mapEmpty();
        }
        final Promise<Void> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        payload.put("query", new JsonObject("terms").put("_id", new JsonArray(new ArrayList(id))));

        httpClient.request(HttpMethod.POST, "/" + index + "/_update_by_query" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
            if (res.statusCode() == 200 || res.statusCode() == 201) {
                future.complete();
            } else {
                res.bodyHandler(resBody -> {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                });
            }
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<JsonArray> search(final String index, final JsonObject payload, final ElasticOptions options) {
        final Promise<JsonArray> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_search" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    final JsonArray hits = body.getJsonObject("hits").getJsonArray("hits");
                    final JsonArray mapped = new JsonArray(hits.stream().map(o -> {
                        final JsonObject json = (JsonObject) o;
                        return json.getJsonObject("_source").put("_id", json.getString("_id"));
                    }).collect(Collectors.toList()));
                    future.complete(mapped);
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<SearchResult> searchWithMeta(final String index, final JsonObject payload, final ElasticOptions options) {
        final Promise<SearchResult> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_search" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    final JsonArray hits = body.getJsonObject("hits").getJsonArray("hits");
                    final Long total = body.getJsonObject("hits").getJsonObject("total").getLong("value");
                    final String relation = body.getJsonObject("hits").getJsonObject("total").getString("relation");
                    final List<JsonObject> mapped = hits.stream().map(o -> {
                        final JsonObject json = (JsonObject) o;
                        return json.getJsonObject("_source").put("_id", json.getString("_id"));
                    }).collect(Collectors.toList());
                    future.complete(new SearchResult(total, relation, mapped));
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<Integer> count(final String index, final JsonObject payload, final ElasticOptions options) {
        final Promise<Integer> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        httpClient.request(HttpMethod.POST, "/" + index + "/_count" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(payload.toString()))
            .onSuccess(res -> {
            res.bodyHandler(resBody -> {
                if (res.statusCode() == 200) {
                    final JsonObject body = new JsonObject(resBody.toString());
                    future.complete(body.getInteger("count"));
                } else {
                    future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                }
            });
        }).onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<Void> storeScript(final String scriptId, final Buffer script) {
        final Promise<Void> future = Promise.promise();
        httpClient.request(HttpMethod.PUT, "/_scripts/" + scriptId)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/json"))
            .flatMap(r -> r.send(script.toString()))
            .onSuccess(res -> {
                res.bodyHandler(resBody -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        future.complete();
                    } else {
                        future.fail(res.statusCode() + ":" + res.statusMessage() + ". " + resBody);
                    }
                });
            })
            .onFailure(getOnErrorHandler(future));
        return future.future();
    }

    public Future<ElasticBulkBuilder> bulk(final String index, final ElasticOptions options) {
        final Promise<Buffer> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        return httpClient.request(HttpMethod.POST, "/" + index + "/_bulk" + queryParams)
            .map(this::authorize)
            .map(r -> r.putHeader("content-type", "application/x-ndjson")
                .putHeader("Accept", "application/json; charset=UTF-8")
                .setChunked(true))
            .onFailure(getOnErrorHandler(future))
            .map(ElasticBulkBuilder::new);
    }

    public Future<ElasticBulkBuilder> bulk(final ElasticOptions options) {
        final Promise<Buffer> future = Promise.promise();
        final String queryParams = options.getQueryParams();
        return httpClient.request(HttpMethod.POST, "/_bulk" + queryParams)
            .map(this::authorize)
            .map(req -> req.putHeader("Content-Type", "application/x-ndjson")
                .putHeader("Accept", "application/json; charset=UTF-8")
                .setChunked(true)
                .exceptionHandler(getOnErrorHandler(future)))
            .map(ElasticBulkBuilder::new);
    }

    public static class SearchResult{
        private final Long count;
        private final String countRelation;
        private final List<JsonObject> rows;

        public SearchResult(final Long count, final String countRelation, final List<JsonObject> rows) {
            this.countRelation = countRelation;
            this.count = count;
            this.rows = rows;
        }

        public String getCountRelation() {return countRelation;}
        public List<JsonObject> getRows() {return rows;}
        public Long getCount() {
            if("eq".equals(countRelation)){
                return count;
            }else{
                //gte
                return count+1;
            }
        }
    }

    public static class ElasticOptions {
        private final Set<String> routing = new HashSet<>();
        private boolean waitFor = false;
        private boolean refresh = false;

        public ElasticOptions withRefresh(boolean refresh) {
            this.refresh = refresh;
            return this;
        }

        public ElasticOptions withWaitFor(boolean waitFor) {
            this.waitFor = waitFor;
            return this;
        }

        public ElasticOptions withRouting(String routing) {
            this.routing.add(routing);
            return this;
        }

        public ElasticOptions withRouting(Collection<String> routing) {
            this.routing.addAll(routing);
            return this;
        }

        public String getQueryParams() {
            final List<String> queryParams = new ArrayList<>();
            if (waitFor) {
                queryParams.add("refresh=wait_for");
            }
            if (refresh) {
                queryParams.add("refresh=true");
            }
            if (!routing.isEmpty()) {
                queryParams.add("routing=" + String.join(",", routing));
            }
            if (queryParams.isEmpty()) {
                return "";
            } else {
                return "?" + String.join("&", queryParams);
            }
        }
    }

}
