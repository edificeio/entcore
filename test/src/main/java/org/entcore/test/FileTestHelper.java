package org.entcore.test;

import io.vertx.core.json.JsonObject;
import java.util.*;

public class FileTestHelper {

    public JsonObject jsonFromResource(String resource) {
        final Scanner scanner = new Scanner(getClass().getClassLoader().getResourceAsStream(resource), "UTF-8");
        try {
            final String src = scanner.useDelimiter("\\A").next();
            return new JsonObject(src);
        } finally {
            scanner.close();
        }
    }

}