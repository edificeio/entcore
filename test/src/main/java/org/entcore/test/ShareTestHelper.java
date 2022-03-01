package org.entcore.test;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.StartupUtils;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.share.impl.SqlShareService;

import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

public class ShareTestHelper {
    private final Vertx vertx;

    public ShareTestHelper(Vertx v) {
        this.vertx = v;
    }

    public JsonObject createShareForUser(String userId, List<String> actions) {
        return new JsonObject().put("users", new JsonObject().put(userId, new JsonArray(actions)));
    }

    public JsonObject createShareForGroup(String userId, List<String> actions) {
        return new JsonObject().put("groups", new JsonObject().put(userId, new JsonArray(actions)));
    }
    //

    public ShareService createSqlShareService(TestContext context, String schema, String table) throws Exception {
        try {
            // delegate IDE build to gradle in order to have annotation processor running
            // before tests
            final URL uri = getClass().getClassLoader().getResource("securedaction");
            final String parent = Paths.get(uri.toURI()).getParent().toAbsolutePath().toString();
            final String oldValue = FileResolver.absolutePath("");
            try {

                FileResolver.getInstance().setBasePath(parent);
                final JsonArray actions = StartupUtils.loadSecuredActions(vertx);
                final Map<String, SecuredAction> mapActions = StartupUtils.securedActionsToMap(actions);
                return new SqlShareService(schema, table, vertx.eventBus(), mapActions, null);
            } finally {
                FileResolver.getInstance().setBasePath(oldValue);
            }
        } catch (Exception e) {
            context.fail(e);
            throw e;
        }
    }

    public ShareService createMongoShareService(TestContext context, String collection) throws Exception {
        try {
            // delegate IDE build to gradle in order to have annotation processor running
            // before tests
            final URL uri = getClass().getClassLoader().getResource("securedaction");
            final String parent = Paths.get(uri.toURI()).getParent().toAbsolutePath().toString();
            final String oldValue = FileResolver.absolutePath("");
            try {

                FileResolver.getInstance().setBasePath(parent);
                final JsonArray actions = StartupUtils.loadSecuredActions(vertx);
                final Map<String, SecuredAction> mapActions = StartupUtils.securedActionsToMap(actions);
                return new MongoDbShareService(vertx.eventBus(), MongoDb.getInstance(), collection, mapActions, null);
            } finally {
                FileResolver.getInstance().setBasePath(oldValue);
            }
        } catch (Exception e) {
            context.fail(e);
            throw e;
        }
    }

    public Map<String,SecuredAction> getSecuredActions(TestContext context) throws Exception {
        try {
            // delegate IDE build to gradle in order to have annotation processor running
            // before tests
            final URL uri = getClass().getClassLoader().getResource("securedaction");
            final String parent = Paths.get(uri.toURI()).getParent().toAbsolutePath().toString();
            final String oldValue = FileResolver.absolutePath("");
            try {

                FileResolver.getInstance().setBasePath(parent);
                final JsonArray actions = StartupUtils.loadSecuredActions(vertx);
                final Map<String, SecuredAction> mapActions = StartupUtils.securedActionsToMap(actions);
                return mapActions;
            } finally {
                FileResolver.getInstance().setBasePath(oldValue);
            }
        } catch (Exception e) {
            context.fail(e);
            throw e;
        }
    }
}
