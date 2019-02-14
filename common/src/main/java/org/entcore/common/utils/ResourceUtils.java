package org.entcore.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResourceUtils {

    private static final Pattern workspaceDoc = Pattern.compile("/workspace/document/(?<id>([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}))");

    public static List<String> extractIds(String resource) {
        Matcher matcher = workspaceDoc.matcher(resource);
        List<String> documentsIds = new ArrayList<>();
        while (matcher.find()) {
            documentsIds.add(matcher.group("id"));
        }
        return documentsIds;
    }

}
