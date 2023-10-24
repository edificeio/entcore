package org.entcore.common.neo4j;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

public class Neo4jRestClientCheckRead implements Neo4jRestClientCheck {
    private final String authorizationHeader;
    private final Vertx vertx;
    private final String readCheckQuery;
    private static final Logger logger = LoggerFactory.getLogger(Neo4jRestClientCheckRead.class);

    public Neo4jRestClientCheckRead(Vertx vertx, final String authorizationHeader, JsonObject neo4jConfig) {
        this.vertx = vertx;
        this.authorizationHeader = authorizationHeader;
        this.readCheckQuery = neo4jConfig.getString("readcheck-query", "MATCH () RETURN 1 LIMIT 1");
    }

    @Override
    public Future<Void> check(Neo4jRestClientNodeManager manager) {
        final List<Future<?>> futures = new ArrayList<>();
        for (final Neo4jRestClientNode node : manager.getClients()) {
            final Promise<Void> futureNode = Promise.promise();
            futures.add(futureNode.future());
            if (node.isBanned()) {
                futureNode.tryComplete();
            } else {
                //check only if not banned
                final HttpClient client = node.getHttpClient();
                client.request(HttpMethod.POST, "/db/data/cypher")
                .map(req -> req.putHeader("Content-Type", "application/json")
                               .putHeader("Accept", "application/json; charset=UTF-8"))
                .map(this::prepareRequest)
                .flatMap(req -> req.send(new JsonObject().put("query", readCheckQuery).encode()))
                .onSuccess(resp -> {
                    resp.bodyHandler(body -> {
                        try {
                            if (resp.statusCode() == 200) {
                                final JsonObject json = new JsonObject(body.toString());
                                final JsonArray columns = json.getJsonArray("columns", new JsonArray());
                                if (columns.contains("1")) {
                                    node.setReadable(true);
                                } else {
                                    logger.error("Neo4j Read check failed with value: " + json);
                                    node.setReadable(false);
                                }
                                futureNode.tryComplete();
                            } else {
                                logger.error("Neo4j Read check failed with status: " + resp.statusCode());
                                node.setReadable(false);
                                futureNode.tryComplete();
                            }
                        } catch (Exception e) {
                            logger.error("Neo4j Read check failed with message: " + e.getMessage());
                            node.setReadable(false);
                            futureNode.tryComplete();
                        }
                    });
                })
                .onFailure(th -> {
                    if (th instanceof VertxException && "Connection was closed".equals(th.getMessage())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Neo4j Read check failed", th);
                        }
                    }else if(th instanceof ConnectException){
                        //cannot connect to remote host
                        node.setReadable(false);
                    }  else {
                        logger.error("Neo4j Read check failed", th);
                    }
                    //complete
                    futureNode.tryComplete();
                });
            }
        }
        return Future.all(futures).mapEmpty();
    }

    private HttpClientRequest prepareRequest(final HttpClientRequest request) {
        if (!StringUtils.isEmpty(this.authorizationHeader)) {
            request.headers().add("Authorization", this.authorizationHeader);
        }
        return request;
    }

}
