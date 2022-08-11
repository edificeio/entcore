package org.entcore.test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import java.util.*;

public class FileTestHelper {

    /** 
     * Load the content of a resource file (under the test/resources directory) as a String
     * @param resource the file pathname
     * @return The file content
     */
    private String stringFromResource(String resource) {
        final Scanner scanner = new Scanner(getClass().getClassLoader().getResourceAsStream(resource), "UTF-8");
        try {
            return scanner.useDelimiter("\\A").next();
        } finally {
            scanner.close();
        }
    }

    /** 
     * Load the JSON content of a resource file (under the test/resources directory)
     * Example: {@code JsonObject config = test.file().jsonFromResource("config/entcore.json");}
     * @param resource the file pathname
     * @return The JSON (object) content
     */
    public JsonObject jsonFromResource(String resource) {
        return new JsonObject(stringFromResource(resource));
    }

    /** 
     * Bufferize the content of a resource file (under the test/resources directory)
     * Example: {@code Buffer onePriceGrid = test.file().bufferFromResource("price_grid/ONE.csv");}
     * @param resource the file pathname
     * @return The file content bufferized
     */
    public Buffer bufferFromResource(String resource) {
        return Buffer.buffer(stringFromResource(resource));
    }

}