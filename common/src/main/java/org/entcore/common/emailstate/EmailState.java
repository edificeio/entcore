package org.entcore.common.emailstate;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import org.entcore.common.user.UserInfos;

public class EmailState {
    static public String BUS_ADDRESS = "mail.state";

	/**
	 * Start a new mail validation workflow.
	 * @param userId user ID
	 * @param email the mail address to be checked
	 * @return the new emailState
	 */
    static public Future<JsonObject> setPending(EventBus eb, String userId, String email) {
        Promise<JsonObject> promise = Promise.promise();
		JsonObject action = new JsonObject()
            .put("action", "set-pending")
            .put("userId", userId)
            .put("email", email);
		eb.request(BUS_ADDRESS, action, handlerToAsyncHandler( reply -> {
            completePromise(reply, promise);
        }));
        return promise.future();
    }

	/**
	 * Check if a user has a verified email address
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid email address }
	 */
    static public Future<JsonObject> isValid(EventBus eb, String userId) {
        Promise<JsonObject> promise = Promise.promise();
		JsonObject action = new JsonObject()
            .put("action", "is-valid")
            .put("userId", userId);
		eb.request(BUS_ADDRESS, action, handlerToAsyncHandler( reply -> {
            completePromise(reply, promise);
        }));
        return promise.future();
    }

	/**
	 * Verify a pending email address of a user, by checking a code.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 * 	tries?: number of remaining retries,
	 *  ttl: number of seconds remaining before expiration of the code
	 * }
	 */
    static public Future<JsonObject> tryValidate(EventBus eb, String userId, String code) {
        Promise<JsonObject> promise = Promise.promise();
		JsonObject action = new JsonObject()
            .put("action", "try-validate")
            .put("userId", userId)
            .put("code", code);
		eb.request(BUS_ADDRESS, action, handlerToAsyncHandler( reply -> {
            completePromise(reply, promise);
        }));
        return promise.future();
    }

	/**
	 * Get current mail validation details.
	 * @param userId user ID
	 * @return {email:string, emailState:object|null, waitInSeconds:number}
	 */
    static public Future<JsonObject> getDetails(EventBus eb, String userId) {
        Promise<JsonObject> promise = Promise.promise();
		JsonObject action = new JsonObject()
            .put("action", "get-details")
            .put("userId", userId);
		eb.request(BUS_ADDRESS, action, handlerToAsyncHandler( reply -> {
            completePromise(reply, promise);
        }));
        return promise.future();
    }

	private static void completePromise(Message<JsonObject> res, Promise<JsonObject> promise) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonObject r = res.body().getJsonObject("result", new JsonObject());
			promise.complete( r );
		} else {
			promise.fail( res.body().getString("message", "") );
		}
	}

	/** 
	 * Send an email with actual validation code.
	 * @param infos User infos
	 * @param email address where to send
	 * @param pendingEmailState with code to send
	 * @return email ID
	*/
	static public Future<Long> sendMail(EventBus eb, final HttpServerRequest request, UserInfos infos, JsonObject pendingEmailState) {
        Promise<Long> promise = Promise.promise();
		if( infos==null || pendingEmailState==null ) {
			promise.complete(null);
		} else {
			JsonObject action = new JsonObject()
				.put("action", "send-mail")
				.put("userId", infos.getUserId())
				.put("firstName", infos.getFirstName())
				.put("lastName", infos.getLastName())
				.put("userName", infos.getUsername())
				.put("email", EmailStateUtils.getPending(pendingEmailState))
				.put("emailState", pendingEmailState);
			eb.request( BUS_ADDRESS,
						action, 
						new DeliveryOptions().setHeaders(request.headers()), 
						handlerToAsyncHandler( reply -> {
				if ("ok".equals(reply.body().getString("status"))) {
					Long r = reply.body().getLong("result");
					promise.complete( r );
				} else {
					promise.fail( reply.body().getString("message", "") );
				}
			}));
		}
        return promise.future();
	}
}
