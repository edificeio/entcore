package org.entcore.common.utils;

import org.entcore.common.service.VisibilityFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResourceUtils {

    private static final Pattern workspaceDoc = Pattern.compile("/workspace/document/(?<id>([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}))");
    private static final Pattern workspacePubDoc = Pattern.compile("/workspace/pub/document/(?<id>([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}))");

    public static List<String> extractIds(String resource) {
        if(resource==null){
            return new ArrayList<>();
        }
        Matcher matcher = workspaceDoc.matcher(resource);
        List<String> documentsIds = new ArrayList<>();
        while (matcher.find()) {
            documentsIds.add(matcher.group("id"));
        }
        return documentsIds;
    }

    public static List<String> extractIds(String resource, VisibilityFilter filter) {
        if(resource==null){
            return new ArrayList<>();
        }
        Pattern pattern = VisibilityFilter.PUBLIC.equals(filter)?workspacePubDoc:workspaceDoc;
        Matcher matcher = pattern.matcher(resource);
        List<String> documentsIds = new ArrayList<>();
        while (matcher.find()) {
            documentsIds.add(matcher.group("id"));
        }
        return documentsIds;
    }

    public static String transformUrlTo(String resource, List<String> ids, VisibilityFilter filter) {
        if(resource==null){
            return resource;
        }
        String patternBefore = "/workspace/pub/document/%s";
        String patternAfter = "/workspace/document/%s";
        if(VisibilityFilter.PUBLIC.equals(filter)){
            patternBefore = "/workspace/document/%s";
            patternAfter = "/workspace/pub/document/%s";
        }
        for(String id: ids){
            String before = String.format(patternBefore,id);
            String after = String.format(patternAfter,id);
            resource = resource.replaceAll(before,after);
        }
        return resource;
    }

}
