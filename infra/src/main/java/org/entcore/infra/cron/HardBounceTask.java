/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.infra.cron;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.email.Bounce;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

public class HardBounceTask implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(HardBounceTask.class);
	public static final String PLATFORM_ITEM_TYPE = "INVALID_EMAIL";
	private final EmailSender emailSender;
	private final int relativeDay;
	private final TimelineHelper timeline;
	private final AsyncMap<Object, Object> invalidEmails;
	public static final String PLATEFORM_COLLECTION = "platform";

	public HardBounceTask(EmailSender emailSender, int relativeDay, TimelineHelper timeline, AsyncMap<Object, Object> invalidEmails) {
		this.emailSender = emailSender;
		this.relativeDay = relativeDay;
		this.timeline = timeline;
		this.invalidEmails = invalidEmails;
	}

	@Override
	public void handle(Long event) {
		log.info("Start hard bounce task.");
		log.debug(emailSender.getClass().getName());
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, relativeDay);
		emailSender.hardBounces(c.getTime(), new Handler<Either<String, List<Bounce>>>() {
			@Override
			public void handle(Either<String, List<Bounce>> r) {
				if (r.isRight()) {
					Set<String> emails = new HashSet<>();
					for (Bounce b : r.right().getValue()) {
						if (Utils.isNotEmpty(b.getEmail())) {
							if (emails.add(b.getEmail())) {
								invalidEmails.put(b.getEmail(), "", new Handler<AsyncResult<Void>>() {
									@Override
									public void handle(AsyncResult<Void> event) {
										if (event.failed()) {
											log.error("Error adding invalid email in map.", event.cause());
										}
									}
								});
							}
						}
					}
					if (emails.isEmpty()) {
						return;
					}

					JsonObject q = new JsonObject().put("type", PLATFORM_ITEM_TYPE);
					JsonObject modifier = new JsonObject().put("$addToSet", new JsonObject().put("invalid-emails",
							new JsonObject().put("$each", new JsonArray(new ArrayList<>(emails)))));
					MongoDb.getInstance().update(PLATEFORM_COLLECTION, q, modifier, true, false);

					String query = "MATCH (u:User) WHERE u.email IN {emails} REMOVE u.email RETURN collect(distinct u.id) as ids";
					JsonObject params = new JsonObject().put("emails", new JsonArray(new ArrayList<>(emails)));
					Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								JsonArray res = event.body().getJsonArray("result");
								if (res != null && res.size() == 1 && res.getJsonObject(0) != null) {
									notifyOnTimeline(res.getJsonObject(0).getJsonArray("ids"));
								}
							} else {
								log.error(event.body().getString("message"));
							}
						}
					});
				} else {
					log.error(r.left().getValue());
				}
			}
		});
	}

	private void notifyOnTimeline(JsonArray userIds) {
		if (userIds == null) return;

		List<String> recipients = userIds.getList();
		timeline.notifyTimeline(new JsonHttpServerRequest(new JsonObject()),
				"userbook.delete-email", null, recipients, null, new JsonObject());
	}

}
