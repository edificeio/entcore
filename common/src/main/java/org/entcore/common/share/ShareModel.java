package org.entcore.common.share;

import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class ShareModel {


    private final JsonArray sharedJson;
    private final Optional<String> creatorId;
    private final Map<String, ShareRoles> reverseNormalizedRights;
    private final Map<String, Set<ShareRoles>> normalizedRightsByUser = new HashMap<>();
    private final Map<String, Set<ShareRoles>> normalizedRightsByGroup = new HashMap<>();

    public ShareModel(final JsonArray sharedJson, final Map<String, SecuredAction> actions, final Optional<String> creatorId) {
        this.sharedJson = sharedJson;
        this.creatorId = creatorId;
        this.reverseNormalizedRights = ShareRoles.getRoleBySecuredActionName(actions);
        for(final Object shareObject : sharedJson.getList()){
            if(shareObject instanceof JsonObject){
                final JsonObject jsonShare = (JsonObject) shareObject;
                // get role enum from fqdn java name
                final Set<ShareRoles> roles = new HashSet<>();
                for(final String key : jsonShare.fieldNames()){
                    final Object value = jsonShare.getValue(key);
                    if(Boolean.TRUE.equals(value) && reverseNormalizedRights.containsKey(key)){
                        final ShareRoles found = reverseNormalizedRights.get(key);
                        roles.add(found);
                    }
                }
                // set map from userId or groupId
                if(jsonShare.containsKey("groupId")){
                    final String groupId = jsonShare.getString("groupId");
                    this.normalizedRightsByGroup.put(groupId, roles);
                }else if(jsonShare.containsKey("userId")){
                    final String userId = jsonShare.getString("userId");
                    this.normalizedRightsByUser.put(userId, roles);
                }
            }
        }
    }

    /**
     *
     * @return set of rights using this serialization model: [creator:USER_ID, user:read:USER_ID, group:contrib:GROUPID,...]
     */
    public List<String> getSerializedRights(){
        //prepare visible set
        final List<String> visibleBy = new ArrayList<>();
        // serialize creator right
        if(creatorId.isPresent()){
            visibleBy.add(ShareRoles.getSerializedForCreator(creatorId.get()));
        }
        //for each user right serialize it
        for (final Map.Entry<String, Set<ShareRoles>> entryUser : this.normalizedRightsByUser.entrySet()) {
            final String userId = entryUser.getKey();
            final Set<ShareRoles> roles = entryUser.getValue();
            for (final ShareRoles role : roles) {
                visibleBy.add(role.getSerializedForUser(userId));
            }
        }
        //for each user right serialize it
        for (final Map.Entry<String, Set<ShareRoles>> entryGroup : this.normalizedRightsByGroup.entrySet()) {
            final String groupId = entryGroup.getKey();
            final Set<ShareRoles> roles = entryGroup.getValue();
            for (final ShareRoles role : roles) {
                visibleBy.add(role.getSerializedForGroup(groupId));
            }
        }
        return visibleBy;
    }

    public Map<String, ShareRoles> getReverseNormalizedRights() {
        return reverseNormalizedRights;
    }

    public Map<String, Set<ShareRoles>> getNormalizedRightsByGroup() {
        return normalizedRightsByGroup;
    }

    public Map<String, Set<ShareRoles>> getNormalizedRightsByUser() {
        return normalizedRightsByUser;
    }

    public JsonArray getSharedJson() {
        return sharedJson;
    }

    public Optional<String> getCreatorId() {
        return creatorId;
    }
}
