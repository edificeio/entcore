package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.explorer.impl.ExplorerRepositoryEvents;
import org.entcore.common.explorer.to.ExplorerReindexResourcesRequest;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.UserInfos;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(VertxUnitRunner.class)
public class ExplorerRepositoryEventsTest {


    @Test
    public void testImportNoResources(TestContext context) {
        final Async async = context.async();
        final Map<String, IExplorerPluginClient> pluginMap = new HashMap<>();
        final RecordingReindexPluginClient plugin = new RecordingReindexPluginClient();
        pluginMap.put("toto", plugin);
        final ExplorerRepositoryEvents explorerRepositoryEvents = new ExplorerRepositoryEvents(
                new RepositoryEventsWithSuppliedImportReport(new JsonObject()),
                pluginMap,plugin);
        explorerRepositoryEvents.importResources("importId", "userId", "userLogin",
                "userName", "importPath", "locale", "host", false, e -> {
            context.assertTrue(plugin.requests.isEmpty(), "Reindex should not have been called");
            async.complete();
        });
    }

    @Test
    public void testImportNullResources(TestContext context) {
        final Map<String, IExplorerPluginClient> pluginMap = new HashMap<>();
        final RecordingReindexPluginClient plugin = new RecordingReindexPluginClient();
        pluginMap.put("toto", plugin);
        final ExplorerRepositoryEvents explorerRepositoryEvents = new ExplorerRepositoryEvents(
                new RepositoryEventsWithSuppliedImportReport(null),
                pluginMap,plugin);
        explorerRepositoryEvents.importResources("importId", "userId", "userLogin", "userName",
                "importPath", "locale", "host", false, e -> {
                context.assertTrue(plugin.requests.isEmpty(), "Reindex should not have been called");
        });
    }

    @Test
    public void testImportResourcesNotConcernedByThePlugin(TestContext context) {
        final Map<String, IExplorerPluginClient> pluginMap = new HashMap<>();
        final RecordingReindexPluginClient plugin = new RecordingReindexPluginClient();
        pluginMap.put("toto", plugin);
        final ExplorerRepositoryEvents explorerRepositoryEvents = new ExplorerRepositoryEvents(
                new RepositoryEventsWithSuppliedImportReport(populateImportedResourcesForApp("tata")),
                pluginMap,plugin);
        explorerRepositoryEvents.importResources("importId", "userId", "userLogin", "userName",
        "importPath", "locale", "host", false, e -> {
            context.assertTrue(plugin.requests.isEmpty(), "Reindex should not have been called because no data were of a type handled by the plugin");
        });
    }

    @Test
    public void testImportResourcesConcernedByThePlugin(TestContext context) {
        final Map<String, IExplorerPluginClient> pluginMap = new HashMap<>();
        final RecordingReindexPluginClient plugin = new RecordingReindexPluginClient();
        pluginMap.put("toto", plugin);
        final ExplorerRepositoryEvents explorerRepositoryEvents = new ExplorerRepositoryEvents(
            new RepositoryEventsWithSuppliedImportReport(populateImportedResourcesForApp("toto")),
            pluginMap,plugin);
        explorerRepositoryEvents.importResources("importId", "userId", "userLogin", "userName",
                "importPath", "locale", "host", false, e -> {
            context.assertEquals(1, plugin.requests.size(), "Reindex should have been called once for imported data");
            final ExplorerReindexResourcesRequest reindexRequest = plugin.requests.get(0);
            final Set<String> ids = reindexRequest.getIds();
            context.assertEquals(3, ids.size(), "all resources should have been reindexed");
            context.assertTrue(ids.contains("toto-id1"), "id 1 should have been reindexed");
            context.assertTrue(ids.contains("toto-id2"), "id 2 should have been reindexed");
            context.assertTrue(ids.contains("toto-id4"), "id 4 should have been reindexed");
        });
    }

    private JsonObject populateImportedResourcesForApp(final String appName) {
        return populateImportedResourcesForApp(appName, new JsonObject());
    }
    private JsonObject populateImportedResourcesForApp(final String appName, final JsonObject report) {
        final JsonObject resourcesIdsMap = report.getJsonObject("resourcesIdsMap", new JsonObject());
        final JsonObject idsMapForApp = new JsonObject()
                .put("id1", appName + "-id1")
                .put("id2", appName + "-id2")
                .put("id3", appName + "-id4"); // Case of an id collision
        resourcesIdsMap.put(appName, idsMapForApp);
        report.put("resourcesIdsMap", resourcesIdsMap);
        return report;
    }


