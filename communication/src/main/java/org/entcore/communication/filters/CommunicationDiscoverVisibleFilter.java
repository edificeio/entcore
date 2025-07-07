package org.entcore.communication.filters;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.communication.CheckCommunicationExistsResponseDTO;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.migration.AppMigrationConfiguration;
import org.entcore.common.migration.BrokerSwitchConfiguration;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;

public class CommunicationDiscoverVisibleFilter implements ResourcesProvider {

    private AppMigrationConfiguration appMigrationConfiguration;
    JsonArray discoverVisibleExpectedProfile = new JsonArray();

    public CommunicationDiscoverVisibleFilter() {
        this.discoverVisibleExpectedProfile = Vertx.currentContext().config().getJsonArray("discoverVisibleExpectedProfile", new JsonArray());
    }

    private final Neo4j neo4j = Neo4j.getInstance();


    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {

        if(user == null || discoverVisibleExpectedProfile.isEmpty() || !discoverVisibleExpectedProfile.contains(user.getType())) {
            handler.handle(false);
            return;
        }

        String groupId = resourceRequest.params().get("groupId");
        if(groupId != null && !groupId.trim().isEmpty()) {
            String query = "MATCH (g:CommunityGroup:Group:Visible {id: {groupId}})<-[:IN|COMMUNIQUE]-(u:User {id: {userId}}) " +
                            "RETURN COUNT(u) > 0 as exists";

            JsonObject params = new JsonObject().put("userId", user.getUserId()).put("groupId", groupId);

            check(query, params, handler);
        } else {
            handler.handle(true);
        }
    }

    private void check(String query, JsonObject params, Handler<Boolean> handler) {
        neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray r = event.body().getJsonArray("result");
                handler.handle(
                        "ok".equals(event.body().getString("status")) &&
                                r != null && r.size() == 1 &&
                                (r.getJsonObject(0)).getBoolean("exists", false)
                );
            }
        });
    }


    private Future<Boolean> switchAuthorization(final JsonObject params) {
        final Promise<Boolean> promise = Promise.promise();
        final JsonObject payload = new JsonObject()
          .put("action", "visibleFilter")
          .put("service", "referential")
          .put("params", params);

        Vertx.currentContext().owner().eventBus().request(BrokerSwitchConfiguration.LEGACY_MIGRATION_ADDRESS, payload)
          .onSuccess(response -> promise.complete(StringUtils.parseJson((String) response.body(), CheckCommunicationExistsResponseDTO.class).isExists()))
          .onFailure(promise::fail);
        return promise.future();
    }

    private boolean isReadAvailable() {
        final AppMigrationConfiguration appMigration = getAppMigration();
        return appMigration.isEnabled() && appMigration.getAvailableReadActions().contains("communicationDiscoverVisibleFilter");
    }


    private AppMigrationConfiguration getAppMigration() {
        if (appMigrationConfiguration == null) {
            appMigrationConfiguration = AppMigrationConfiguration.fromVertx("referential");
        }
        return appMigrationConfiguration;
    }
}
