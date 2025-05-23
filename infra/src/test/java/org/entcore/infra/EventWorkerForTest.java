package org.entcore.infra;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.events.impl.PostgresqlEventStoreFactory;
import org.entcore.test.TestHelper;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventWorkerForTest extends BusModBase implements Handler<Message<JsonObject>> {

    @Override
    public void start() {
        super.start();
        vertx.eventBus().consumer(EventWorkerForTest.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        //multiple pgclient (eventstore) should return error
        //it should trigger init fail for eventstore because eventstore is already defined outside of worker
        final EventStoreFactory fac = EventStoreFactory.getFactory();
        fac.setVertx(vertx);
        final EventStore store = fac.getEventStore("test");
        store.createAndStoreEvent("ACCESS", TestHelper.helper().directory().generateUser("user1"));
        //TODO missing handler on eventstore interface
        vertx.setTimer(500, r->{
            message.reply(new JsonObject().put("success", true));
        });
    }
}
