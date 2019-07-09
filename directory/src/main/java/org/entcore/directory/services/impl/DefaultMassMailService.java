package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.data.FileResolver;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.http.Renders.*;

import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.services.MassMailService;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.user.DefaultFunctions.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;

public class DefaultMassMailService extends Renders implements MassMailService {
    protected static final Logger log = LoggerFactory.getLogger(DefaultMassMailService.class);
    private final Vertx vertx;
    private final EventBus eb;
    private final EmailSender notifHelper;
    private final String node;
    private final Neo4j neo = Neo4j.getInstance();

    public DefaultMassMailService(Vertx vertx, EventBus eb, EmailSender notifHelper, JsonObject config) {
        super(vertx, config);
        this.notifHelper = notifHelper;
        this.vertx = vertx;
        this.eb = eb;
        String n = (String) vertx.sharedData().getLocalMap("server").get("node");
        this.node = n == null ? "" : n;
    }

    public void massMailTypePdf(UserInfos userInfos, final HttpServerRequest request, final String templatePath, final String baseUrl, final String filename, final String type, final JsonArray users) {

        final JsonObject templateProps = new JsonObject().put("hostname", Renders.getHost(request)).put("host",Renders.getScheme(request));

        final String templateNamePrefix;
        if ("pdf".equals(type)) {
            templateNamePrefix = "massmail.pdf";
            templateProps.put("users", users);
        } else if ("newPdf".equals(type)) {
            templateNamePrefix = "massmail_new" + File.separator +"massmail_new.pdf";
            templateProps.put("users", users).put("A5",true);
        } else if ("simplePdf".equals(type)) {
            templateNamePrefix = "massmail_simple.pdf";
            List list = users.getList();
            JsonArray blocks = new JsonArray();
            for (int i = 0; i < list.size(); i += 8) {
                blocks.add(new JsonObject().put("users", new JsonArray(list.subList(i, Math.min((i + 8), list.size())))));
            }
            if (!blocks.isEmpty()) {
                blocks.getJsonObject(blocks.size() - 1).put("end", true);
            }
            templateProps.put("blocks", blocks);
        } else {
            badRequest(request);
            return;
        }
        getTemplateName(userInfos, request, templatePath, templateNamePrefix, "xhtml").setHandler(templateNameRes -> {
            if (templateNameRes.failed()) {
                badRequest(request, templateNameRes.cause().getMessage());
                return;
            }
            String templateName = templateNameRes.result();
            vertx.fileSystem().readFile(templatePath + templateName, result -> {
                if (!result.succeeded()) {
                    badRequest(request);
                    return;
                }

                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                DefaultMassMailService.this.processTemplate(request, templateProps, templateName, reader, writer -> {
                    String processedTemplate = ((StringWriter) writer).getBuffer().toString();

                    if (processedTemplate == null) {
                        badRequest(request);
                        return;
                    }

                    JsonObject actionObject = new JsonObject();
                    actionObject
                            .put("content", processedTemplate.getBytes())
                            .put("baseUrl", baseUrl);

                    eb.send(node + "entcore.pdf.generator", actionObject, new DeliveryOptions()
                            .setSendTimeout(600000l), handlerToAsyncHandler(reply -> {
                        JsonObject pdfResponse = reply.body();
                        if (!"ok".equals(pdfResponse.getString("status"))) {
                            badRequest(request, pdfResponse.getString("message"));
                            return;
                        }

                        byte[] pdf = pdfResponse.getBinary("content");
                        request.response().putHeader("Content-Type", "application/pdf");
                        request.response().putHeader("Content-Disposition",
                                "attachment; filename=" + filename + ".pdf");
                        request.response().end(Buffer.buffer(pdf));
                    }));
                });
            });
        });
    }

