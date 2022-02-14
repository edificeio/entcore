package org.entcore.test;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

/**
 * TestHelper is the main utility class for writing unit tests.
 * It allows building a functional environment for your test classes :
 * <ul>
 * <li>mocks server configuration,</li>
 * <li>adds DB support,</li>
 * <li>adds core services : feeder, directory, app registry...</li>
 * </ul>
 * It also provides some HTTP utilities.
 * <hr/>
 * Example use :
 * <pre>{@code
package org.example.entcore;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.test.TestHelper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;

{@literal @}RunWith(VertxUnitRunner.class)
public class ExampleTest {
    protected static final TestHelper test = TestHelper.helper();
    {@literal @}ClassRule public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    {@literal @}ClassRule public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();
    {@literal @}BeforeClass
    public static void setUp(final TestContext context) throws Exception {
        final Async async = context.async();
        test.database().initNeo4j(context, neo4jContainer);
        test.database().initPostgreSQL(context, pgContainer, "myschema", true, 30000L).await();
        // ...then asynchronously load resources, deploy verticles...
        async.complete(); // When setup is done !
    }

    {@literal @}Test
    public void myUnitTest(final TestContext context) throws Exception {
        final Async async = context.async();
        // ...do whatever needs testing with this.test; When done, check whatever needs checking :
        context.assertTrue(somethingTruthy);
        context.assertFalse(somethingFalsy);
        async.complete(); // When test is done !
    }
}</pre>
 */
public class TestHelper {
    protected final Vertx vertx = Vertx.vertx();
    private final DatabaseTestHelper database = new DatabaseTestHelper(vertx);
    private final ShareTestHelper share = new ShareTestHelper(vertx);
    private final HttpTestHelper http = new HttpTestHelper(this);
    private final PortalTestHelper portal = new PortalTestHelper(this);
    private final FileTestHelper file = new FileTestHelper();
    private final DirectoryTestHelper directory = new DirectoryTestHelper(this, vertx);
    private final AppRegistryTestHelper registry = new AppRegistryTestHelper(this);
    private final UserbookTestHelper userbook = new UserbookTestHelper(this, vertx);
    private final ConfigTestHelper config = new ConfigTestHelper(vertx);

    public Vertx vertx() {
        return this.vertx;
    }

    /**
     * TestHelper factory
     * @return a new TestHelper instance
     */
    public static TestHelper helper() {
        return new TestHelper();
    }

    /** @return The configuration helper. */
    public ConfigTestHelper config() {
        return config;
    }

    /** @return The database helper. */
    public DatabaseTestHelper database() {
        return database;
    }

    /** @return The userbook helper. */
    public UserbookTestHelper userbook() {
        return userbook;
    }

    /** @return The directory helper. */
    public DirectoryTestHelper directory() {
        return directory;
    }

    /** @return The HTTP client helper. */
    public HttpTestHelper http() {
        return http;
    }

    /** @return The portal helper. */
    public PortalTestHelper portal() {
        return portal;
    }

    /** @return The application registry helper. */
    public AppRegistryTestHelper registry() {
        return registry;
    }

    /** @return The share helper. */
    public ShareTestHelper share() {
        return share;
    }

    /** @return The file helper. */
    public FileTestHelper file() {
        return file;
    }

    /*TODO Fluent method to document.  */
    public AssertTestHelper asserts() { return new AssertTestHelper(this, vertx); }

    public TestHelper initSharedData() {
        final LocalMap<Object, Object> map = vertx.sharedData().getLocalMap("cluster");
        map.put("node", false);
        map.put("node", "");
        return this;
    }
}
