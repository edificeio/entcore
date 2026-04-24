package org.entcore.timeline.services.impl;

import com.google.common.collect.Lists;
import com.samskivert.mustache.Mustache;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.collections.SharedDataHelper;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.template.FileTemplateProcessor;
import fr.wseduc.webutils.template.lambdas.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.email.impl.PostgresEmailDto;
import org.entcore.common.email.impl.PostgresEmailSender;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.utils.StopWatch;
import org.entcore.common.utils.StringUtils;
import org.entcore.timeline.controllers.TimelineLambda;
import org.entcore.timeline.services.CronMailerService;
import org.entcore.timeline.services.TimelineConfigService;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.utils.DateUtils.formatUtcDateTime;

public class PeriodicTimelineMailerService implements CronMailerService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTimelineMailerService.class);
    private final EventBus eb;
    private Map<String, String> registeredNotifications;
    private TimelineConfigService configService;
    private Map<String, String> eventsI18n;
    private HashMap<String, JsonObject> lazyEventsI18n;
    private final PostgresEmailSender emailSender;
    private final int USERS_LIMIT;
    private final long QUERY_TIMEOUT;
    private final MongoDb mongo = MongoDb.getInstance();
    private final Neo4j neo4j = Neo4j.getInstance();
    private FileTemplateProcessor templateProcessor;
    private JsonObject config;
    private String pathPrefix;
    private String staticHost;
    private final Map<String, Mustache.Lambda> staticLambdas = new HashMap<>();

    public PeriodicTimelineMailerService(Vertx vertx, JsonObject config) {
        eb = Server.getEventBus(vertx);
        EmailFactory emailFactory = EmailFactory.getInstance();
        emailSender = (PostgresEmailSender) emailFactory.getSenderWithPriority(EmailFactory.PRIORITY_VERY_LOW);
        USERS_LIMIT = config.getInteger("users-loop-limit", 200);
        QUERY_TIMEOUT = config.getLong("query-timeout", 300000L);
        init(vertx, config);
    }

    protected void init(Vertx vertx, JsonObject config) {
        this.config = config;
        this.pathPrefix = Server.getPathPrefix(config);

        this.templateProcessor = new FileTemplateProcessor(vertx, "view/", true);
        this.templateProcessor.escapeHTML(true);

        SharedDataHelper.getInstance().getLocal("server", "static-host").onSuccess((staticHost) -> this.staticHost = (String) staticHost).onFailure((ex) -> log.error("Error getting static-host conf", ex));

        this.staticLambdas.put("formatBirthDate", new FormatBirthDateLambda());
        this.staticLambdas.put("modVersion", new ModsLambda(vertx));
    }

    public Map<String, Mustache.Lambda> getLambdas(HttpServerRequest request) {
        Map<String, Mustache.Lambda> lambdas = new HashMap<>(staticLambdas);
        String host = Renders.getHost(request);
        if (host == null) {
            host = "";
        }

        String sttcHost = this.staticHost != null ? this.staticHost : host;
        lambdas.put("static", new StaticLambda(this.config.getBoolean("ssl", sttcHost.startsWith("https")), sttcHost, this.pathPrefix + "/public"));
        lambdas.put("infra", new InfraLambda(this.config.getBoolean("ssl", sttcHost.startsWith("https")), sttcHost, "/infra/public", request.headers().get("X-Forwarded-For") == null));
        lambdas.put("datetime", new LocaleDateLambda(I18n.acceptLanguage(request)));
        lambdas.putAll(TimelineLambda.getLambdas(request, eventsI18n, lazyEventsI18n));
        return lambdas;
    }

    public void setConfigService(TimelineConfigService configService) {
        this.configService = configService;
    }

    public void setRegisteredNotifications(Map<String, String> registeredNotifications) {
        this.registeredNotifications = registeredNotifications;
    }

    public void setEventsI18n(Map<String, String> eventsI18n) {
        this.eventsI18n = eventsI18n;
    }

    public void setLazyEventsI18n(HashMap<String, JsonObject> lazyEventsI18n) {
        this.lazyEventsI18n = lazyEventsI18n;
    }

    public void translateTimeline(JsonArray i18nKeys,HttpServerRequest req, String language, Handler<JsonArray> handler) {
        JsonArray translations = new JsonArray();
        for(Object keyObj : i18nKeys){
            String key = (String) keyObj;
            translations.add(TimelineLambda.translate(language, key, req, eventsI18n, lazyEventsI18n));
        }
        handler.handle(translations);
    }

    public Future<JsonArray> translateTimeline(JsonArray i18nKeys, HttpServerRequest req, String language) {
        Promise<JsonArray> promise = Promise.promise();
        translateTimeline(i18nKeys, req, language, promise::complete);
        return promise.future();
    }


    @Override
    public Future<JsonObject> sendDailyMails(Date date, int dayDelta) {
        return sendDailyMails(Optional.ofNullable(date), dayDelta);
    }

    @Override
    public Future<JsonObject> sendDailyMails(int dayDelta) {
        return sendDailyMails(Optional.empty(), dayDelta);
    }

    @Override
    public Future<JsonObject> sendWeeklyMails(Date date, int dayDelta) {
        return sendWeeklyMails(Optional.ofNullable(date), dayDelta);
    }

    @Override
    public Future<JsonObject> sendWeeklyMails(int dayDelta) {
        return sendWeeklyMails(Optional.empty(), dayDelta);
    }

    protected Future<JsonObject> sendWeeklyMails(Optional<Date> forDate, int dayDelta) {
        final AtomicInteger endPage = new AtomicInteger(0);
        final Calendar weekDate = Calendar.getInstance();
        forDate.ifPresent(weekDate::setTime);
        weekDate.add(Calendar.DAY_OF_MONTH, dayDelta - 6);
        weekDate.set(Calendar.HOUR_OF_DAY, 0);
        weekDate.set(Calendar.MINUTE, 0);
        weekDate.set(Calendar.SECOND, 0);
        weekDate.set(Calendar.MILLISECOND, 0);
        //
        final Calendar weekEndDate = Calendar.getInstance();
        forDate.ifPresent(weekEndDate::setTime);
        weekEndDate.add(Calendar.DAY_OF_MONTH, dayDelta + 1);
        weekEndDate.set(Calendar.HOUR_OF_DAY, 0);
        weekEndDate.set(Calendar.MINUTE, 0);
        weekEndDate.set(Calendar.SECOND, 0);
        weekEndDate.set(Calendar.MILLISECOND, 0);

        final JsonObject results = new JsonObject()
                .put("mails.sent", new AtomicInteger(0))
                .put("users.ko", 0);
        final JsonObject notificationsDefaults = new JsonObject();
        final List<String> notifiedUsers = new ArrayList<>();

        StopWatch step1 = new StopWatch();
        return getRecipientsUsers(weekDate.getTime(), weekEndDate.getTime())
                .compose(event -> {
            log.info("[WeeklyMails][perf] getRecipientUsersTiming " + step1.elapsedTimeMillis() + " ms");
            if (event != null && event.size() > 0) {
                notifiedUsers.addAll(event.getList());
                endPage.set((event.size() / USERS_LIMIT) + (event.size() % USERS_LIMIT != 0 ? 1 : 0));
                results.put("users.recipients", notifiedUsers.size());
                results.put("users.pages", endPage.get());
            } else {
                results.put("users.recipients", 0);
                results.put("users.pages", 0);
                return Future.succeededFuture();
            }
            StopWatch step2 = new StopWatch();
            return getNotificationsDefaults().compose( notifications -> {
                log.info("[WeeklyMails][perf] getNotifications " + step2.elapsedTimeMillis() + " ms");
                if (notifications == null) {
                    log.error("[sendWeeklyMails] Error while retrieving notifications defaults.");
                } else {
                    for (Object notifObj : notifications) {
                        final JsonObject notif = (JsonObject) notifObj;
                        notificationsDefaults.put(notif.getString("key", ""), notif);
                    }
                }
                List<List<String>> usersPartitioned = Lists.partition(notifiedUsers, USERS_LIMIT);
                return processPages(usersPartitioned, 0, results, weekDate.getTime(), weekEndDate.getTime(), notificationsDefaults, Periodicity.WEEKLY);
            });
        }).map(v -> results);
    }


    protected Future<JsonObject> sendDailyMails(Optional<Date> forDate, int dayDelta) {
        final AtomicInteger endPage = new AtomicInteger(0);
        Calendar fromDate = Calendar.getInstance();
        forDate.ifPresent(fromDate::setTime);
        fromDate.add(Calendar.DAY_OF_MONTH, dayDelta);
        fromDate.set(Calendar.HOUR_OF_DAY, 0);
        fromDate.set(Calendar.MINUTE, 0);
        fromDate.set(Calendar.SECOND, 0);
        fromDate.set(Calendar.MILLISECOND, 0);
        //
        final Calendar toDate = Calendar.getInstance();
        forDate.ifPresent(toDate::setTime);
        toDate.add(Calendar.DAY_OF_MONTH, dayDelta + 1);
        toDate.set(Calendar.HOUR_OF_DAY, 0);
        toDate.set(Calendar.MINUTE, 0);
        toDate.set(Calendar.SECOND, 0);
        toDate.set(Calendar.MILLISECOND, 0);

        final JsonObject results = new JsonObject()
                .put("mails.sent", new AtomicInteger(0))
                .put("users.ko", new AtomicInteger(0));
        final JsonObject notificationsDefaults = new JsonObject();
        final List<String> notifiedUsers = new ArrayList<>();

        StopWatch step1 = new StopWatch();
        return getRecipientsUsers(fromDate.getTime(), toDate.getTime())
                .compose(event -> {
                    log.info("[DailyMails][perf] getRecipientUsersTiming " + step1.elapsedTimeMillis() + " ms");
                    if (event != null && !event.isEmpty()) {
                        notifiedUsers.addAll(event.getList());
                        endPage.set((event.size() / USERS_LIMIT) + (event.size() % USERS_LIMIT != 0 ? 1 : 0));
                        results.put("users.recipients", notifiedUsers.size());
                        results.put("users.pages", endPage.get());
                    } else {
                        results.put("users.recipients", 0);
                        results.put("users.pages", 0);
                        return Future.succeededFuture();
                    }
                    StopWatch step2 = new StopWatch();
                    return getNotificationsDefaults().compose( notifications -> {
                        log.info("[DailyMails][perf] getNotifications " + step2.elapsedTimeMillis() + " ms");
                        if (notifications == null) {
                            log.error("[sendDailyMails] Error while retrieving notifications defaults.");
                        } else {
                            for (Object notifObj : notifications) {
                                final JsonObject notif = (JsonObject) notifObj;
                                notificationsDefaults.put(notif.getString("key", ""), notif);
                            }
                        }
                        List<List<String>> usersPartitioned = Lists.partition(notifiedUsers, USERS_LIMIT);
                        return processPages(usersPartitioned, 0, results, fromDate.getTime(), toDate.getTime(), notificationsDefaults, Periodicity.DAILY);
                    });
                }).map(v -> results);
    }

    private Future<Void> processPages(List<List<String>> pages, int index, JsonObject results, Date from, Date to,
                                      JsonObject notificationsDefaults, Periodicity periodicity) {
        if (index >= pages.size()) {
            return Future.succeededFuture();
        }

        List<String> users = pages.get(index);
        int currentPage = index + 1;

        log.info("[PeriodicMailer] Page : " + currentPage + "/" + pages.size());

        if(periodicity == Periodicity.WEEKLY) {
            return assemblePipeline(currentPage, users, from, to, notificationsDefaults)
                    .onFailure(t -> log.error("[PeriodicMailer] Error during mail pipeline ", t))
                    .compose(result -> {
                        ((AtomicInteger) results.getValue("mails.sent")).addAndGet(result.getInteger("mails.sent"));
                        ((AtomicInteger) results.getValue("users.ko")).addAndGet(result.getInteger("users.ko"));
                        return processPages(pages, index + 1, results, from, to, notificationsDefaults, periodicity);
                    });
        }
        return assembleDailyPipeline(currentPage, users, from, to, notificationsDefaults)
                .onFailure(t -> log.error("[PeriodicMailer] Error during mail pipeline ", t))
                .compose(result -> {
                    ((AtomicInteger) results.getValue("mails.sent")).addAndGet(result.getInteger("mails.sent", 0));
                    ((AtomicInteger) results.getValue("users.ko")).addAndGet(result.getInteger("users.ko",  0));
                    return processPages(pages, index + 1, results, from, to, notificationsDefaults, periodicity);
                });
    }

    private Future<JsonObject> assemblePipeline(int page, List<String> users, Date from, Date to, JsonObject notificationsDefaults) {
        return Future.succeededFuture().compose( h -> getAuthorizedUsers( users, page)
                .compose( filteredUser -> getUserPreferences(filteredUser, page)))
                .compose( preferences -> getAggregatedUserNotifications(preferences, from, to, page))
                .compose( notificationContext -> applyRuleToNotification(notificationContext, notificationsDefaults, page))
                .compose( notificationContext -> processTimelineTemplate(notificationContext, page))
                .compose( notificationContext -> sendMassMail(notificationContext, page));
    }

    private Future<JsonObject> assembleDailyPipeline(int page, List<String> users, Date from, Date to, JsonObject notificationsDefaults) {
        return Future.succeededFuture().compose( h -> getAuthorizedUsers( users, page)
                .compose( filteredUser -> getUserPreferences(filteredUser, page)))
                .compose( preferences -> getUsersNotifications(preferences, from, to, page))
                .compose( notificationContext -> applyDailyRuleToNotification(notificationContext, notificationsDefaults, page))
                .compose( notificationContext -> processDailyTimelineTemplate(notificationContext, page))
                .compose( notificationContext -> sendMassMail(notificationContext, page));
    }

    private Future<NotificationContext> processTimelineTemplate(NotificationContext notificationContext, int page) {
        List<Future<?>> futures = new ArrayList<>();

        StopWatch step6 = new StopWatch();
        log.info("Notifications list " + page + " size " + notificationContext.usersNotifications.size());
        for (Map.Entry<String, JsonArray> notificationEntry : notificationContext.usersNotifications.entrySet()) {
            final JsonObject userPrefs = notificationContext.preferences.stream().map(JsonObject.class::cast)
                    .filter( p -> notificationEntry.getKey().equals(p.getString("userId")))
                    .findFirst()
                    .orElse(null);

            if (userPrefs == null || notificationEntry.getValue().isEmpty()) {
                continue;
            }
            final String userId = userPrefs.getString("userId");
            final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
            final String userScheme = userPrefs.getString("lastScheme", "http");
            String mutableUserLanguage = "fr";
            try {
                mutableUserLanguage = getOrElse(new JsonObject(getOrElse(userPrefs.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
            } catch (Exception e) {
                log.error("UserId [" + userId + "] - Bad language preferences format");
            }
            final String userLanguage = mutableUserLanguage;
            final String userDisplayName = getOrElse(userPrefs.getString("displayName"), "", true);

            final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject()
                    .put("headers", new JsonObject()
                            .put("Host", userDomain)
                            .put("X-Forwarded-Proto", userScheme)
                            .put("Accept-Language", userLanguage)));

            JsonObject templateParams = new JsonObject().put("notifications", notificationEntry.getValue());
            templateParams.put("displayName", userDisplayName);
            futures.add(processTimelineTemplate(templateParams,  "notifications/weekly-mail.html", request)
                .onSuccess( template -> {
                        if( template != null) {
                            notificationContext.templates.put(userId, template);
                        }
                    }
                ));

            JsonArray keys = new JsonArray()
                    .add("timeline.weekly.mail.subject.header");
            futures.add(translateTimeline(keys, request, userLanguage)
                    .onSuccess( translations -> { notificationContext.subjects.put(userId, translations.getString(0)); }));
        }

        return Future.join(futures).map( v -> {
            log.info("[WeeklyMails][perf] process template page " + page + " time " + step6.elapsedTimeMillis() + " ms");
            return notificationContext;
        });
    }


    private Future<NotificationContext> processDailyTimelineTemplate(NotificationContext notificationContext, int page) {
        List<Future<?>> futures = new ArrayList<>();

        StopWatch step6 = new StopWatch();
        log.info("Notifications list " + page + " size " + notificationContext.usersNotifications.size());
        for (Map.Entry<String, JsonArray> notificationEntry : notificationContext.usersNotifications.entrySet()) {
            final JsonObject userPrefs = notificationContext.preferences.stream().map(JsonObject.class::cast)
                    .filter( p -> notificationEntry.getKey().equals(p.getString("userId")))
                    .findFirst()
                    .orElse(null);

            if (userPrefs == null || notificationEntry.getValue().isEmpty()) {
                continue;
            }
            final String userId = userPrefs.getString("userId");
            final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
            final String userScheme = userPrefs.getString("lastScheme", "http");
            String mutableUserLanguage = "fr";
            try {
                mutableUserLanguage = getOrElse(new JsonObject(getOrElse(userPrefs.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
            } catch (Exception e) {
                log.error("UserId [" + userId + "] - Bad language preferences format");
            }
            final String userLanguage = mutableUserLanguage;
            final String userDisplayName = getOrElse(userPrefs.getString("displayName"), "", true);
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.forLanguageTag(userLanguage));

            final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject()
                    .put("headers", new JsonObject()
                            .put("Host", userDomain)
                            .put("X-Forwarded-Proto", userScheme)
                            .put("Accept-Language", userLanguage)));

            JsonObject templateParams = new JsonObject().put("nestedTemplatesArray", notificationEntry.getValue())
                                                        .put("notificationDates", notificationEntry.getValue().stream()
                                                                .map(JsonObject.class::cast)
                                                                .map( notification -> formatter.format(MongoDb.parseIsoDate(notification.getJsonObject("date"))))
                                                                .collect(Collectors.toList()))
                                                        .put("displayName", userDisplayName);

            futures.add(processTimelineTemplate(templateParams,  "notifications/daily-mail.html", request)
                    .onSuccess( template -> {
                            if(template != null) {
                                notificationContext.templates.put(userId, template);
                            }
                        }
                    ));

            JsonArray keys = new JsonArray()
                    .add("timeline.daily.mail.subject.header");
            futures.add(translateTimeline(keys, request, userLanguage)
                    .onSuccess( translations -> { notificationContext.subjects.put(userId, translations.getString(0));}));
        }

        return Future.join(futures).map( v -> {
            log.info("[DailyMails][perf] process template page " + page + " time " + step6.elapsedTimeMillis() + " ms");
            return notificationContext;
        });
    }

    public Future<JsonObject> sendMassMail(NotificationContext notificationContext, int page) {
        Promise<JsonObject> promise = Promise.promise();
        List<PostgresEmailDto> mails = new ArrayList<>();

        for (Map.Entry<String, String> template : notificationContext.templates.entrySet()) {
            PostgresEmailDto mail = new PostgresEmailDto();
            final JsonObject userPrefs = notificationContext.preferences.stream().map(JsonObject.class::cast)
                    .filter( p -> template.getKey().equals(p.getString("userId")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("UserId [" + template.getKey() + "] not found"));
            mail.setId(UUID.randomUUID())
                    .setBody(template.getValue())
                    .setSubject(notificationContext.subjects.get(template.getKey()))
                    .setTo(Lists.newArrayList(new PostgresEmailDto.User(userPrefs.getString("userMail"), null)));
            mails.add(mail);
        }
        StopWatch step7 = new StopWatch();
        emailSender.sendEmails(mails)
                .onFailure(t -> { log.error(t.getMessage());})
                .onSuccess(v -> {
                    log.info("[PeriodicMails][perf] sendMails page " + page + ", sented : " + v.getSuccess().get() + " time " + step7.elapsedTimeMillis() + " ms");
                    promise.complete(new JsonObject().put("mails.sent", v.getSuccess().get()).put("users.ko", v.getFailure().get()));
                });

        return promise.future();
    }

    public Future<String> processTimelineTemplate(JsonObject parameters,
                                        String template, HttpServerRequest request) {
        return processTemplate(request, parameters, template);
    }

    public Future<String> processTemplate(HttpServerRequest request, JsonObject p, String template) {
        Promise<String> promise = Promise.promise();
        Map<String, Mustache.Lambda> lambdas = getLambdas(request);
        this.templateProcessor.processTemplate(template, p,  lambdas, promise::complete);
        return promise.future();
    }


    public Future<JsonArray> getAuthorizedUsers(List<String> usersIds, int pagination) {
        Promise<JsonArray> promise = Promise.promise();
        StopWatch step3 = new StopWatch();

        final String query =
                "MATCH (u:User)-[:IN]->(g:Group)-[:AUTHORIZED]->(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) " +
                        "WHERE u.id IN {notifiedUsers} AND u.activationCode IS NULL AND u.email IS NOT NULL AND length(u.email) > 0 " +
                        "AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\"" +
                        "RETURN DISTINCT u.email as mail, u.id as id ";
        JsonObject params = new JsonObject()
                .put("notifiedUsers", new JsonArray(usersIds));
        neo4j.execute(query, params, Neo4jResult.validResultHandler( event -> {
            log.info("[PeriodicMails][perf] getImpactedUsers page " + pagination + " time " + step3.elapsedTimeMillis() + " ms");
            if (event.isLeft()) {
               log.error("[PeriodicMails] Error while retrieving impacted users : " + event.left().getValue());
               promise.complete( new JsonArray());
            } else {
                JsonArray users = event.right().getValue();
                promise.complete(users);
            }
        } ));

        return promise.future();
    }

    public Future<JsonArray> getUserPreferences(JsonArray users, int pagination) {
        Promise<JsonArray> promise = Promise.promise();
        if (users.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }
        final JsonArray userIds = new JsonArray();
        for (Object userObj : users)
            userIds.add(((JsonObject) userObj).getString("id", ""));
        StopWatch step4 = new StopWatch();
        NotificationUtils.getUsersPreferences(eb, userIds, "language: uac.language, displayName: u.displayName", h -> {
            log.info("[PeriodicMails][perf] getUsersPreferences page " + pagination + " time " + step4.elapsedTimeMillis() + " ms");
            promise.complete(h);
        });
        return promise.future();
    }


    public Future<JsonArray> getNotificationsDefaults() {
        Promise<JsonArray> promise = Promise.promise();
        configService.list(event -> {
            if (event.isLeft()) {
                promise.complete(null);
            } else {
                JsonArray config = event.right().getValue();

                JsonArray notificationsList = new JsonArray();
                for (String key : registeredNotifications.keySet()) {
                    JsonObject notif = new JsonObject(registeredNotifications.get(key));
                    notif.put("key", key);
                    for (Object notifConfigObj : config) {
                        JsonObject notifConfig = (JsonObject) notifConfigObj;
                        if (notifConfig.getString("key", "").equals(key)) {
                            notif.put("defaultFrequency",
                                    notifConfig.getString("defaultFrequency", notif.getString("defaultFrequency")));
                            notif.put("restriction",
                                    notifConfig.getString("restriction", notif.getString("restriction")));
                            break;
                        }
                    }
                    notificationsList.add(notif);
                }
               promise.complete(notificationsList);
            }
        });
        return promise.future();
    }

    /**
     * Returns either user preferences or defaults when the user has not chosen specific values.
     *
     * @param field : Which preference
     * @param userPrefs : User preferences
     * @param defaultPrefs : Default preferences
     * @return The prevailing preference
     */
    private String notificationPrefsMixin(String field, JsonObject userPrefs, JsonObject defaultPrefs){
        return userPrefs.getString(field, defaultPrefs.getString(field, ""));
    }

    private Future<JsonArray> getRecipientsUsers(Date from, Date to) {
        Promise<JsonArray> promise = Promise.promise();

        final JsonObject aggregation = new JsonObject();
        JsonArray pipeline = new JsonArray();
        aggregation
                .put("aggregate", "timeline")
                .put("allowDiskUse", true)
                .put("pipeline", pipeline)
                .put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));
        JsonObject matcher = new JsonObject().put("$and", new JsonArray()
                .add(new JsonObject().put("date", new JsonObject().put("$gte", new JsonObject().put("$date", formatUtcDateTime(from)))))
                .add(new JsonObject().put("date", new JsonObject().put("$lt", new JsonObject().put("$date", formatUtcDateTime(to)))))
        );
        pipeline.add(new JsonObject().put("$match", matcher));
        pipeline.add(new JsonObject().put("$unwind", "$recipients"));
        pipeline.add(new JsonObject().put("$group", new JsonObject().put("_id", "$recipients.userId")));
        //
        mongo.command(aggregation.encode(), new DeliveryOptions().setSendTimeout(QUERY_TIMEOUT), event -> {
            if ("error".equals(event.body().getString("status", "error"))) {
                log.error("getRecipientsUsers failed: "+ event.body().encode());
                promise.fail(event.body().encode());
            } else {
                JsonArray r = event.body().getJsonObject("result", new JsonObject())
                        .getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch");
                if (r != null && r.size() > 0) {
                    final List<String> userIds = r.stream().map(e -> ((JsonObject)e).getString("_id")).filter(e -> !StringUtils.isEmpty(e)).collect(Collectors.toList());
                   promise.complete(new JsonArray(userIds));
                } else {
                   promise.complete(new JsonArray());
                }
            }
        });

        return promise.future();
    }

     /**
     * Retrieves an aggregated list of notifications from mongodb for users.
     *
     *  Notifications are grouped by type & event-type.
     *
     * @param userPreferences : List of Userid to retreive
     * @param from : Starting date in the past
     * @param to : End date used to filter notifications
     */
    private Future<NotificationContext> getAggregatedUserNotifications(JsonArray userPreferences, Date from, Date to, int page){
        Promise<NotificationContext> promise = Promise.promise();
        if(userPreferences.isEmpty()){
            promise.complete(NotificationContext.empty());
            return promise.future();
        }
        final JsonArray userIds = new JsonArray();
        userPreferences.forEach( o -> {
            JsonObject pref = (JsonObject) o;
            userIds.add(pref.getString("userId"));
        });

        final JsonObject aggregation = new JsonObject();
        JsonArray pipeline = new JsonArray();
        aggregation
                .put("aggregate", "timeline")
                .put("allowDiskUse", true)
                .put("pipeline", pipeline)
                .put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));

        final JsonObject matcher = new JsonObject()
                .put("$and", new JsonArray()
                        .add(new JsonObject().put("recipients.userId", new JsonObject()
                                .put("$in", userIds)))
                        .add(new JsonObject().put("date", new JsonObject().put("$gte", new JsonObject().put("$date", formatUtcDateTime(from)))))
                        .add(new JsonObject().put("date", new JsonObject().put("$lt", new JsonObject().put("$date", formatUtcDateTime(to))))));

        JsonObject transformer = new JsonObject("{ \"type\": 1, \"event-type\": 1, \"recipients\": { \"$filter\": { \"input\": \"$recipients\", \"as\": \"r\", \"cond\" : { \"$in\" : " +
                "  [\"$$r.userId\", " + userIds.encode() + "]} }}}");
        JsonObject grouper = new JsonObject("{ \"_id\" : { \"type\": \"$type\", \"event-type\": \"$event-type\",  \"userId\" : \"$recipients.userId\"}, \"count\": { \"$sum\": 1 } }");

        pipeline.add(new JsonObject().put("$match", matcher));
        pipeline.add(new JsonObject().put("$project", transformer));
        pipeline.add(new JsonObject().put("$unwind", "$recipients"));
        pipeline.add(new JsonObject().put("$group", grouper));

        StopWatch step5 = new StopWatch();
        mongo.command(aggregation.toString(), event -> {
            log.info("[WeeklyMails][perf] getAggregatedUsersNotifications page " + page + " time " + step5.elapsedTimeMillis() + " ms");
            if("error".equals(event.body().getString("status", "error"))){
                promise.complete(NotificationContext.empty());
            } else {
                JsonArray result = event.body().getJsonObject("result", new JsonObject())
                        .getJsonObject("cursor", new JsonObject())
                        .getJsonArray("firstBatch", new JsonArray());
                promise.complete(new NotificationContext(userPreferences, result));
            }
        });

        return promise.future();
    }

    private Future<NotificationContext> getUsersNotifications(JsonArray userPreferences, Date from, Date to, int page){
        StopWatch step5 = new StopWatch();
        Promise<NotificationContext> promise = Promise.promise();
        final JsonObject aggregation = new JsonObject();
        JsonArray pipeline = new JsonArray();

        if(userPreferences.isEmpty()){
            promise.complete(NotificationContext.empty());
            return promise.future();
        }
        final JsonArray userIds = new JsonArray();
        userPreferences.forEach( o -> {
            JsonObject pref = (JsonObject) o;
            userIds.add(pref.getString("userId"));
        });

        aggregation
                .put("aggregate", "timeline")
                .put("allowDiskUse", true)
                .put("pipeline", pipeline)
                .put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));

        final JsonObject matcher = new JsonObject()
                .put("$and", new JsonArray()
                        .add(new JsonObject().put("recipients.userId", new JsonObject()
                                .put("$in", userIds)))
                        .add(new JsonObject().put("date", new JsonObject().put("$gte", new JsonObject().put("$date", formatUtcDateTime(from)))))
                        .add(new JsonObject().put("date", new JsonObject().put("$lt", new JsonObject().put("$date", formatUtcDateTime(to))))));

        final JsonObject matcherPostUnwind = new JsonObject().put("recipients.userId", new JsonObject().put("$in", userIds));

        JsonObject transformer = new JsonObject("{ \"_id\": 0, \"type\": 1, \"event-type\": 1, \"params\":1, \"date\": 1, \"recipients\": \"$recipients\"}");

        pipeline.add(new JsonObject().put("$match", matcher));
        pipeline.add(new JsonObject().put("$unwind", "$recipients"));
        pipeline.add(new JsonObject().put("$match", matcherPostUnwind));
        pipeline.add(new JsonObject().put("$project", transformer));

        mongo.command(aggregation.toString(), event -> {
            log.info("[DailyMails][perf] getUsersNotifications page " + page + " time " + step5.elapsedTimeMillis() + " ms");
            if("error".equals(event.body().getString("status", "error"))){
                promise.complete(NotificationContext.empty());
            } else {
                JsonArray result = event.body().getJsonObject("result", new JsonObject())
                        .getJsonObject("cursor", new JsonObject())
                        .getJsonArray("firstBatch", new JsonArray());
                promise.complete(new NotificationContext(userPreferences, result));
            }
        });
        return promise.future();
    }

    private Future<NotificationContext> applyRuleToNotification(NotificationContext notificationContext, JsonObject notificationsDefaults , int page){
        Promise<NotificationContext> promise = Promise.promise();

        Map<String, List<JsonObject>> usersNotifications = new HashMap<>();

        for (Object notificationObj : notificationContext.notifications) {
            JsonObject notification = ((JsonObject) notificationObj).copy().getJsonObject("_id");
            String userId = notification.getString("userId");

            final String notificationName =
                    notification.getString("type", "").toLowerCase() + "." +
                            notification.getString("event-type", "").toLowerCase();

            if (notificationsDefaults.getJsonObject(notificationName) == null)
                continue;

            JsonObject notificationPreference = notificationContext.preferences.stream().map(JsonObject.class::cast)
                        .filter( p -> p.getString("userId").equals(userId))
                        .findFirst().orElse(null);
            if (notificationPreference == null){
                continue;
            }
            notificationPreference.getJsonObject("preferences", new JsonObject())
                    .getJsonObject("config", new JsonObject())
                    .getJsonObject(notificationName, new JsonObject());
            if (TimelineNotificationsLoader.Frequencies.WEEKLY.name().equals(
                    notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
                    !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
                            notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
                    !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
                            notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName)))) {
                notification.put("notificationName", notificationName);

                usersNotifications.putIfAbsent(userId, new ArrayList<>());
                usersNotifications.get(userId).add(notification);
            }
        }

        for (Map.Entry<String, List<JsonObject>> userNotif : usersNotifications.entrySet()) {
            final JsonObject weeklyNotificationsObj = new JsonObject();
            final JsonArray weeklyNotificationsGroupedArray = new JsonArray();
            for (Object notif : userNotif.getValue()) {
                JsonObject notification = (JsonObject) notif;
                if (!weeklyNotificationsObj.containsKey(notification.getString("type").toLowerCase()))
                    weeklyNotificationsObj.put(notification.getString("type").toLowerCase(), new JsonObject()
                            .put("link", notificationsDefaults
                                    .getJsonObject(notification.getString("notificationName")).getString("app-address", ""))
                            .put("event-types", new JsonArray()));
                weeklyNotificationsObj
                        .getJsonObject(notification.getString("type").toLowerCase())
                        .getJsonArray(("event-types"), new JsonArray())
                        .add(notification);
            }

            for (String key : weeklyNotificationsObj.getMap().keySet()) {
                weeklyNotificationsGroupedArray.add(new JsonObject()
                        .put("type", key)
                        .put("link", weeklyNotificationsObj.getJsonObject(key).getString("link", ""))
                        .put("event-types", weeklyNotificationsObj.getJsonObject(key).getJsonArray("event-types")));
            }
            notificationContext.usersNotifications.put(userNotif.getKey(), weeklyNotificationsGroupedArray);
        }
        promise.complete(notificationContext);
        return promise.future();
    }


    private Future<NotificationContext> applyDailyRuleToNotification(NotificationContext notificationContext, JsonObject notificationsDefaults , int page){
        Promise<NotificationContext> promise = Promise.promise();

        Map<String, List<JsonObject>> usersNotifications = new HashMap<>();

        for (Object notificationObj : notificationContext.notifications) {
            JsonObject notification = ((JsonObject) notificationObj).copy();
            String userId = notification.getJsonObject("recipients").getString("userId");

            final String notificationName =
                    notification.getString("type", "").toLowerCase() + "." +
                            notification.getString("event-type", "").toLowerCase();

            if (notificationsDefaults.getJsonObject(notificationName) == null)
                continue;

            JsonObject notificationPreference = notificationContext.preferences.stream().map(JsonObject.class::cast)
                    .filter( p -> p.getString("userId").equals(userId))
                    .findFirst().orElse(null);
            if (notificationPreference == null){
                continue;
            }
            notificationPreference.getJsonObject("preferences", new JsonObject())
                    .getJsonObject("config", new JsonObject())
                    .getJsonObject(notificationName, new JsonObject());
            if (TimelineNotificationsLoader.Frequencies.DAILY.name().equals(
                    notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
                    !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
                            notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
                    !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
                            notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName)))) {

                notification.put("template", notificationsDefaults.getJsonObject(notificationName, new JsonObject()).getString("template", ""));

                usersNotifications.putIfAbsent(userId, new ArrayList<>());
                usersNotifications.get(userId).add(notification);
            }
        }

        for (Map.Entry<String, List<JsonObject>> userNotif : usersNotifications.entrySet()) {
            notificationContext.usersNotifications.put(userNotif.getKey(), new JsonArray(userNotif.getValue()));
        }
        promise.complete(notificationContext);
        return promise.future();
    }

    private static class NotificationContext {

        NotificationContext(JsonArray preferences, JsonArray notifications) {
            this.preferences = preferences;
            this.notifications = notifications;
        }

        static NotificationContext empty() {
            return new NotificationContext(new JsonArray(), new JsonArray());
        }

        public JsonArray preferences;
        public JsonArray notifications;
        public Map<String, JsonArray> usersNotifications = new HashMap<>();
        public  Map<String, String> templates = new HashMap<>();
        public  Map<String, String> subjects = new HashMap<>();
    }

    private enum Periodicity {
        DAILY,
        WEEKLY
    }

}