    public void massMailTypeMail(UserInfos userInfos, final HttpServerRequest request, final String templatePath, final JsonArray users) {
        getTemplateName(userInfos, request, templatePath, "massmail.mail", "txt").setHandler(templateNameRes -> {
            if (templateNameRes.failed()) {
                badRequest(request, templateNameRes.cause().getMessage());
                return;
            }
            String templateName = templateNameRes.result();
            vertx.fileSystem().readFile(templatePath + templateName, result -> {
                if (!result.succeeded()) {
                    badRequest(request);
                    return;
                }

                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                final JsonArray mailHeaders = new JsonArray().add(
                        new JsonObject().put("name", "Content-Type").put("value", "text/html; charset=\"UTF-8\""));

                for (Object userObj : users) {
                    final JsonObject user = (JsonObject) userObj;
                    final String userMail = user.getString("email");
                    if (userMail == null || userMail.trim().isEmpty()) {
                        continue;
                    }

                    final String mailTitle = !user.containsKey("activationCode") ||
                            user.getString("activationCode") == null ||
                            user.getString("activationCode").trim().isEmpty() ?
                            "directory.massmail.mail.subject.activated" :
                            "directory.massmail.mail.subject.not.activated";

                    try {
                        reader.reset();
                    } catch (IOException exc) {
                        log.error("[MassMail] Error on StringReader (" + exc.toString() + ")");
                    }

                    processTemplate(request, user, templateName, reader, writer -> {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();

                        if (processedTemplate == null) {
                            badRequest(request);
                            return;
                        }

                        notifHelper.sendEmail(
                                request,
                                userMail, null, null,
                                mailTitle,
                                processedTemplate, null, true, mailHeaders,
                                handlerToAsyncHandler(event -> {
                                    if ("error".equals(event.body().getString("status"))) {
                                        log.error("[MassMail] Error while sending mail (" + event.body().getString("message", "") + ")");
                                    }
                                }));
                    });
                }
                ok(request);
            });
        });

    }

