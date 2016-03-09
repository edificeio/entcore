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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.Bounce;
import fr.wseduc.webutils.email.BusMailSender;
import fr.wseduc.webutils.email.EmailSender;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

import java.util.Date;
import java.util.List;

public class SmtpSender extends BusMailSender implements EmailSender {

	public SmtpSender(Vertx vertx, Container container) {
		super(vertx, container);
		String node = (String) vertx.sharedData().getMap("server").get("node");
		if (node == null) {
			node = "";
		}
		emailAddress = node + "wse.email";
	}

	@Override
	public void hardBounces(Date date, Handler<Either<String, List<Bounce>>> handler) {

	}

	@Override
	public void hardBounces(Date startDate, Date endDate, Handler<Either<String, List<Bounce>>> handler) {

	}

}
