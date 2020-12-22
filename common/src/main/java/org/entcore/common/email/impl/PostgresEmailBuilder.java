package org.entcore.common.email.impl;

import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostgresEmailBuilder {

    public static EmailBuilder mail() {
        return new EmailBuilder();
    }

    public static AttachmentBuilder attachment(EmailBuilder mail) {
        return new AttachmentBuilder((UUID) mail.getMail().get("id"), (LocalDateTime) mail.getMail().get("date"));
    }

    public static class EmailBuilder {
        private final Map<String, Object> mail = new HashMap<>();

        EmailBuilder() {
            mail.put("id", UUID.randomUUID());
            mail.put("date", LocalDateTime.now());
        }

        public EmailBuilder withProfile(String profile) {
            mail.put("profile", profile);
            return this;
        }

        public EmailBuilder withModule(String module) {
            mail.put("module", module);
            return this;
        }

        public EmailBuilder withPlatformId(String platform_id) {
            mail.put("platform_id", platform_id);
            return this;
        }

        public EmailBuilder withPlatformUrl(String platform_url) {
            mail.put("platform_url", platform_url);
            return this;
        }

        public EmailBuilder withUserId(String user_id) {
            mail.put("user_id", user_id);
            return this;
        }

        public EmailBuilder withFrom(String from) {
            return this.withFrom(from, null);
        }

        public EmailBuilder withFrom(String from, String name) {
            mail.put("from_mail", from);
            if (!StringUtil.isNullOrEmpty(name)) {
                mail.put("from_name", name);
            }
            return this;
        }

        public EmailBuilder withTo(String toMail) {
            return this.withTo(toMail, null);
        }

        public EmailBuilder withTo(String toMail, String toName) {
            mail.putIfAbsent("receivers", new JsonArray());
            final JsonObject json = new JsonObject().put("email", toMail);
            if (!StringUtil.isNullOrEmpty(toName)) {
                json.put("name", toName);
            }
            ((JsonArray) mail.get("receivers")).add(json);
            return this;
        }

        public EmailBuilder withCc(String cc) {
            return this.withCc(cc, null);
        }

        public EmailBuilder withCc(String cc, String ccName) {
            mail.putIfAbsent("cc", new JsonArray());
            final JsonObject json = new JsonObject().put("email", cc);
            if (!StringUtil.isNullOrEmpty(ccName)) {
                json.put("name", ccName);
            }
            ((JsonArray) mail.get("cc")).add(json);
            return this;
        }

        public EmailBuilder withBcc(String bcc) {
            return this.withBcc(bcc, null);
        }

        public EmailBuilder withBcc(String bcc, String bccName) {
            mail.putIfAbsent("bcc", new JsonArray());
            final JsonObject json = new JsonObject().put("email", bcc);
            if (!StringUtil.isNullOrEmpty(bccName)) {
                json.put("name", bccName);
            }
            ((JsonArray) mail.get("bcc")).add(json);
            return this;
        }

        public EmailBuilder withSubject(String subject) {
            mail.put("subject", subject);
            return this;
        }

        public EmailBuilder withHeader(String name, String value) {
            mail.putIfAbsent("headers", new JsonArray());
            return withHeader(new JsonObject().put("name", value).put("value", value));
        }

        public EmailBuilder withHeader(JsonObject header) {
            mail.putIfAbsent("headers", new JsonArray());
            ((JsonArray) mail.get("headers")).add(header);
            return this;
        }

        public EmailBuilder withBody(String body) {
            mail.put("body", body);
            return this;
        }

        public EmailBuilder withPriority(int priority) {
            mail.put("priority", priority);
            return this;
        }

        public Map<String, Object> getMail() {
            return mail;
        }

        public JsonObject toJsonObject(){
            final JsonObject json = JsonObject.mapFrom(new HashMap<>(mail));
            json.remove("date");
            json.put("id", json.getValue("id").toString());
            return json;
        }

        public EmailBuilder fromJson(JsonObject json){
            final JsonObject copy = json.copy();
            this.mail.putAll(copy.getMap());
            if(json.containsKey("id")){
                this.mail.put("id", UUID.fromString(json.getString("id")));
            }
            return this;
        }
    }

    public static class AttachmentBuilder {
        private final Map<String, Object> attachment = new HashMap<>();

        AttachmentBuilder(UUID id, LocalDateTime date) {
            attachment.put("mail_id", id);
            attachment.put("date", date);
            attachment.put("id", UUID.randomUUID());
        }

        public AttachmentBuilder withName(String name) {
            attachment.put("name", name);
            return this;
        }

        public AttachmentBuilder withEncodedContent(String content) {
            attachment.put("content", content);
            return this;
        }

        public Map<String, Object> getAttachment() {
            return attachment;
        }

        public JsonObject toJsonObject(){
            final JsonObject json = JsonObject.mapFrom(new HashMap<>(attachment));
            json.remove("date");
            json.put("id", json.getValue("id").toString());
            json.put("mail_id", json.getValue("mail_id").toString());
            return json;
        }

        public AttachmentBuilder fromJson(JsonObject json){
            this.attachment.putAll(json.getMap());
            if(json.containsKey("id")){
                this.attachment.put("id", UUID.fromString(json.getString("id")));
            }
            this.attachment.put("mail_id", UUID.fromString(json.getString("mail_id")));
            return this;
        }
    }
}
