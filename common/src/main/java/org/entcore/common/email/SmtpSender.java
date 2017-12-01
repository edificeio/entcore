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

package org.entcore.common.email;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.Bounce;
import fr.wseduc.webutils.email.BusMailSender;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SmtpSender extends BusMailSender implements EmailSender {

	private final ObjectMapper mapper;

	public SmtpSender(Vertx vertx) {
		super(vertx, null);
		String node = (String) vertx.sharedData().getLocalMap("server").get("node");
		if (node == null) {
			node = "";
		}
		emailAddress = node + "wse.email";
		mapper = new ObjectMapper();
		SimpleModule simpleModule = new SimpleModule("BsonDateModule", new Version(1, 0, 0, null, "org.entcore", "common"));
		simpleModule.addDeserializer(Date.class, new BsonDateDeserializer());
		mapper.registerModule(simpleModule);
		mapper.addMixInAnnotations(Bounce.class, BounceMixIn.class);
	}

	@Override
	public void hardBounces(Date date, Handler<Either<String, List<Bounce>>> handler) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, 1);
		hardBounces(date, c.getTime(), handler);
	}

	@Override
	public void hardBounces(Date startDate, Date endDate, final Handler<Either<String, List<Bounce>>> handler) {
		final JsonObject query = new JsonObject()
				.put("date", new JsonObject()
						.put("$gte", new JsonObject().put("$date", removeTime(startDate).getTime()))
						.put("$lt", new JsonObject().put("$date", removeTime(endDate).getTime())));
		MongoDb.getInstance().find("bounces", query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				try {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray l = event.body().getJsonArray("results");
						if (l == null || l.size() == 0) {
							handler.handle(new Either.Right<String, List<Bounce>>(
									Collections.<Bounce>emptyList()));
							return;
						}
						List<Bounce> bounces = mapper.readValue(l.encode(), new TypeReference<List<Bounce>>(){});
						handler.handle(new Either.Right<String, List<Bounce>>(bounces));
					} else {
						handler.handle(new Either.Left<String, List<Bounce>>(event.body().getString("message")));
					}
				} catch (RuntimeException | IOException e) {
					handler.handle(new Either.Left<String, List<Bounce>>(e.getMessage()));
					log.error(e.getMessage(), e);
				}
			}
		});
	}

	private Date removeTime(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

}
