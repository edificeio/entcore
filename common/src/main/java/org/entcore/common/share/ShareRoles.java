package org.entcore.common.share;

import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ShareRoles {
    Read("read"),
    Contrib("contrib"),
    Manager("manager"),
    Publish("publish"),
    Comment("comment");
    public final String key;

    private static final String VISIBLE_BY_CREATOR = "creator";
    private static final String VISIBLE_BY_USER = "user";
    private static final String VISIBLE_BY_GROUP = "group";

    ShareRoles(final String key) {
        this.key = key;
    }

    public String getSerializedForUser(final String userId){
        return getSerializedForUserAndRole(userId, this.key);
    }

    public String getSerializedForGroup(final String groupId){
        return getSerializedForGroupAndRole(groupId, this.key);
    }

    public static String getSerializedForCreator(final String userId){
        return VISIBLE_BY_CREATOR + ":" + userId;
    }

    public static String getSerializedForUserAndRole(final String userId, final String role){
        return VISIBLE_BY_USER + ":" + userId + ":" + role;
    }

    public static String getSerializedForGroupAndRole(final String groupId, final String role){
        return VISIBLE_BY_GROUP + ":" + groupId + ":" + role;
    }


    /**
     * @param action
     * @return true if securedaction is a resourc eright
     */
    public static boolean isRoleBasedAction(final SecuredAction action) {
        if (action == null || action.getDisplayName() == null || !ActionType.RESOURCE.name().equalsIgnoreCase(action.getType())) {
            return false;
        }
        final String role = action.getDisplayName().substring(action.getDisplayName().lastIndexOf('.') + 1);
        return isRoleName(role);
    }

    public static boolean isRoleName(final String toCheck) {
        for (final ShareRoles role : ShareRoles.values()) {
            if (role.key.equalsIgnoreCase(toCheck)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<ShareRoles> getRoleByName(final String toCheck) {
        for (final ShareRoles role : ShareRoles.values()) {
            if (role.key.equalsIgnoreCase(toCheck) || toCheck.contains("."+role.key)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param securedActions generated from security annotation
     * @return JSON like {blog.comment: ["org-entcore-blog-controllers-PostController|updateComment" ...}
     */
    public static JsonObject getSecuredActionNameByNormalizedRole(final Map<String, SecuredAction> securedActions) {
        final JsonObject rights = new JsonObject();
        for (SecuredAction action: securedActions.values()) {
            if (ShareRoles.isRoleBasedAction(action)) {
                JsonArray a = rights.getJsonArray(action.getDisplayName());
                if (a == null) {
                    a = new fr.wseduc.webutils.collections.JsonArray();
                    rights.put(action.getDisplayName(), a);
                }
                a.add(action.getName().replaceAll("\\.", "-"));
            }
        }
        return rights;
    }

    /**
     *
     * @param securedActions generated from security annotation
     * @return Map like { "org-entcore-blog-controllers-PostController|updateComment": Roles.Comment, ...}
     */
    public static Map<String, ShareRoles> getRoleBySecuredActionName(final Map<String, SecuredAction> securedActions) {
        final JsonObject rights = getSecuredActionNameByNormalizedRole(securedActions);
        final Map<String, ShareRoles> newRights = new HashMap<>();
        for(final String key : rights.fieldNames()){
            final Optional<ShareRoles> foundRole = ShareRoles.getRoleByName(key);
            if(foundRole.isPresent()){
                for(final Object fqdn : rights.getJsonArray(key).getList()){
                    newRights.put(fqdn.toString(), foundRole.get());
                }
            }
        }
        return newRights;
    }
}
