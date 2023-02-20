package org.entcore.common.sms;

import fr.wseduc.sms.Sms;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;

public class SmsSenderFactory {
    private static final SmsSenderFactory instance = new SmsSenderFactory();

    private SmsSenderFactory() {}

    public static final SmsSenderFactory getInstance() {
        return instance;
    }

    public void init(Vertx vertx, JsonObject config) {
        Sms.getFactory().init(vertx, config);
    }

    /**
     * Create an instance with a default renderer (see the implementation of underlying
     * {@link fr.wseduc.sms.Sms#SmsFactory.newInstance(EventStore) newInstance})
     * @param eventStore Event store to be used to report successfully sent SMS
     * @return An instance of the SMS sender
     */
    public SmsSender newInstance(final EventStore eventStore) {
        return newInstance(null, eventStore);
    }
    public SmsSender newInstance(final Renders render, final EventStore eventStore) {
        Sms sms = Sms.getFactory().newInstance(render);
        return new SmsSender(sms, eventStore);
    }
}