    public static class RecordingReindexPluginClient extends DummyPluginClient {
        private final List<ExplorerReindexResourcesRequest> requests = new ArrayList<>();

        @Override
        public Future<IndexResponse> tryReindex(UserInfos user, ExplorerReindexResourcesRequest request, int times, int delay) {
            requests.add(request);
            return super.tryReindex(user, request, times, delay);
        }

        @Override
        public Future<IndexResponse> reindex(final UserInfos user, final ExplorerReindexResourcesRequest request) {
            requests.add(request);
            return super.reindex(user, request);
        }
    }


    public static class DummyPluginClient implements IExplorerPluginClient {
        @Override
        public Future<IndexResponse> tryReindex(UserInfos user, ExplorerReindexResourcesRequest request, int times, int delay) {
            return Future.succeededFuture(new IndexResponse(0, 0));
        }

        @Override
        public Future<IndexResponse> reindex(final UserInfos user, final ExplorerReindexResourcesRequest request) {
            return Future.succeededFuture(new IndexResponse(0, 0));
        }

        @Override
        public Future<IndexResponse> reindex(ExplorerReindexResourcesRequest request) {
            return Future.succeededFuture(new IndexResponse(0, 0));
        }

        @Override
        public Future<List<String>> createAll(final UserInfos user, final List<JsonObject> json, final boolean isCopy) {
            return Future.succeededFuture(Collections.emptyList());
        }

        @Override
        public Future<DeleteResponse> deleteById(final UserInfos user, final Set<String> ids) {
            return Future.succeededFuture(new DeleteResponse());
        }

        @Override
        public Future<ShareResponse> shareByIds(final UserInfos user, final Set<String> ids, final JsonObject shares) {
            return Future.succeededFuture(new ShareResponse(0, new JsonObject()));
        }

        @Override
        public Future<JsonObject> getMetrics(final UserInfos user) {
            return Future.succeededFuture(new JsonObject());
        }
    }

    public static class RepositoryEventsWithSuppliedImportReport implements RepositoryEvents {
        private final JsonObject report;

        public RepositoryEventsWithSuppliedImportReport(final JsonObject report) {
            this.report = report;
        }
        @Override
        public void exportResources(final boolean exportDocuments, final boolean exportSharedResources, final String exportId, final String userId, final JsonArray groups, final String exportPath, final String locale, final String host, final Handler<Boolean> handler) {
            handler.handle(true);
        }

        @Override
        public void exportResources(final JsonArray resourcesIds, final boolean exportDocuments, final boolean exportSharedResources, final String exportId, final String userId, final JsonArray groups, final String exportPath, final String locale, final String host, final Handler<Boolean> handler) {
            handler.handle(true);
        }

        @Override
        public void importResources(final String importId, final String userId, final String userLogin, final String userName, final String importPath, final String locale, final String host, final boolean forceImportAsDuplication, final Handler<JsonObject> handler) {
            handler.handle(report);
        }

        @Override
        public void deleteGroups(final JsonArray groups) {
        }

        @Override
        public void deleteGroups(JsonArray groups, Handler<List<ResourceChanges>> handler) {
            handler.handle(Collections.emptyList());
        }

        @Override
        public void deleteUsers(final JsonArray users) {
        }

        @Override
        public void deleteUsers(JsonArray users, Handler<List<ResourceChanges>> handler) {
            handler.handle(Collections.emptyList());
        }

        @Override
        public void usersClassesUpdated(final JsonArray updates) {
        }

        @Override
        public void transition(final JsonObject structure) {
        }

        @Override
        public void mergeUsers(final String keepedUserId, final String deletedUserId) {
        }

        @Override
        public void removeShareGroups(final JsonArray oldGroups) {
        }

        @Override
        public void tenantsStructuresUpdated(final JsonArray addedTenantsStructures, final JsonArray deletedTenantsStructures) {
        }

        @Override
        public void timetableImported(final String uai) {
        }
    };
}
