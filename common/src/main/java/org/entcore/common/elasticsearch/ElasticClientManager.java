package org.entcore.common.elasticsearch;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TODO merge with entcore (onerror circuit breaker...)
public class ElasticClientManager {
    static final Logger log = LoggerFactory.getLogger(ElasticClientManager.class);
    private final Random rnd = new Random();
    private final List<ElasticClient> clients = new ArrayList<>();

    public static ElasticClientManager create(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("elasticsearchConfig") != null) {
            final JsonObject elasticsearchConfig = config.getJsonObject("elasticsearchConfig");
            final ElasticClientManager esClient = new ElasticClientManager(vertx, elasticsearchConfig);
            return esClient;
        }else{
            final String elasticsearchConfig = (String) vertx.sharedData().getLocalMap("server").get("elasticsearchConfig");
            if(elasticsearchConfig!=null){
                final ElasticClientManager esClient = new ElasticClientManager(vertx, new JsonObject(elasticsearchConfig));
                return esClient;
            }else{
                throw new Exception("Missing elasticsearchConfig config");
            }
        }
    }

    private static URI[] toURI(final JsonObject config) throws Exception{
        final JsonArray esUri = config.getJsonArray("uris");
        final List<URI> uriList = new ArrayList<>();
        for (int i = 0; i < esUri.size(); i++) {
            final Object uri = esUri.getValue(i);
            if (uri instanceof String) {
                uriList.add(new URI(uri.toString()));
            } else {
                throw new Exception("Bad uri for elastic search: " + uri);
            }
        }
        final URI[] uris = uriList.toArray(new URI[uriList.size()]);
        return uris;
    }

    public ElasticClientManager(final Vertx vertx, final JsonObject config) throws Exception {
        this(vertx, toURI(config), config);
    }

    public ElasticClientManager(final Vertx vertx, final URI[] uris) {
        this(vertx, uris, new JsonObject());
    }

    public ElasticClientManager(final Vertx vertx, final URI[] uris, final JsonObject config) {
        try {
            final int poolSize = config.getInteger("poolSize", 16);
            final boolean keepAlive = config.getBoolean("keepAlive", true);
            for (final URI uri : uris) {
                HttpClientOptions httpClientOptions = new HttpClientOptions()
                        .setKeepAlive(keepAlive)
                        .setMaxPoolSize(poolSize)
                        .setDefaultHost(uri.getHost())
                        .setDefaultPort(uri.getPort())
                        .setConnectTimeout(20000);
                clients.add(new ElasticClient(vertx.createHttpClient(httpClientOptions)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public ElasticClient getClient() {
        return clients.get(rnd.nextInt(clients.size()));
    }
}
