package org.entcore.test;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

public class TestHelper {
    protected final Vertx vertx = Vertx.vertx();
    private final DatabaseTestHelper database = new DatabaseTestHelper(vertx);
    private final ShareTestHelper share = new ShareTestHelper(vertx);
    private final HttpTestHelper http = new HttpTestHelper(this);
    private final PortalTestHelper portal = new PortalTestHelper(this);
    private final FileTestHelper file = new FileTestHelper();
    private final DirectoryTestHelper directory = new DirectoryTestHelper(this, vertx);
    private final AppRegistryTestHelper registry = new AppRegistryTestHelper(this);

    public Vertx vertx() {
        return this.vertx;
    }

    public static TestHelper helper() {
        return new TestHelper();
    }

    public DatabaseTestHelper database() {
        return database;
    }

    public DirectoryTestHelper directory() {
        return directory;
    }

    public HttpTestHelper http() {
        return http;
    }

    public PortalTestHelper portal() {
        return portal;
    }

    public AppRegistryTestHelper registry() {
        return registry;
    }

    public ShareTestHelper share() {
        return share;
    }

    public FileTestHelper file() {
        return file;
    }

    public TestHelper initSharedData() {
        final LocalMap<Object, Object> map = vertx.sharedData().getLocalMap("cluster");
        map.put("node", false);
        map.put("node", "");
        return this;
    }
}
