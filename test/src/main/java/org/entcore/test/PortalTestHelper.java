package org.entcore.test;

import java.util.Map;

public class PortalTestHelper {
    private final TestHelper test;

    public PortalTestHelper(TestHelper t) {
        this.test = t;
    }

    public Map<String, String> skins(String host, String name) {
        final Map<String, String> skins = test.vertx.sharedData().getLocalMap("skins");
        return skins;
    }

    public PortalTestHelper addSkin(String host, String name) {
        final Map<String, String> skins = test.vertx.sharedData().getLocalMap("skins");
        skins.put(host, name);
        return this;
    }
}