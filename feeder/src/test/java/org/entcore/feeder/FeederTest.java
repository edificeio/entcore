package org.entcore.feeder;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class FeederTest {

    @Test
    public void testCheckPathIsStraightForward () {
        String[] paths = {"a/b/c/d", "a", "",
                // Absolute paths
                "/tmp", "/etc/passwd", "/var/log", "/root"};
        for(String path : paths) {
            boolean result = Feeder.checkPathIsStraightForward(path);
            assertTrue("path '" + path + "'containing only forward slashes should be considered straight forward", result);
        }
    }

    @Test
    public void testCheckPathIsNotStraightForward () {
        String[] paths = {
                // Whitespace
                " ", "   ", "\t", "\n", "test\ntest", "test\ttest",
                // Dot-dot traversals
                "..", "../", "../test", "../test/more",
                "test/..", "test/../", "test/../more",
                "test/../test", "test/../test/more",
                "test/../more/test", "test/../more/test/more",
                "a/../../b", "../../../etc/passwd",
                // Shell injection characters
                "test;cmd", "test&cmd", "test|cmd",
                "test$var", "test`cmd`", "test!cmd",
                "test*", "test?", "test[a]", "test{a,b}",
                // Home directory references
                "~", "~/test", "test/~/more",
                // URL-encoded traversals
                "%2e%2e/test", "test%2ftest", "%2e%2e%2f",
                // Null byte injection
                "test\0evil",
                // Windows-style separators
                "..\\test", "C:\\test"
        };
        for(String path : paths) {
            boolean result = Feeder.checkPathIsStraightForward(path);
            assertFalse("path '" + path + "' should NOT be considered straight forward", result);
        }
    }
}
