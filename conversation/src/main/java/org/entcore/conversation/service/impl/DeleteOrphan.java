/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.conversation.service.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
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
	private static final long TIMEOUT = 300000l;

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
		sql.transaction(builder.build(), new DeliveryOptions().setSendTimeout(TIMEOUT),
				SqlResult.validResultHandler(1, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> res) {
				if (res.isRight()) {
					log.info("Successful delete orphan conversation messages.");
					final JsonArray attachments = res.right().getValue();
					if (attachments != null && attachments.size() > 0) {
						log.info("Orphan attachments : " + attachments.encode());
						JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
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
						sql.prepared(deletOrphanAttachments, ids, new DeliveryOptions().setSendTimeout(TIMEOUT), new Handler<Message<JsonObject>>() {
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
