package org.entcore.common.explorer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.share.ShareModel;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class ExplorerMessage {

    public enum ExplorerContentType{
        Text, Html, Pdf
    }
    public enum ExplorerAction{
        Upsert(ExplorerPriority.High),
        Delete(ExplorerPriority.High),
        Audience(ExplorerPriority.Low);
        private final ExplorerPriority priority;
        ExplorerAction(final ExplorerPriority i){
            this.priority = i;
        }

        public ExplorerPriority getPriority(boolean forSearch) {
            if(forSearch){
                return ExplorerPriority.Low;
            }
            return priority;
        }
    }

    public enum ExplorerPriority{
        High(1), Medium(0), Low(-1);
        private final int value;
        ExplorerPriority(final int i){
            this.value = i;
        }

        public int getValue() {
            return value;
        }
    }

    private String id;
    private final String action;
    private final JsonObject message = new JsonObject();
    private final ExplorerPriority priority;
    private String idQueue;

    public static String CONTENT_KEY = "content";
    public static String CONTENT_HTML_KEY = "contentHtml";
    public static String CONTENT_PDF_KEY = "contentPdf";
    public static String SUBRESOURCES_KEY = "subresources";

    public ExplorerMessage(final String id, final String action, final ExplorerPriority priority) {
        this.id = id;
        this.action = action;
        this.priority = priority;
    }
    public ExplorerMessage(final String id, final ExplorerAction action, final boolean search) {
        this.id = id;
        this.action = action.name();
        this.priority = action.getPriority(search);
    }

    public static ExplorerMessage upsert(final IdAndVersion id, final UserInfos user, final boolean forSearch,
                                         final String application, final String resourceType, final String entityType) {
        final ExplorerMessage builder = new ExplorerMessage(id.getId(), ExplorerAction.Upsert, forSearch);
        // dont set creator and createdat here, set it from resource json
        builder.message.put("updatedAt", new Date().getTime());
        builder.message.put("updaterId", user.getUserId());
        builder.message.put("updaterName", user.getUsername());
        builder.withVersion(id.getVersion());
        builder.withType(application, resourceType, entityType);
        return builder;
    }

    public static ExplorerMessage delete(final IdAndVersion id, final UserInfos user, final boolean forSearch) {
        final ExplorerMessage builder = new ExplorerMessage(id.getId(), ExplorerAction.Delete, forSearch);
        builder.message.put("version", id.getVersion());
        builder.message.put("deletedAt", new Date().getTime());
        builder.message.put("deleterId", user.getUserId());
        builder.message.put("deleterName", user.getUsername());
        return builder;
    }

    public ExplorerMessage withCreator(final UserInfos user){
        withCreatorId(user.getUserId());
        withCreatorName(user.getUsername());
        return this;
    }

    public ExplorerMessage withCreatedAt(final Date date){
        message.put("createdAt", date.getTime());
        return this;
    }

    public ExplorerMessage withCreatorId(final String id){
        message.put("creatorId", id);
        return this;
    }

    public ExplorerMessage withCreatorName(final String name){
        message.put("creatorName", name);
        return this;
    }

    public ExplorerMessage withParentId(final Optional<Long> parentId) {
        final JsonObject override = this.getOverrideSafe();
        if(parentId.isPresent()){
            override.put("parentId", parentId.get().toString());
        }
        this.withOverrideFields(override);
        return this;
    }

    public ExplorerMessage withParentEntId(final Optional<String> parentId) {
        final JsonObject override = this.getOverrideSafe();
        if(parentId.isPresent()){
            override.put("parentEntId", parentId.get());
        }
        this.withOverrideFields(override);
        return this;
    }

    public ExplorerMessage withChildrenEntId(final Set<String> childId) {
        final JsonObject override = this.getOverrideSafe();
        override.put("childEntId", new JsonArray(new ArrayList<>(childId)));
        this.withOverrideFields(override);
        return this;
    }

    public ExplorerMessage withMigrationFlag(final boolean migration) {
        final JsonObject override = this.getOverrideSafe();
        override.put("migration", migration);
        this.withOverrideFields(override);
        return this;
    }

    public ExplorerMessage withPublic(final boolean pub) {
        message.put("public", pub);
        return this;
    }

    public ExplorerMessage withTrashed(final boolean trashed){
        message.put("trashed", trashed);
        return this;
    }

    public ExplorerMessage withThumbnail(final String thumbnail){
        message.put("thumbnail", thumbnail);
        return this;
    }

    public ExplorerMessage withDescription(final String description){
        message.put("description", description);
        return this;
    }

    public ExplorerMessage withType(final String application, final String resourceType, final String entityType) {
        message.put("application", application);
        message.put("resourceType", resourceType);
        message.put("entityType", entityType);
        return this;
    }
    public ExplorerMessage withVersion(Long version) {
        message.put("version", version);
        return this;
    }

    public ExplorerMessage withMute(String userId, boolean mute) {
        message.put("mute", new JsonObject().put(userId, mute));
        return this;
    }

    public ExplorerMessage withTrashedBy(List<String> trashedBy) {
        message.put("trashedBy", new JsonArray(trashedBy));
        return this;
    }

    public ExplorerMessage withForceApplication(final String application) {
        message.put("application", application);
        return this;
    }

    public ExplorerMessage withName(final String name) {
        message.put("name", name);
        return this;
    }

    public ExplorerMessage withContent(final String text, final ExplorerContentType type) {
        if(text== null){
            return this;
        }
        switch(type){
            case Pdf:
                message.put(CONTENT_PDF_KEY, text);
                break;
            case Html:
                message.put(CONTENT_HTML_KEY, text);
                break;
            case Text:
            default:
                message.put(CONTENT_KEY, text);
                break;
        }
        return this;
    }

    public ExplorerMessage withCustomFields(final JsonObject values) {
        message.put("custom", values);
        return this;
    }

    public ExplorerMessage withOverrideFields(final JsonObject values) {
        message.put("override", values);
        return this;
    }

    public ExplorerMessage withSubResourceContent(final String id, final String content, final ExplorerContentType type) {
        final JsonArray subResources = message.getJsonArray(SUBRESOURCES_KEY, new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        switch(type){
            case Pdf:
                subResource.put(CONTENT_PDF_KEY, content);
                break;
            case Html:
                subResource.put(CONTENT_HTML_KEY, content);
                break;
            case Text:
            default:
                subResource.put(CONTENT_KEY, content);
                break;
        }
        subResource.put("deleted", false);
        subResources.add(subResource);
        message.put(SUBRESOURCES_KEY, subResources);
        return this;
    }

    public ExplorerMessage withSubResources(final JsonArray subResources) {
        message.put(SUBRESOURCES_KEY, subResources);
        return this;
    }

    public ExplorerMessage withSubResource(final String id, final boolean deleted) {
        final JsonArray subResources = message.getJsonArray(SUBRESOURCES_KEY, new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        subResource.put("deleted", deleted);
        subResources.add(subResource);
        message.put(SUBRESOURCES_KEY, subResources);
        return this;
    }

    public ExplorerMessage withSubResourceHtml(final String id, final String content, final long version) {
        final JsonArray subResources = message.getJsonArray(SUBRESOURCES_KEY, new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        subResource.put(CONTENT_HTML_KEY, content);
        subResource.put("deleted", false);
        subResource.put("version", version);
        subResources.add(subResource);
        message.put(SUBRESOURCES_KEY, subResources);
        return this;
    }

    public ExplorerMessage withShared(final ShareModel shareModel) {
        message.put("rights", new JsonArray(shareModel.getSerializedRights()));
        return this;
    }

    @Deprecated
    public ExplorerMessage withShared(final JsonArray shared, final List<String> rights) {
        // dont need to push shared to explorer
        message.put("rights", new JsonArray(rights));
        return this;
    }

    public ExplorerMessage withForceId(final String id) {
        this.id = id;
        return this;
    }

    public ExplorerMessage withSkipCheckVersion(final boolean skipCheckVersion) {
        message.put("skipCheckVersion", skipCheckVersion);
        return this;
    }

    public boolean getMigrationFlag(){
        return this.getOverrideSafe().getBoolean("migration", false);
    }

    public boolean getSkipCheckVersion(){
        return this.message.getBoolean("skipCheckVersion", false);
    }

    public Optional<String> getParentEntId(){
        return Optional.ofNullable(this.getOverrideSafe().getString("parentEntId"));
    }
    public Optional<String> getParentId(){
        return Optional.ofNullable(this.getOverrideSafe().getValue("parentId")).map(e->e.toString());
    }
    public Optional<Set<String>> getChildEntId(){
        return Optional.ofNullable(this.getOverrideSafe().getValue("childEntId")).filter(e-> {
            return e instanceof  JsonArray;
        }).map(e->{
          final JsonArray arr = (JsonArray) e;
          return arr.stream().map(row -> row.toString()).collect(Collectors.toSet());
        });
    }

    public JsonObject getMessage() {
        return message;
    }
    public String getId() {
        return id;
    }
    public Optional<JsonArray> getOptionalShared() {
        return Optional.ofNullable(message.getJsonArray("shared"));
    }
    public Optional<JsonArray> getOptionalRights() {
        return Optional.ofNullable(message.getJsonArray("rights"));
    }
    public JsonArray getShared() {
        return message.getJsonArray("shared", new JsonArray());
    }
    public JsonArray getRights() {
        return message.getJsonArray("rights", new JsonArray());
    }
    public String getCreatorId() {
        return message.getString("creatorId", "");
    }
    public String getUpdaterId() {
        return message.getString("updaterId", "");
    }
    public String getName() {
        return message.getString("name");
    }
    public long getVersion() {
        return message.getLong("version");
    }
    public String getCreatorName() {
        return message.getString("creatorName");
    }
    public String getApplication() {
        return message.getString("application");
    }
    public String getResourceType() {
        return message.getString("resourceType");
    }
    public String getEntityType() {
        return message.getString("entityType");
    }
    public JsonObject getOverride() {
        return message.getJsonObject("override");
    }
    public JsonObject getOverrideSafe() {
        return message.getJsonObject("override", new JsonObject());
    }
    public String getAction() {
        return action;
    }
    public ExplorerPriority getPriority() {
        return priority;
    }
    public String getResourceUniqueId() {
        return getId()+":"+getApplication()+":"+getResourceType();
    }
    public JsonArray getSubresources() {
        return this.message.getJsonArray(SUBRESOURCES_KEY, new JsonArray());
    }
    public void setIdQueue(String idQueue) {
        this.idQueue = idQueue;
    }
    public JsonObject getMute() {
        final JsonObject mute = this.message.getJsonObject("mute");
        if(mute == null) {
            return new JsonObject();
        }
        return mute;
    }
    public JsonArray getTrashedBy() {
        final JsonArray trashedBy = this.message.getJsonArray("trashedBy");
        if (trashedBy == null) {
            return new JsonArray();
        }
        return trashedBy;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ExplorerMessage{");
        sb.append("id='").append(id).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append(", message=").append(message);
        sb.append(", priority=").append(priority);
        sb.append(", idQueue='").append(idQueue).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
