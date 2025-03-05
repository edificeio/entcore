package org.entcore.common.email.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.email.Bounce;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.eventbus.ResultMessage;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncError;
import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncResult;

public class PostgresEmailSender implements EmailSender {
    protected static final Logger logger = LoggerFactory.getLogger(PostgresEmailSender.class);
    private final EmailSender oldMailSender;
    private final Renders renders;
    private final EventBus eventBus;
    private final int maxSize;
    private final String platformId;
    private final PostgresEmailHelper helper;
    private final int priority;
    private final String senderEmail;
    private final String host;

    public PostgresEmailSender(EmailSender aMailSender, Vertx vertx, JsonObject moduleConfig, JsonObject emailConfig, int priority) {
        final JsonObject pgConfig = emailConfig.getJsonObject("postgresql");
        this.priority = priority;
        this.oldMailSender = aMailSender;
        this.eventBus = vertx.eventBus();
        this.renders = new Renders(vertx, moduleConfig);
        maxSize = pgConfig.getInteger("max-size", -1);
        this.helper = PostgresEmailHelper.create(vertx, pgConfig);
        final String eventStoreConf = (String) vertx.sharedData().getLocalMap("server").get("event-store");
        if (eventStoreConf != null) {
            final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
            platformId = eventStoreConfig.getString("platform");
        } else {
            platformId = null;
        }
        //
        final String defaultMail = emailConfig.getString("email", "noreply@one1d.fr");
        final String defaultHost = emailConfig.getString("host", "http://localhost:8009");
        if(moduleConfig != null){
            this.senderEmail = moduleConfig.getString("email", defaultMail);
            this.host = moduleConfig.getString("host", defaultHost);
        }else{
            this.senderEmail = defaultMail;
            this.host = defaultHost;
        }
    }

    @Override
    public void hardBounces(Date date, Handler<Either<String, List<Bounce>>> handler) {
        //TODO migrate hard bounce to pg mailer?
        oldMailSender.hardBounces(date, handler);
    }

    @Override
    public void hardBounces(Date date, Date date1, Handler<Either<String, List<Bounce>>> handler) {
        //TODO migrate hard bounce to pg mailer?
        oldMailSender.hardBounces(date, date1, handler);
    }

    @Override
    public String getSenderEmail() {
        return senderEmail;
    }

