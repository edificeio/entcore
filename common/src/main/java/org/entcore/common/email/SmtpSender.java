/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
