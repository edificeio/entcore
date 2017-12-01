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

import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.storage.Storage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeleteOrphan implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(DeleteOrphan.class);

	private static final String SELECT_ORPHAN_ATTACHMENT =
			"select a.id as orphanid from conversation.attachments a " +
			"left join conversation.usermessagesattachments uma on uma.attachment_id = a.id " +
			"where uma.message_id is NULL;";

	private static final String DELETE_ORPHAN_MESSAGE =
			"delete from conversation.messages where id IN " +
			"(select m.id from conversation.messages m " +
			"left join conversation.usermessages um on um.message_id = m.id " +
			"where um.user_id is NULL);";

	private final Storage storage;

	public DeleteOrphan(Storage storage) {
		this.storage = storage;
	}

	@Override
	public void handle(Long event) {
		final Sql sql = Sql.getInstance();
		final SqlStatementsBuilder builder = new SqlStatementsBuilder();
		builder.raw(DELETE_ORPHAN_MESSAGE);
		builder.raw(SELECT_ORPHAN_ATTACHMENT);
		sql.transaction(builder.build(), SqlResult.validResultHandler(1, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> res) {
				if (res.isRight()) {
					log.info("Successful delete orphan conversation messages.");
					final JsonArray attachments = res.right().getValue();
					if (attachments != null && attachments.size() > 0) {
						log.info("Orphan attachments : " + attachments.encode());
						JsonArray ids = new JsonArray();
						for (Object attObj : attachments) {
							if (!(attObj instanceof JsonObject)) continue;
							JsonObject unusedAttachment = (JsonObject) attObj;
							final String attachmentId = unusedAttachment.getString("orphanid");
							ids.add(attachmentId);
							storage.removeFile(attachmentId, new Handler<JsonObject>() {
								public void handle(JsonObject event) {
									if (!"ok".equals(event.getString("status"))) {
										log.error("Error while tying to delete attachment file (_id: {" + attachmentId + "})");
									}
								}
							});
						}
						final String deletOrphanAttachments =
								"delete from conversation.attachments where id IN " + Sql.listPrepared(ids.getList());
						sql.prepared(deletOrphanAttachments, ids, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									log.error("Error deleting orphan attachments : " + event.body().getString("message", ""));
								} else {
									log.info("Successful delete orphan conversation attachments.");
								}
							}
						});
					}
				} else {
					log.error("Orphan conversation error : " + res.left().getValue());
				}
			}
		}));
	}

}
