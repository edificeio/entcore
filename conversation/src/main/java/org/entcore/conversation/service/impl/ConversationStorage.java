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
