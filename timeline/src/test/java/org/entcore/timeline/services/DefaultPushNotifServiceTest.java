package org.entcore.timeline.services;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.I18nOverrides;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.timeline.services.impl.DefaultPushNotifService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class DefaultPushNotifServiceTest {

    private Vertx vertx;
    private DefaultPushNotifService service;
    private final Map<String, String> eventsI18n = new HashMap<>();

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        service = new DefaultPushNotifService(vertx, new JsonObject(), null);
        eventsI18n.put("fr", "\"push.notif.title\":\"Nouveau document\",");
        service.setEventsI18n(eventsI18n);
        I18n.getInstance().setOverrides(I18nOverrides.builder()
                .addDomainOverrides("ent.example.org", "fr", null,
                        new JsonObject().put("push.notif.title", "Nouveau fichier"))
                .build());
    }

    @After
    public void tearDown(TestContext context) {
        I18n.getInstance().setOverrides(null);
        vertx.close(context.asyncAssertSuccess());
    }

    private static JsonObject notification() {
        return new JsonObject().put("pushNotif", new JsonObject()
                .put("title", "push.notif.title").put("body", "Un document a été partagé"));
    }

    @Test
    public void theTitleGetsTheOverridesOfTheDomainOfTheRecipient(TestContext context) {
        service.processMessage(notification(), "fr-FR,fr;q=0.9", "ent.example.org", true, false, message ->
                context.assertEquals("Nouveau fichier", message.getJsonObject("notification").getString("title")));
        service.processMessage(notification(), "fr", "other.example.org", true, false, message ->
                context.assertEquals("Nouveau document", message.getJsonObject("notification").getString("title"),
                        "without override, the title registered by the application is used"));
    }

    @Test
    public void titlesRegisteredLaterAreTranslated(TestContext context) {
        service.processMessage(notification(), "fr", I18n.DEFAULT_DOMAIN, true, false, message ->
                context.assertEquals("Nouveau document", message.getJsonObject("notification").getString("title")));
        eventsI18n.put("fr", eventsI18n.get("fr") + "\"push.notif.other\":\"Autre\",");
        service.processMessage(new JsonObject().put("pushNotif", new JsonObject().put("title", "push.notif.other")),
                "fr", I18n.DEFAULT_DOMAIN, true, false, message ->
                        context.assertEquals("Autre", message.getJsonObject("notification").getString("title"),
                                "the translations are not cached forever"));
    }
}
