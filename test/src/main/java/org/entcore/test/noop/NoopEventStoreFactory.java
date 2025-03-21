package org.entcore.test.noop;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;

public class NoopEventStoreFactory extends EventStoreFactory {

    private NoopEventStoreFactory(){}

    public static final EventStoreFactory INSTANCE = new NoopEventStoreFactory();

    private static final EventStore EVENT_STORE = new EventStore() {
        @Override
        public void createAndStoreEvent(final String eventType, final HttpServerRequest request, final JsonObject customAttributes) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final UserInfos user, final JsonObject customAttributes) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final HttpServerRequest request) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final UserInfos user) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final String login) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final String login, final HttpServerRequest request) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final String login, final String clientId) {

        }

        @Override
        public void createAndStoreEvent(final String eventType, final String login, final String clientId, final HttpServerRequest request) {

        }

        @Override
        public void createAndStoreEventByUserId(final String eventType, final String userId, final String clientId) {

        }

        @Override
        public void createAndStoreEventByUserId(final String eventType, final String userId, final String clientId, final HttpServerRequest request) {

        }

        @Override
        public void storeCustomEvent(final String baseEventType, final JsonObject payload) {

        }

        @Override
        public void createAndStoreShareEvent(String userId, String resourceId, List<String> rights) {

        }
    };
    @Override
    public EventStore getEventStore(final String module) {
        return EVENT_STORE;
    }
}
