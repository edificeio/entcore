package org.entcore.common.explorer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Date;
import java.util.Optional;

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

    public static ExplorerMessage upsert(final String id, final UserInfos user, final boolean forSearch) {
        final ExplorerMessage builder = new ExplorerMessage(id, ExplorerAction.Upsert, forSearch);
        builder.message.put("createdAt", new Date().getTime());
        builder.message.put("creatorId", user.getUserId());
        builder.message.put("creatorName", user.getUsername());
        builder.message.put("updatedAt", new Date().getTime());
        builder.message.put("updaterId", user.getUserId());
        builder.message.put("updaterName", user.getUsername());
        return builder;
    }

    public static ExplorerMessage delete(final String id, final UserInfos user, final boolean forSearch) {
        final ExplorerMessage builder = new ExplorerMessage(id, ExplorerAction.Delete, forSearch);
        builder.message.put("deletedAt", new Date().getTime());
        builder.message.put("deleterId", user.getUserId());
        builder.message.put("deleterName", user.getUsername());
        return builder;
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

    public ExplorerMessage withType(final String application, final String resourceType) {
        message.put("application", application);
        message.put("resourceType", resourceType);
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
                message.put("contentPdf", text);
                break;
            case Html:
                message.put("contentHtml", text);
                break;
            case Text:
            default:
                message.put("content", text);
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
        final JsonArray subResources = message.getJsonArray("subresources", new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        switch(type){
            case Pdf:
                subResource.put("contentPdf", content);
                break;
            case Html:
                subResource.put("contentHtml", content);
                break;
            case Text:
            default:
                subResource.put("content", content);
                break;
        }
        subResource.put("deleted", false);
        subResources.add(subResource);
        message.put("subresources", subResources);
        return this;
    }

    public ExplorerMessage withSubResource(final String id, final boolean deleted) {
        final JsonArray subResources = message.getJsonArray("subresources", new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        subResource.put("deleted", deleted);
        subResources.add(subResource);
        message.put("subresources", subResources);
        return this;
    }

    public ExplorerMessage withSubResourceHtml(final String id, final String content) {
        final JsonArray subResources = message.getJsonArray("subresources", new JsonArray());
        final Optional<JsonObject> subResourceOpt = subResources.stream().map(e -> (JsonObject)e).filter(e-> e.getString("id","").equals(id)).findFirst();
        final JsonObject subResource = subResourceOpt.orElse(new JsonObject().put("id", id));
        subResource.put("contentHtml", content);
        subResource.put("deleted", false);
        subResources.add(subResource);
        message.put("subresources", subResources);
        return this;
    }

    public ExplorerMessage withShared(final JsonArray shared) {
        message.put("shared", shared);
        return this;
    }

    public ExplorerMessage withForceId(final String id) {
        this.id = id;
        return this;
    }

    public boolean getMigrationFlag(){
        return this.getOverrideSafe().getBoolean("migration", false);
    }
    public Optional<String> getParentEntId(){
        return Optional.ofNullable(this.getOverrideSafe().getString("parentEntId"));
    }
    public Optional<String> getParentId(){
        return Optional.ofNullable(this.getOverrideSafe().getValue("parentId")).map(e->e.toString());
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
    public JsonArray getShared() {
        return message.getJsonArray("shared", new JsonArray());
    }
    public String getCreatorId() {
        return message.getString("creatorId");
    }
    public String getName() {
        return message.getString("name");
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

    public void setIdQueue(String idQueue) {
        this.idQueue = idQueue;
    }
}
