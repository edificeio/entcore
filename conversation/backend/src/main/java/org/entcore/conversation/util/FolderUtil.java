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

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class FolderUtil {
    final static public String ID           = "id";
    final static public String PARENT_ID    = "parent_id";
    final static public String DEPTH        = "depth";
    final static public String SUB_FOLDERS  = "subFolders";

    final static private Comparator<JsonObject> compareOnIdAsc = (JsonObject o1, JsonObject o2) -> {
        return getId(o1).compareTo(getId(o2));
    };

    final static private Comparator<JsonObject> compareOnParentThenIdAsc = (JsonObject o1, JsonObject o2) -> {
        int parentCompared = getParentId(o1).compareTo(getParentId(o2));
        return parentCompared != 0 ? parentCompared : FolderUtil.compareOnIdAsc.compare(o1, o2);
    };

    /**
     * Converts a flat list of folders into a hierarchical tree structure based on their depth and parent-child relationships.
     *
     * @param list  the flat list of folders to be converted into a tree structure. Each folder is represented as a JsonObject.
     * @param depth the maximum depth of the folder hierarchy.
     * @return a JsonArray representing the root folders of the hierarchical tree structure.
     */
    static public JsonArray listToTree(final JsonArray list, final int depth) {
        // Sort root folders (with depth=1) by ID
        SortedSet<JsonObject> rootFolders =  new TreeSet<>(compareOnIdAsc);
        // Sort children folders (with depth=2+) by ID.
        SortedSet<JsonObject>[] folderLevelById = new SortedSet[depth];
        // Also sort children folders (with depth=2+) by parent folder ID
        SortedSet<JsonObject>[] folderLevelByParentId = new SortedSet[depth];

        // Sets of level 0 (for depth=1) are not initialized, because only `rootFolders` will be used for level 0.        
        for( int level=1; level<depth; level++ ) {
            folderLevelById[level] = new TreeSet<>(compareOnIdAsc);
            folderLevelByParentId[level] = new TreeSet<>(compareOnParentThenIdAsc);
        }
        // Put every folder in the SortedSet dedicated to its depth.
        list.stream().forEach((item) -> {
            if( ! (item instanceof JsonObject) ) return;
            final JsonObject folder = (JsonObject) item;
            final int folderLevel = (int) (getDepth(folder) - 1);
            if(folderLevel==0) {
                rootFolders.add(folder);
            } else if(0<folderLevel && folderLevel<depth) {
                folderLevelById[folderLevel].add(folder);
                folderLevelByParentId[folderLevel].add(folder);
            }
        });

        // Insert every folder of depth 2 or more in its parent folder.
        for(int level=1; level<depth; level++ ) {
            final int parentLevel = level - 1;
            Iterator<JsonObject> parentsIterator = (parentLevel==0 ? rootFolders : folderLevelById[parentLevel]).iterator();
            Iterator<JsonObject> levelIterator = folderLevelByParentId[level].iterator();
            JsonObject root = parentsIterator.hasNext() ? parentsIterator.next() : null;
            while(root!=null && levelIterator.hasNext()) {
                JsonObject sub = levelIterator.next();
                int compared = getId(root).compareTo(getParentId(sub));
                while( compared < 0 && parentsIterator.hasNext() ) {
                    root = parentsIterator.next();
                    compared = getId(root).compareTo(getParentId(sub));
                }
                if( compared == 0 ) {
                    addSubFolder(root, sub);
                }
            }
        }

        return JsonArray.of(rootFolders.toArray());
    }

    static public String getId(JsonObject o) {
        return o.getString(ID);
    }

    static public String getParentId(JsonObject o) {
        return getOrElse(o.getString(PARENT_ID), "");
    }

    static public long getDepth(JsonObject o) {
        return o.getLong(DEPTH, 0L);
    }

    static public void addSubFolder(JsonObject parent, JsonObject child) {
        if(parent == null) return;
        JsonArray subFolders = parent.getJsonArray(SUB_FOLDERS);
        if( subFolders == null) {
            subFolders = new JsonArray();
            parent.put(SUB_FOLDERS, subFolders);
        }
        subFolders.add(child);
    }
}
