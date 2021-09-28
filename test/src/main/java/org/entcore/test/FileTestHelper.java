package org.entcore.test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import java.util.*;

public class FileTestHelper {

    private String stringFromResource(String resource) {
        final Scanner scanner = new Scanner(getClass().getClassLoader().getResourceAsStream(resource), "UTF-8");
        try {
            return scanner.useDelimiter("\\A").next();
        } finally {
            scanner.close();
        }
    }

    public JsonObject jsonFromResource(String resource) {
        return new JsonObject(stringFromResource(resource));
    }

    public Buffer bufferFromResource(String resource) {
        return Buffer.buffer(stringFromResource(resource));
    }

}