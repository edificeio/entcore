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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class FolderUtil {
    final static public String ID           = "id";
    final static public String PARENT_ID    = "parent_id";
    final static public String DEPTH        = "depth";
    final static public String SUB_FOLDERS  = "subFolders";

    final static private Comparator<JsonObject> compareOnParentAsc = (JsonObject o1, JsonObject o2) -> {
        return getParentId(o1).compareTo(getParentId(o2));
    };

    final static private Comparator<JsonObject> compareOnIdAsc = (JsonObject o1, JsonObject o2) -> {
        return getId(o1).compareTo(getId(o2));
    };


    static public JsonArray listToTree(final JsonArray list) {
        // Root folders sorted by id
        List<JsonObject> rootFolders = new ArrayList<>();
        // Sub-folders sorted by parent_id
        List<JsonObject> subFolders = new ArrayList<>();

        list.stream().forEach((item) -> {
            if( ! (item instanceof JsonObject) ) return;
            final JsonObject folder = (JsonObject) item;
            (getDepth(folder) == 1 ? rootFolders : subFolders).add(folder);
        });
        Collections.sort(rootFolders, compareOnIdAsc);
        Collections.sort(subFolders, compareOnParentAsc);

        Iterator<JsonObject> rootsIterator = rootFolders.iterator();
        Iterator<JsonObject> subsIterator = subFolders.iterator();
        JsonObject root = rootsIterator.hasNext() ? rootsIterator.next() : null;
        while(root!=null && subsIterator.hasNext()) {
            JsonObject sub = subsIterator.next();
            int compared = getId(root).compareTo(getParentId(sub));
            while( compared < 0 && rootsIterator.hasNext() ) {
                root = rootsIterator.next();
                compared = getId(root).compareTo(getParentId(sub));
            }
            if( compared == 0 ) {
                addSubFolder(root, sub);
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
