package org.entcore.common.sms;

import fr.wseduc.sms.Sms;
import fr.wseduc.sms.SmsSendingReport;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.events.EventStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Delegate class of {@link fr.wseduc.sms.Sms Sms} which stores new event for every successfully sent SMS.
 */
public class SmsSender {
    public static final String EVENT = "SMS";
    /** Actual SMS sender.*/
    private final Sms sms;
    private final EventStore eventStore;

    public SmsSender(final Sms sms, EventStore eventStore) {
        this.sms = sms;
        this.eventStore = eventStore;
    }

    /**
     * Send a unique SMS to a recipient.
     * @param request HTTP request that triggered the SMS
     * @param phone Phone number to which the SMS should be sent
     * @param template Name of the SMS template to use
     * @param params Values to populate the template
     * @return ID of the SMS (format may differ depending on who is the actual sender)
     */
    public Future<String> sendUnique(HttpServerRequest request, final String phone, String template, JsonObject params) {
        return sendUnique(request, phone, template, params, null);
    }
    /**
     * Send a unique SMS to a recipient.
     * @param request HTTP request that triggered the SMS
     * @param phone Phone number to which the SMS should be sent
     * @param template Name of the SMS template to use
     * @param params Values to populate the template
     * @param module Name of the module which triggered the SMS
     * @return ID of the SMS (format may differ depending on who is the actual sender)
     * */
    public Future<String> sendUnique(HttpServerRequest request, final String phone, String template, JsonObject params, final String module){
        return send(request, phone, template, params, module).map(report -> {
                final String[] sentSmdIds = report.getIds();
                return sentSmdIds == null || sentSmdIds.length == 0 ? sentSmdIds[0] : "";
            }
        );
    }

    public Future<SmsSendingReport> send(HttpServerRequest request, final String phone, String template, JsonObject params){
        return send(request, phone, template, params, null);
    }

    public Future<SmsSendingReport> send(HttpServerRequest request, final String phone, String template, JsonObject params, final String module) {
        return sms.send(request, phone, template, params, module)
                .onSuccess(report -> storeEvent(phone, report, request, module));
    }

    public Future<SmsSendingReport> send(HttpServerRequest request, final String phone, String message, final String module) {
        return sms.send(phone, message)
                .onSuccess(report -> storeEvent(phone, report, request, module));
    }


    /**
     * Stores in event module all the SMS that were successfully sent.
     * @param report SMS sending report which lists all the SMS that were successfully sent
     * @param request HTTP request that triggered the SMS sending
     * @param module Name of the module which triggered the SMS
     */
    private void storeEvent(final String phone,
                            final SmsSendingReport report,
                           final HttpServerRequest request,
                           final String module){
        if(eventStore != null) {
            for (String sentSmdId : report.getIds()) {
                final JsonObject customAttributes = new JsonObject()
                    .put("sms_id", sentSmdId)
                    .put("phone", phone);
                if (StringUtils.isNotBlank(module)) {
                    customAttributes.put("override-module", module);
                }
                eventStore.createAndStoreEvent(EVENT, request, customAttributes);
            }
        }
    }
}