    @Override
    public String getHost(HttpServerRequest request) {
        if (request == null) {
            return host;
        }
        return Renders.getScheme(request) + "://" + Renders.getHost(request);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, getSenderEmail(), cc, bcc, subject, templateBody,
                templateParams, translateSubject, null, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
                          String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
                          boolean translateSubject, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, getSenderEmail(), cc, bcc, subject, attachments, templateBody,
                templateParams, translateSubject, null, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, JsonArray headers, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, getSenderEmail(), cc, bcc, subject, templateBody,
                templateParams, translateSubject, headers, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, from, cc, bcc, subject, templateBody,
                templateParams, translateSubject, null, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, JsonArray headers, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, getSenderEmail(), cc, bcc, subject, null, templateBody,
                templateParams, translateSubject, headers, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
                          String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
                          boolean translateSubject, JsonArray headers, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        List<Object> toList = null;
        List<Object> ccList = null;
        List<Object> bccList = null;

        if (to != null) {
            toList = new ArrayList<>();
            toList.add(to);
        }
        if (cc != null) {
            ccList = new ArrayList<>();
            ccList.add(cc);
        }
        if (bcc != null) {
            bccList = new ArrayList<>();
            bccList.add(bcc);
        }

        sendEmail(request, toList, getSenderEmail(), ccList, bccList, subject, attachments, templateBody,
                templateParams, translateSubject, headers, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, List<Object> to, List<Object> cc, List<Object> bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, getSenderEmail(), cc, bcc, subject, templateBody,
                templateParams, translateSubject, null, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, List<Object> to, String from, List<Object> cc, List<Object> bcc,
                          String subject, String templateBody, JsonObject templateParams,
                          boolean translateSubject, JsonArray headers, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        sendEmail(request, to, from, cc, bcc, subject, null, templateBody,
                templateParams, translateSubject, headers, handler);
    }

    @Override
    public void sendEmail(HttpServerRequest request, List<Object> to, String from, List<Object> cc, List<Object> bcc,
                          String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
                          boolean translateSubject, JsonArray headers, final Handler<AsyncResult<Message<JsonObject>>> handler) {
        try {
            final PostgresEmailBuilder.EmailBuilder mail = PostgresEmailBuilder.mail();
            for (final Object t : to) {
                mail.withTo(t.toString());
            }
            mail.withFrom(from);
            if (cc != null) {
                for (final Object c : cc) {
                    mail.withCc(c.toString());
                }
            }
            if (bcc != null) {
                for (final Object c : bcc) {
                    mail.withBcc(c.toString());
                }
            }

            if (translateSubject) {
                mail.withSubject(I18n.getInstance().translate(
                        subject, getHost(request), I18n.acceptLanguage(request)));
            } else {
                mail.withSubject(subject);
            }

            if (headers != null) {
                for (final Object o : headers) {
                    if (o instanceof JsonObject) {
                        mail.withHeader((JsonObject) o);
                    }
                }
            }

            final Handler<String> mailHandler = body -> {
                if (body != null) {
                    mail.withBody(body);
                    //attachments
                    final List<PostgresEmailBuilder.AttachmentBuilder> attList = new ArrayList<>();
                    int mailSize = body.getBytes().length;
                    if (attachments != null) {
                        for (Object o : attachments) {
                            if (!(o instanceof JsonObject)) continue;
                            JsonObject att = (JsonObject) o;
                            if (att.getString("name") == null || att.getString("content") == null) continue;
                            mailSize += att.getString("content").getBytes().length;
                            if (maxSize > 0 && mailSize > maxSize) {
                                mailSize -= att.getString("content").getBytes().length;
                                logger.warn("Mail too big, can't attach " + att.getString("name"));
                            } else {
                                attList.add(PostgresEmailBuilder.attachment(mail).withName(att.getString("name")).withEncodedContent(att.getString("content")));
                            }
                        }
                    }
                    addMeta(request, mail).compose(r -> {
                        return helper.createWithAttachments(mail, attList);
                    }).onComplete(r -> {
                        if (r.failed()) {
                            logger.error("Failed to save mail: ", r.cause());
                            if(handler != null){
                                handleAsyncError("Message is null.", handler);
                            }
                        } else {
                            if(handler != null) {
                                handleAsyncResult(new ResultMessage(), handler);
                            }
                        }
                    });
                } else {
                    logger.error("Message is null.");
                    handleAsyncError("Message is null.", handler);
                }
            };

            if (templateParams != null) {
                renders.processTemplate(request, templateBody, templateParams, mailHandler);
            } else {
                mailHandler.handle(templateBody);
            }
        }catch(Exception e){
            logger.error("Failed to send email ", e);
            if(handler != null){
                handleAsyncError(e.getMessage(), handler);
            }
        }
    }

    private Future<PostgresEmailBuilder.EmailBuilder> addMeta(HttpServerRequest request, PostgresEmailBuilder.EmailBuilder mail) {
        final Promise<PostgresEmailBuilder.EmailBuilder> future = Promise.promise();
        mail.withPriority(priority);
        final String moduleName = BaseServer.getModuleName();
        if (Utils.isNotEmpty(moduleName)) {
            mail.withModule(moduleName);
        }
        if (Utils.isNotEmpty(platformId)) {
            mail.withPlatformId(platformId);
        }
        mail.withPlatformUrl(getHost(request));
        if(request != null){
            UserUtils.getUserInfos(eventBus, request, user -> {
                if (user != null) {
                    if (Utils.isNotEmpty(user.getUserId())) {
                        mail.withUserId(user.getUserId());
                    }
                    if (Utils.isNotEmpty(user.getType())) {
                        mail.withProfile(user.getType());
                    }
                }
                future.complete(mail);
            });
        }else{
            future.complete(mail);
        }
        return future.future();
    }
}
