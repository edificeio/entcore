package org.entcore.common.http.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.JacksonCodec;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.entcore.common.mongodb.MongoDbResult.validAsyncResultsHandler;

public class BlockRouteFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(BlockRouteFilter.class);

    private static final String BLOCK_ROUTE_COLLECTION = "blockroutes";

    private final MongoDb mongoDb = MongoDb.getInstance();
    private final EventBus eb;
    private final String prefix;
    private List<BlockingCondition> blockingConditions;

    private static class BlockingCondition {

        private final List<String> profiles;
        private final List<String> excludeStructures;
        private final List<Pattern> routePatterns;

        @JsonCreator
        public BlockingCondition(
            @JsonProperty("profiles") List<String> profiles,
            @JsonProperty("exclude-structures") List<String> excludeStructures,
            @JsonProperty("route-patterns") List<String> routePatterns) {
            this.profiles = profiles;
            this.excludeStructures = excludeStructures;
            this.routePatterns = routePatterns.stream().map(Pattern::compile).collect(Collectors.toList());
        }

        public boolean isBlocked(HttpServerRequest request, UserInfos user) {
            if (user.getType() != null && profiles != null && !(profiles.contains(user.getType()))) {
                return false;
            }
            if (user.getStructures() != null && excludeStructures != null) {
                for (String structureId: user.getStructures()) {
                    if (excludeStructures.contains(structureId)) {
                        return false;
                    }
                }
            }
            if (routePatterns != null) {
                for (Pattern routePattern: routePatterns) {
                    if (routePattern.matcher(request.path()).matches()) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    public BlockRouteFilter(Vertx vertx, EventBus eb, String prefix, long refreshConfPeriod) {
        this.prefix = prefix;
        this.eb = eb;
        this.blockingConditions = new ArrayList<>();
        vertx.setPeriodic(refreshConfPeriod, timerId -> refreshConf());
    }

    private void refreshConf() {
        log.info("Refresh conf BlockRouteFilter for prefix : " + prefix);
        final JsonObject query = new JsonObject().put("prefix", prefix);
        final JsonObject keys = new JsonObject().put("_id", 0)
            .put("profiles", 1)
            .put("exclude-structures", 1)
            .put("route-patterns", 1);
        mongoDb.find(BLOCK_ROUTE_COLLECTION, query, new JsonObject(), keys, validAsyncResultsHandler(ar -> {
            if (ar.succeeded()) {
                BlockRouteFilter.this.blockingConditions =
                        JacksonCodec.decodeValue(ar.result().encode(), new TypeReference<List<BlockingCondition>>(){});
            } else {
                log.error("Error loading block route conf", ar.cause());
            }
        }));
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                for (BlockingCondition blockingCondition: blockingConditions) {
                    if (blockingCondition.isBlocked(request, user)) {
                        handler.handle(false);
                        return;
                    }
                }
                handler.handle(true);
            } else {
                handler.handle(false);
            }
        });
    }

    @Override
    public void deny(HttpServerRequest request) {
        request.response().setStatusCode(401).setStatusMessage("Unauthorized")
                .putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
    }

}