    public void massMailTypeCSV(final HttpServerRequest request, JsonArray users) {
        String path = FileResolver.absolutePath("view/text/export.txt");

        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                badRequest(request);
                return;
            }
            processTemplate(request, "text/export.txt", new JsonObject().put("list", users), new Handler<String>() {
                @Override
                public void handle(final String export) {
                    if (export != null) {
                        request.response().putHeader("Content-Type", "application/csv");
                        request.response().putHeader("Content-Disposition", "attachment; filename.csv");
                        request.response().end('\ufeff' + export);
                    } else {
                        renderError(request);
                    }
                }
            });
        });


    }

    @Override
    public void massmailNoCheck(String structureId, JsonObject filterObj, boolean groupChildren, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
        this.massmailUsers(structureId, filterObj, true, groupChildren, null, false, userInfos, results);
    }

    @Override
    public void massmailUsers(String structureId, JsonObject filterObj, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
        this.massmailUsers(structureId, filterObj, true, true, null, true, userInfos, results);
    }

    @Override
    public void massmailUsers(String structureId, JsonObject filterObj, boolean groupClasses,
                              boolean groupChildren, Boolean hasMail, boolean performCheck, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {

        String filter =
                "MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), " +
                        "(g)-[:HAS_PROFILE]-(p: Profile) ";
        String condition = "";
        String optional =
                "OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
                        "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

        JsonObject params = new JsonObject().put("structureId", structureId);

        //Activation
        if (filterObj.containsKey("activated")) {
            String activated = filterObj.getString("activated", "false");
            if ("false".equals(activated.toLowerCase())) {
                condition = "WHERE NOT(u.activationCode IS NULL) ";
            } else if ("true".equals(activated.toLowerCase())) {
                condition = "WHERE (u.activationCode IS NULL) ";
            } else {
                condition = "WHERE 1 = 1 ";
            }
        } else {
            condition = "WHERE NOT(u.activationCode IS NULL) ";
        }

        //Profiles
        if (filterObj.containsKey("profiles") && filterObj.getJsonArray("profiles").size() > 0) {
            condition += "AND p.name IN {profilesArray} ";
            params.put("profilesArray", filterObj.getJsonArray("profiles"));
        }

        //Levels
        if (filterObj.containsKey("levels") && filterObj.getJsonArray("levels").size() > 0) {
            condition += " AND u.level IN {levelsArray} ";
            params.put("levelsArray", filterObj.getJsonArray("levels"));
        }

        //Classes
        if (filterObj.containsKey("classes") && filterObj.getJsonArray("classes").size() > 0) {
            filter += ", (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) ";
            optional = "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";
            condition += " AND c.id IN {classesArray} ";
            params.put("classesArray", filterObj.getJsonArray("classes"));
        }

        //Adml
        if (filterObj.containsKey("adml")) {
            String adml = filterObj.getString("adml", "false");
            if ("false".equals(adml.toLowerCase())) {
                condition += " AND NOT (u)-[:HAS_FUNCTION]->({ externalId: 'ADMIN_LOCAL' }) ";
            } else if ("true".equals(adml.toLowerCase())) {
                condition += " AND (u)-[:HAS_FUNCTION]->({ externalId: 'ADMIN_LOCAL' }) ";
            }
        }

        //Email
        if (hasMail != null) {
            if (hasMail) {
                condition += " AND COALESCE(u.email, \"\") <> \"\" ";
            } else {
                condition += " AND COALESCE(u.email, \"\") = \"\" ";
            }

        }

        //Date
        if (filterObj.containsKey("dateFilter") && filterObj.containsKey("date")) {
            String dateFilter = filterObj.getString("dateFilter", "error");
            String date = filterObj.getString("date", "error");

            if ((dateFilter.equals("before") || (dateFilter.equals("after"))) && date.matches("\\d+")) {
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date(Long.parseLong(date)));
                String operator = dateFilter.equals("before") ? "<=" : ">=";
                condition += " AND u.created " + operator + " \"" + formattedDate + "\" ";
            }
        }

        // List of users (from class-admin)
        if (filterObj.containsKey("userIds")) {
            condition += " AND u.id IN {userIds} ";
            params.put("userIds", filterObj.getJsonArray("userIds"));
        }

        //Admin check
        if (performCheck && !userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
                !userInfos.getFunctions().containsKey(ADMIN_LOCAL) &&
                !userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
            results.handle(new Either.Left<String, JsonArray>("forbidden"));
            return;
        } else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
            UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
            List<String> scope = f.getScope();
            if (scope != null && !scope.isEmpty()) {
                condition += "AND s.id IN {scope} ";
                params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
            }
        } else if (userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
            if (filterObj.getJsonArray("classes").size() < 1) {
                results.handle(new Either.Left<String, JsonArray>("forbidden"));
                return;
            }

            UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
            List<String> scope = f.getScope();
            if (scope != null && !scope.isEmpty()) {
                condition = "AND c.id IN {scope} ";
                params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
            }
        }

        //With clause
        String withStr =
                "WITH u, p ";

        //Return clause
        String returnStr =
                "RETURN distinct collect(p.name)[0] as profile, " +
                        "u.id as id, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, " +
                        "u.email as email, CASE WHEN u.loginAlias IS NOT NULL THEN u.loginAlias ELSE u.login END as login, u.login as originalLogin, u.activationCode as activationCode, u.created as creationDate ";

        if (groupClasses) {
            withStr += ", collect(distinct c.name) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
            returnStr += ", classes, classname, isInClass, CASE count(classes) WHEN 0 THEN null ELSE head(classes) END as firstClass, CASE WHEN size(classes) < 2 THEN null ELSE tail(classes) END as otherClasses ";
        } else {
            withStr += ", c.name as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
            returnStr += ", classname, isInClass ";
        }

        if (groupChildren) {
            withStr += ", CASE count(child) WHEN 0 THEN null ELSE filter(c IN (collect(distinct {firstName: child.firstName, lastName: child.lastName, classname: c.name})) WHERE not(c.firstName is null)) END as children ";
            returnStr += ", children, CASE count(children) WHEN 0 THEN null ELSE head(children) END as firstChild, CASE WHEN size(children) < 2 THEN null ELSE tail(children) END as otherChildren ";
        } else {
            if (groupClasses) {
                withStr =
                        "WITH u, p, c, " +
                                "CASE count(child) WHEN 0 THEN null " +
                                "ELSE {firstName: child.firstName, lastName: child.lastName, classname: c.name} " +
                                "END as child " + withStr + ", child ";
            } else {
                withStr += ", CASE count(child) WHEN 0 THEN null ELSE {firstName: child.firstName, lastName: child.lastName } END as child ";
            }
            returnStr += ", child ";
        }

        //Order by
        String sort = "ORDER BY ";
        if (filterObj.containsKey("sort")) {
            for (Object sortObj : filterObj.getJsonArray("sort")) {
                String sortstr = (String) sortObj;
                sort += "TOLOWER(TOSTRING(" + sortstr + ")), ";
            }
        }
        sort += "TOLOWER(lastName) ";

        String query = filter + condition + optional + withStr + returnStr + sort;

        neo.execute(query.toString(), params, validResultHandler(results));
    }

    public void massMailUser(String userId, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
        String filter =
                "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User {id: {userId}}), " +
                        "(g)-[:HAS_PROFILE]-(p: Profile) ";
        String condition = "";
        String optional =
                "OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
                        "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

        JsonObject params = new JsonObject().put("userId", userId);

        //With clause
        String withStr =
                "WITH u, p ";

        //Return clause
        String returnStr =
                "RETURN distinct collect(p.name)[0] as profile, " +
                        "u.id as id, u.firstName as firstName, u.lastName as lastName, " +
                        "u.email as email, CASE WHEN u.loginAlias IS NOT NULL THEN u.loginAlias ELSE u.login END as login, u.activationCode as activationCode ";

        withStr += ", collect(distinct c.name) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
        returnStr += ", classes, classname, isInClass ";

        withStr += ", CASE count(child) WHEN 0 THEN null ELSE {firstName: child.firstName, lastName: child.lastName } END as child ";
        returnStr += ", child ";

        String query = filter + condition + optional + withStr + returnStr;

        neo.execute(query.toString(), params, validResultHandler(results));
    }

    @Override
    public void massMailAllUsersByStructure(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
        String filter =
                "MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), " +
                        "(g)-[:HAS_PROFILE]-(p: Profile) ";
        String condition = "";
        String optional =
                "OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
                        "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) " +
                        "OPTIONAL MATCH (u:User)-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) ";

        JsonObject params = new JsonObject().put("structureId", structureId);

        //Admin check
        if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
                !userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
            results.handle(new Either.Left<String, JsonArray>("forbidden"));
            return;
        } else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
            UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
            List<String> scope = f.getScope();
            if (scope != null && !scope.isEmpty()) {
                condition += "WHERE s.id IN {scope} ";
                params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
            }
        }

        //With clause
        String withStr =
                "WITH u, p, rf, f ";

        //Return clause
        String returnStr =
                "RETURN distinct collect(p.name)[0] as type, " +
                        "u.id as id, u.firstName as firstName, u.lastName as lastName, " +
                        "u.email as email, CASE WHEN u.loginAlias IS NOT NULL THEN u.loginAlias ELSE u.login END as login, u.activationCode as code, u.created as creationDate, " +
                        "COLLECT(distinct [f.externalId, rf.scope]) as functions ";

        withStr += ", collect(distinct {id: c.id, name: c.name}) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
        returnStr += ", classes, classname, isInClass ";

        withStr += ", CASE count(child) WHEN 0 THEN null ELSE collect(distinct {firstName: child.firstName, lastName: child.lastName, classname: c.name}) END as children ";
        returnStr += ", filter(c IN children WHERE not(c.firstName is null)) as children ";

        String sort = "ORDER BY lastName";

        String query = filter + condition + optional + withStr + returnStr + sort;

        neo.execute(query.toString(), params, validResultHandler(results));
    }

    private Future<String> getTemplateName(UserInfos user, HttpServerRequest request, String basePath, String prefix, String suffix) {
        Future<String> defaultName = getDefaultFileName(prefix, suffix);
        Future<String> i18Name = getI18FileName(user, request, prefix, suffix);
        return CompositeFuture.all(defaultName, i18Name).compose(all -> {
            Future<String> fullPath = Future.future();
            String i18Path = basePath + i18Name.result();
            String defaultPath = basePath + defaultName.result();
            vertx.fileSystem().exists(i18Path, exists -> {
                if (exists.succeeded() && exists.result()) {
                    fullPath.complete(i18Name.result());
                } else {
                    fullPath.complete(defaultName.result());
                }
            });
            return fullPath;
        });
    }

    private Future<String> getDefaultFileName(String prefix, String suffix) {
        return Future.succeededFuture(String.format("%s.%s", prefix, suffix));
    }

    private Future<String> getI18FileName(UserInfos user, HttpServerRequest request, String prefix, String suffix) {
        return getLanguage(user, request).map(lang -> {
            return String.format("%s.%s.%s", prefix, lang, suffix);
        });
    }

    private Future<String> getLanguage(UserInfos user, HttpServerRequest request) {
        Future<String> future = Future.future();
        String navLang = getOrElse(I18n.acceptLanguage(request), "fr");
        future.complete(navLang);
        return future;
    }

}
