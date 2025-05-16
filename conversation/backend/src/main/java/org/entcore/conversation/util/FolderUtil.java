/* Copyright © "Edifice", 2024
 *
 * This program is published by "Edifice".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Edifice" with a reference to the website: https://edifice.io/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 */
package org.entcore.conversation.util;

import java.util.*;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;


public class FolderUtil {

    final static public String ID          = "id";
    final static public String PARENT_ID   = "parent_id";
    final static public String NAME        = "name";
    final static public String SUB_FOLDERS = "subFolders";

    /**
     * Converts a flat list of folders into a hierarchical tree structure based on their depth and parent-child relationships.
     *
     * @param list  the flat list of folders to be converted into a tree structure. Each folder is represented as a JsonObject.
     * @param depth the maximum depth of the folder hierarchy.
     * @return a JsonArray representing the root folders of the hierarchical tree structure.
     */
    static public JsonArray listToTree(final JsonArray list, final int depth) {
        // Sort the list, case-insensitive, but capitals are prioritized
        List<JsonObject> sortedList = list.stream()
            .map(JsonObject.class::cast)
            .sorted(Comparator.comparing(
                o -> o.getString(NAME),
                (s1, s2) -> {
                    int compare = s1.compareToIgnoreCase(s2);
                    return compare != 0 ? compare : s1.compareTo(s2);
                }
            ))
            .collect(Collectors.toList());

        // Indexing
        Map<String, JsonObject> itemsById = new HashMap<>();
        Map<String, List<JsonObject>> childrenMap = new HashMap<>();

        for (JsonObject item: sortedList) {
            itemsById.put(item.getString(ID), item.copy());

            String parentId = item.getString(PARENT_ID);
            if (!StringUtils.isEmpty(parentId)) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
            }
        }

        // Build the tree, start by the bottom
        JsonArray result = new JsonArray();
        for (JsonObject item: sortedList) {
            String parentId = item.getString(PARENT_ID);
            if (StringUtils.isEmpty(parentId)) {
                JsonObject node = buildNodeWithDepth(item.copy(), childrenMap, itemsById, 1, depth);
                result.add(node);
            }
        }

        return result;
    }

    private static JsonObject buildNodeWithDepth(JsonObject node, Map<String, List<JsonObject>> childrenMap, Map<String, JsonObject> byId, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return node;
        }

        String nodeId = node.getString(ID);
        List<JsonObject> children = childrenMap.get(nodeId);
        if (children != null && !children.isEmpty()) {
            JsonArray childArray = new JsonArray();
            for (JsonObject child: children) {
                JsonObject childCopy = byId.get(child.getString(ID)).copy();
                childArray.add(buildNodeWithDepth(childCopy, childrenMap, byId, depth + 1, maxDepth));
            }
            node.put(SUB_FOLDERS, childArray);
        }

        return node;
    }

}