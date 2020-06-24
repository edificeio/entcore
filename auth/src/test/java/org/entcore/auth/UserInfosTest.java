package org.entcore.auth;

import org.entcore.auth.adapter.UserInfoAdapterV1_0Json;
import org.entcore.auth.adapter.UserInfoAdapterV1_1Json;
import org.entcore.auth.adapter.UserInfoAdapterV2_0Json;
import org.entcore.auth.adapter.UserInfoAdapterV2_1Json;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserInfosTest {

    private JsonObject session() {
        return new JsonObject().put("type", "Teacher").put("cache", new JsonObject())
                .put("realClassesNames", new JsonArray()).put("classes", new JsonArray())
                .put("classNames", new JsonArray().add("CM2")).put("structureNames", new JsonArray().add("Ecole1"))
                .put("groupsIds", new JsonArray()).put("structures", new JsonArray()).put("apps", new JsonArray())
                .put("authorizedActions",
                        new JsonArray().add(new JsonObject().put("name", "client1|action1"))
                                .add(new JsonObject().put("name", "action2")))
                .put("children", new JsonArray()).put("functions", new JsonArray()).put("federated", false);
    }

    private void adapterWithClientId(TestContext context, JsonObject res) {
        context.assertEquals("ENSEIGNANT", res.getString("type"));
        context.assertNull(res.getJsonObject("cache"));
        context.assertEquals("", res.getString("level"));
        context.assertNull(res.getJsonArray("classNames"));
        context.assertNull(res.getJsonArray("structureNames"));
        context.assertNull(res.getBoolean("federated"));
        context.assertEquals("CM2", res.getString("classId"));
        context.assertEquals("Ecole1", res.getString("schoolName"));
        context.assertNull(res.getJsonArray("functions"));
        context.assertNull(res.getJsonArray("groupsIds"));
        context.assertNull(res.getJsonArray("structures"));
        context.assertNull(res.getJsonArray("apps"));
        context.assertNull(res.getJsonArray("children"));
        context.assertEquals(1, res.getJsonArray("authorizedActions").size());
        context.assertEquals("client1|action1",
                res.getJsonArray("authorizedActions").getJsonObject(0).getString("name"));
    }

    private void adapterWithoutClient(TestContext context, JsonObject res) {
        context.assertEquals("ENSEIGNANT", res.getString("type"));
        context.assertNull(res.getJsonObject("cache"));
        context.assertEquals("", res.getString("level"));
        context.assertNotNull(res.getJsonArray("classNames"));
        context.assertNotNull(res.getJsonArray("structureNames"));
        context.assertNotNull(res.getBoolean("federated"));
        context.assertNull(res.getString("classId"));
        context.assertNull(res.getString("schoolName"));
        context.assertNotNull(res.getJsonArray("functions"));
        context.assertNotNull(res.getJsonArray("groupsIds"));
        context.assertNotNull(res.getJsonArray("structures"));
        context.assertNotNull(res.getJsonArray("apps"));
        context.assertNotNull(res.getJsonArray("children"));
        context.assertEquals(2, res.getJsonArray("authorizedActions").size());
    }

    @Test
    public void testUserInfoShouldGenrateV1_0WithClientId(TestContext context) {
        final UserInfoAdapterV1_0Json adapter = new UserInfoAdapterV1_0Json();
        final JsonObject res = adapter.getInfo(session(), "client1");
        adapterWithClientId(context, res);
        context.assertNull(res.getJsonArray("realClassesNames"));
        context.assertNull(res.getJsonArray("classes"));

    }

    @Test
    public void testUserInfoShouldGenrateV1_0WithoutClient(TestContext context) {
        final UserInfoAdapterV1_0Json adapter = new UserInfoAdapterV1_0Json();
        final JsonObject res = adapter.getInfo(session(), null);
        adapterWithoutClient(context, res);
        context.assertNull(res.getJsonArray("realClassesNames"));
        context.assertNotNull(res.getJsonArray("classes"));
    }

    @Test
    public void testUserInfoShouldGenrateV1_1WithClientId(TestContext context) {
        final UserInfoAdapterV1_1Json adapter = new UserInfoAdapterV1_1Json();
        final JsonObject res = adapter.getInfo(session(), "client1");
        adapterWithClientId(context, res);
        context.assertNotNull(res.getJsonArray("realClassesNames"));
        context.assertNotNull(res.getJsonArray("classes"));
        context.assertNotNull(res.getJsonArray("structureIds"));
    }

    @Test
    public void testUserInfoShouldGenrateV1_1WithoutClient(TestContext context) {
        final UserInfoAdapterV1_1Json adapter = new UserInfoAdapterV1_1Json();
        final JsonObject res = adapter.getInfo(session(), null);
        adapterWithoutClient(context, res);
        context.assertNotNull(res.getJsonArray("realClassesNames"));
        context.assertNotNull(res.getJsonArray("classes"));
        context.assertNotNull(res.getJsonArray("structureIds"));
    }

    @Test
    public void testUserInfoShouldGenrateV2_0(TestContext context) {
        final UserInfoAdapterV2_0Json adapter = new UserInfoAdapterV2_0Json();
        final JsonObject session = session();
        final JsonObject res = adapter.getInfo(session, null);
        context.assertNull(res.getJsonObject("cache"));
        context.assertEquals(session.getMap().keySet().size() - 1, res.getMap().keySet().size());
        for (final String key : res.getMap().keySet()) {
            context.assertEquals(session.getValue(key), res.getMap().get(key));
        }
    }

    @Test
    public void testUserInfoShouldGenrateV2_1(TestContext context) {
        final UserInfoAdapterV2_1Json adapter = new UserInfoAdapterV2_1Json();
        final JsonObject session = session();
        final JsonObject res = adapter.getInfo(session, null);
        context.assertEquals(session.getMap().keySet().size(), res.getMap().keySet().size());
        for (final String key : res.getMap().keySet()) {
            context.assertEquals(session.getValue(key), res.getMap().get(key));
        }
    }
}