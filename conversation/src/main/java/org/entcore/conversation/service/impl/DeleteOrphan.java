/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.conversation.service.impl;

import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class DeleteOrphan implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(DeleteOrphan.class);

	private static final String DELETE_ORPHAN_MESSAGE =
			"delete from conversation.messages where id IN " +
			"(select m.id from conversation.messages m " +
			"left join conversation.usermessages um on um.message_id = m.id " +
			"where um.user_id is NULL);";

	private static final String DELETE_ORPHAN_ATTACHMENT =
			"delete from conversation.attachments where id IN " +
			"(select a.id from conversation.attachments a " +
			"left join conversation.usermessagesattachments uma on uma.attachment_id = a.id " +
			"where uma.message_id is NULL);";

	@Override
	public void handle(Long event) {
		final SqlStatementsBuilder builder = new SqlStatementsBuilder();
		builder.raw(DELETE_ORPHAN_MESSAGE);
		builder.raw(DELETE_ORPHAN_ATTACHMENT);
		Sql.getInstance().transaction(builder.build(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error deleting orphan messages and attachments : " + event.body().getString("message", ""));
				}
			}
		});
	}

}
