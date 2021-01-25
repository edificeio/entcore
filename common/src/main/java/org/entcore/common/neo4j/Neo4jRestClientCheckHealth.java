/*
 * Copyright © "Open Digital Education", 2017
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.common.neo4j;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

public class Neo4jRestClientCheckHealth implements Neo4jRestClientCheck {
    private final Vertx vertx;
    private final String authorizationHeader;
    private static final Logger logger = LoggerFactory.getLogger(Neo4jRestClientCheckHealth.class);

    public Neo4jRestClientCheckHealth(final Vertx vertx, final String authorizationHeader) {
        this.vertx = vertx;
        this.authorizationHeader = authorizationHeader;
    }

    @Override
    public Future<Void> check(Neo4jRestClientNodeManager manager) {
        final List<Future> futures = new ArrayList<>();
        for (final Neo4jRestClientNode node : manager.getClients()) {
            final Future<Void> futureNode = Future.future();
            futures.add(futureNode);
            if (node.isBanned()) {
                completeQuiet(futureNode);
            } else {
                //check only if not banned
                final HttpClient client = node.getHttpClient();
                final HttpClientRequest req = client.get("/db/manage/server/ha/available", resp -> {
                    try {
                        //if server respond 200 => available and check type
                        if (resp.statusCode() == 200) {
                            resp.bodyHandler(body -> {
                                if ("master".equals(body.toString())) {
                                    node.setMaster();
                                } else {
                                    node.setSlave();
                                }
                                completeQuiet(futureNode);
                            });
                        } else {
                            node.setAvailable(false);
                            completeQuiet(futureNode);
                        }
                    } catch (Exception e) {
                        logger.error("Neo4j Health check failed with message: " + e.getMessage());
                        node.setAvailable(false);
                        completeQuiet(futureNode);
                    }
                });
                req.exceptionHandler(e -> {
                    //log error
                    if (e instanceof VertxException && "Connection was closed".equals(e.getMessage())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Neo4j Health check failed", e);
                        }
                    }else if(e instanceof ConnectException){
                        //cannot connect to remote host
                        node.setAvailable(false);
                    } else {
                        logger.error("Neo4j Health check failed", e);
                    }
                    //complete
                    completeQuiet(futureNode);
                });
                prepareRequest(req).end();
            }
        }
        return CompositeFuture.all(futures).mapEmpty();
    }

    private HttpClientRequest prepareRequest(final HttpClientRequest request) {
        if (!StringUtils.isEmpty(this.authorizationHeader)) {
            request.headers().add("Authorization", this.authorizationHeader);
        }
        return request;
    }

    private void completeQuiet(final Future<Void> futureNode) {
        if (!futureNode.isComplete()) {
            futureNode.complete();
        }
    }
}
