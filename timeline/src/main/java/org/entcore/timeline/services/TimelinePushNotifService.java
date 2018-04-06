package org.entcore.timeline.services;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TimelinePushNotifService {

    void sendImmediateNotifs( final String notificationName, final JsonObject notification,
                            final JsonArray userList, final JsonObject notificationProperties);

    void sendNotificationMessageUsers(final String notificationName, final JsonObject notification,
                                      final JsonArray recipientIds, boolean addData);

    void sendNotificationMessageTopic(final String notificationName, final JsonObject notification,
                                      final JsonObject templateParameters, final String topic, boolean addData);

    void sendNotificationMessageCondition(final String notificationName, final JsonObject notification,
                                          final JsonObject templateParameters, final String condition, boolean addData);

    void sendDataMessageUsers(final String notificationName, final JsonObject notification,
                              final JsonObject templateParameters, final JsonArray recipientIds);

    void sendDataMessageTopic(final String notificationName, final JsonObject notification,
                              final JsonObject templateParameters, final String topic);

    void sendDataMessageCondition(final String notificationName, final JsonObject notification,
                                  final JsonObject templateParameters, final String condition);





}
