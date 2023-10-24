package org.entcore.infra;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.impl.PostgresEmailHelper;
import org.entcore.common.email.impl.PostgresEmailBuilder;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.impl.PostgresqlEventStoreFactory;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailWorkerForTest extends BusModBase implements Handler<Message<JsonObject>> {

    PostgresEmailBuilder.EmailBuilder mail() {
        return PostgresEmailBuilder.mail().withPriority(0).withProfile("Teacher").withUserId("userid").withPlatformUrl("http://entcore.org").withPlatformId("platformid").withModule("infra").withBody("Test").withHeader("header", "value").withSubject("subject").withFrom("test@entcore.org").withTo("dest@entcore.org");
    }

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(MailWorkerForTest.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        //multiple pgclient (eventstore) should not affect mailer
        //it should trigger init fail for eventstore because eventstore is already defined outside of worker
        final PostgresqlEventStoreFactory fac = new PostgresqlEventStoreFactory();
        fac.setVertx(vertx);
        final EventStore store = fac.getEventStore("test");
        final PostgresEmailHelper helper = PostgresEmailHelper.create(vertx, config().getJsonObject("postgres"));
        final List<PostgresEmailBuilder.AttachmentBuilder> atts = new ArrayList<>();
        final PostgresEmailBuilder.EmailBuilder mail = mail();
        atts.add(PostgresEmailBuilder.attachment(mail).withName("name").withEncodedContent("content"));
        helper.createWithAttachments(mail, atts).onComplete((r -> {
            if (r.failed()) {
                r.cause().printStackTrace();
                message.fail(400, r.cause().getMessage());
            }else {
                helper.setRead((UUID)mail.getMail().get("id"), new JsonObject()).onComplete(r2->{
                    if (r2.failed()) {
                        r2.cause().printStackTrace();
                        message.fail(400, r2.cause().getMessage());
                    } else {
                        message.reply(new JsonObject().put("success", r.succeeded()));
                    }
                });
            }
        }));
    }
}
