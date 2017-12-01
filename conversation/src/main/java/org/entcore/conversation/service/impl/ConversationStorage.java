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

import io.vertx.core.AsyncResult;
import org.entcore.common.storage.FileInfos;
import org.entcore.common.storage.impl.PostgresqlApplicationStorage;
import org.entcore.conversation.Conversation;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class ConversationStorage extends PostgresqlApplicationStorage {

	public ConversationStorage() {
		super("conversation.attachments", Conversation.class.getSimpleName(),
				new JsonObject().put("name", "filename").put("contentType", "\"contentType\""));
	}

	@Override
	public void getInfo(final String fileId, final Handler<AsyncResult<FileInfos>> handler) {
		vertx.setTimer(1000l, new Handler<Long>() {
			@Override
			public void handle(Long event) {
				getInfoProcess(fileId, handler);
			}
		});
	}

	@Override
	public String getInfoQuery() {
		return
				"select distinct a.filename as filename, a.size as size, m.from as owner " +
				"from conversation.attachments a " +
				"left join conversation.usermessagesattachments uma on uma.attachment_id = a.id " +
				"left join conversation.messages m on m.id = uma.message_id " +
				"where a.id = ?;";
	}

}
