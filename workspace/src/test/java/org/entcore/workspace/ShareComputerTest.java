package org.entcore.workspace;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.folders.impl.InheritShareComputer;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ShareComputerTest {
    private JsonObject userRead(final String userId) {
        return new JsonObject().put("userId", userId).put("read", true);
    }

    private JsonObject userContrib(final String userId) {
        return new JsonObject().put("userId", userId).put("read", true).put("contrib", true);
    }

    private JsonObject userManage(final String userId) {
        return new JsonObject().put("userId", userId).put("read", true).put("contrib", true).put("manage", true);
    }

    private JsonObject groupRead(final String groupId) {
        return new JsonObject().put("groupId", groupId).put("read", true);
    }

    private JsonObject groupContrib(final String groupId) {
        return new JsonObject().put("groupId", groupId).put("read", true).put("contrib", true);
    }

    private JsonObject groupManage(final String groupId) {
        return new JsonObject().put("groupId", groupId).put("read", true).put("contrib", true).put("manage", true);
    }

    /**
     * <u>GOAL</u> : Ensure that concatShare merge shares without duplicate
     * <ol>
     *     <li>Create share with 2 different users</li>
     *     <li>Verify that concat generate 2 entries</li>
     *     <li>Create share with 2 different entries (read, manage) but one user</li>
     *     <li>Verify that concat generate 1 entry with manage rights</li>
     *     <li>Create share with 2 same entries (read) and one user</li>
     *     <li>Verify that concat generate 1 entry with 1 read rights</li>
     *     <li>Create share with 2 different groups</li>
     *     <li>Verify that concat generate 2 entries</li>
     *     <li>Create share with 2 different entries (read,manage) but one group</li>
     *     <li>Verify that concat generate 1 entry with manage rights</li>
     *     <li>Create share with 2 same entries (read) and one group</li>
     *     <li>Verify that concat generate 1 entry with 1 read rights</li>
     *     <li>Create share with 4 same entries (user1 read + group1 read)</li>
     *     <li>Verify that concat generate 2 entry with 1 read rights for user1 and group1</li>
     * </ol>
     */
    @Test
    public void shouldConcatShare(TestContext context) {
        // 2 users and 2 entries
        JsonArray result = InheritShareComputer.concatShares(new JsonArray().add(userRead("user1")).add(userContrib("user2")));
        context.assertEquals(2, result.size());
        context.assertEquals(2, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("user1", result.getJsonObject(0).getString("userId"));
        context.assertEquals(3, result.getJsonObject(1).fieldNames().size());
        context.assertEquals("user2", result.getJsonObject(1).getString("userId"));
        // 1 user and 1 entry with manage rights
        result = InheritShareComputer.concatShares(new JsonArray().add(userRead("user1")).add(userManage("user1")));
        context.assertEquals(1, result.size());
        context.assertEquals(4, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("user1", result.getJsonObject(0).getString("userId"));
        // 1 user and 1 entry with read rights
        result = InheritShareComputer.concatShares(new JsonArray().add(userRead("user1")).add(userRead("user1")));
        context.assertEquals(1, result.size());
        context.assertEquals(2, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("user1", result.getJsonObject(0).getString("userId"));
        // 2 groups and 2 entries
        result = InheritShareComputer.concatShares(new JsonArray().add(groupRead("group1")).add(groupContrib("group2")));
        context.assertEquals(2, result.size());
        context.assertEquals(2, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("group1", result.getJsonObject(0).getString("groupId"));
        context.assertEquals(3, result.getJsonObject(1).fieldNames().size());
        context.assertEquals("group2", result.getJsonObject(1).getString("groupId"));
        // 1 group and 1 entry with manage rights
        result = InheritShareComputer.concatShares(new JsonArray().add(groupRead("group1")).add(groupManage("group1")));
        context.assertEquals(1, result.size());
        context.assertEquals(4, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("group1", result.getJsonObject(0).getString("groupId"));
        // 1 group and 1 entry with read rights
        result = InheritShareComputer.concatShares(new JsonArray().add(groupRead("group1")).add(groupRead("group1")));
        context.assertEquals(1, result.size());
        context.assertEquals(2, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("group1", result.getJsonObject(0).getString("groupId"));
        // 2 entry with group1 and user1 with read rights
        result = InheritShareComputer.concatShares(new JsonArray().add(userRead("user1")).add(userRead("user1")).add(groupRead("group1")).add(groupRead("group1")));
        context.assertEquals(2, result.size());
        context.assertEquals(2, result.getJsonObject(0).fieldNames().size());
        context.assertEquals("user1", result.getJsonObject(0).getString("userId"));
        context.assertEquals(2, result.getJsonObject(1).fieldNames().size());
        context.assertEquals("group1", result.getJsonObject(1).getString("groupId"));
    }
}
