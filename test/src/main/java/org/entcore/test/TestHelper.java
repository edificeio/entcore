package org.entcore.test;

import io.vertx.core.Vertx;

public class TestHelper {
    protected final Vertx vertx = Vertx.vertx();
    private final DatabaseTestHelper database = new DatabaseTestHelper(vertx);
    private final ShareTestHelper share = new ShareTestHelper(vertx);
    private final HttpTestHelper http = new HttpTestHelper(this);
    private final FileTestHelper file = new FileTestHelper();
    private final DirectoryTestHelper directory = new DirectoryTestHelper(vertx);
    public Vertx vertx(){
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

    public ShareTestHelper share() {
        return share;
    }

    public HttpTestHelper http() {
        return http;
    }

    public FileTestHelper file() {
        return file;
    }
}
