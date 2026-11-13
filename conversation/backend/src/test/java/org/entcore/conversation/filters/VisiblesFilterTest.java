package org.entcore.conversation.filters;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Covers the answer of the filter when the visibility check fails, typically on the 60 s timeout of the
 * communication bus for a sender and a recipient who both belong to thousands of groups (IMPULS-6465).
 */
@RunWith(VertxUnitRunner.class)
public class VisiblesFilterTest {

    private static final TestHelper test = TestHelper.helper();

    @Rule
    public Timeout timeout = Timeout.seconds(10);

    @BeforeClass
    public static void setUp() {
        Neo4j.getInstance().init(test.vertx(), new JsonObject());
        test.vertx().eventBus().consumer("wse.communication.users", message -> message.fail(500, "timeout"));
    }

    /**
     * Without an answer, the send request hangs until the proxy drops it and the message stays a draft.
     */
    @Test
    public void shouldRefuseWhenVisiblesCannotBeFetched(TestContext context) {
        final Async async = context.async();
        final UserInfos user = new UserInfos();
        user.setUserId("sender");
        final JsonObject body = new JsonObject().put("to", new JsonArray().add("recipient"));
        new VisiblesFilter().authorize(test.http().post("/conversation/send", new JsonObject(), body), null, user,
                authorized -> {
                    context.assertFalse(authorized);
                    async.complete();
                });
    }
}
