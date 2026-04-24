package org.entcore.common.email.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class PostgresEmailDto {

    private UUID id;
    private String profile;
    private String module;
    private String platformId;
    private String platformUrl;
    private String userId;
    private  User from;
    private List<User> to = new ArrayList<>();
    private List<User> cc = new ArrayList<>();
    private List<User> bcc = new ArrayList<>();
    private String subject;
    private String body;
    private Map<String, String> headers = new HashMap<>();
    private int priority;
    private List<Attachment> attachments = new ArrayList<>();

    @JsonIgnore
    public JsonObject toJsonLog() {
        JsonObject json = new JsonObject();
        json.put("id", this.id.toString());
        json.put("profile", this.profile);
        json.put("platform_id", this.platformId);
        json.put("platform_url", this.platformUrl);
        json.put("user_id", this.userId);
        if (from != null) {
            json.put("from_mail", this.from.mail);
            json.put("from_name", this.from.name);
        }
        json.put("receivers", getReceivers());
        json.put("cc", getCcs());
        json.put("bcc", getBccs());
        json.put("subject", this.subject);
        json.put("headers", getHeadersJson());
        json.put("body", this.body);
        json.put("priority", this.priority);
        return json;
    }

    @JsonIgnore
    public JsonArray getReceivers() {
        JsonArray receivers = new JsonArray();
        for (User user : to) {
            receivers.add(new JsonObject().put("email", user.getMail()).put("name", user.getName()));
        }
        return receivers;
    }

    @JsonIgnore
    public JsonArray getCcs() {
        JsonArray ccs = new JsonArray();
        for (User user : cc) {
            ccs.add(new JsonObject().put("email", user.getMail()).put("name", user.getName()));
        }
        return ccs;
    }

    @JsonIgnore
    public JsonArray getBccs() {
        JsonArray bccs = new JsonArray();
        for (User user : bcc) {
            bccs.add(new JsonObject().put("email", user.getMail()).put("name", user.getName()));
        }
        return bccs;
    }

    @JsonIgnore
    public JsonArray getHeadersJson() {
        JsonArray headers = new  JsonArray();
        this.headers.forEach((key, value) -> headers.add(new JsonObject().put("name", key).put("value", value)));
        return headers;
    }

    public UUID getId() {
        return id;
    }

    public PostgresEmailDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getProfile() {
        return profile;
    }

    public PostgresEmailDto setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public String getModule() {
        return module;
    }

    public PostgresEmailDto setModule(String module) {
        this.module = module;
        return this;
    }

    public String getPlatformId() {
        return platformId;
    }

    public PostgresEmailDto setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public PostgresEmailDto setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public PostgresEmailDto setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public User getFrom() {
        return from;
    }

    public PostgresEmailDto setFrom(User from) {
        this.from = from;
        return this;
    }

    public List<User> getTo() {
        return to;
    }

    public PostgresEmailDto setTo(List<User> to) {
        this.to = to;
        return this;
    }

    public List<User> getCc() {
        return cc;
    }

    public PostgresEmailDto setCc(List<User> cc) {
        this.cc = cc;
        return this;
    }

    public List<User> getBcc() {
        return bcc;
    }

    public PostgresEmailDto setBcc(List<User> bcc) {
        this.bcc = bcc;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public PostgresEmailDto setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getBody() {
        return body;
    }

    public PostgresEmailDto setBody(String body) {
        this.body = body;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public PostgresEmailDto setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public PostgresEmailDto setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public PostgresEmailDto setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public static class User {
        private String mail;
        private String name;

        public User() {
            //for serialization
        }

        public User(String mail, String name) {
            this.mail = mail;
            this.name = name;
        }

        public String getMail() {
            return mail;
        }

        public void setMail(String mail) {
            this.mail = mail;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Attachment {
        private UUID id;
        private String name;
        private String content;

        public Attachment() {
            //for serialization
        }

        public Attachment(UUID id, String name, String content) {
            this.id = id;
            this.content = content;
            this.name = name;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
