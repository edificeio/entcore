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

package org.entcore.feeder.aaf;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CleanImportProcessingGlobal extends BaseImportProcessing {


	protected CleanImportProcessingGlobal(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(Handler<Message<JsonObject>> handler) {
		initAcademyPrefix(path);
		parse(handler, null);
	}

	@Override
	public String getMappingResource() {
		return "";
	}

	@Override
	protected void preCommit() {
		log.info(e-> "clean import process global", true);
		final JsonArray importPrefixList = importer.getPrefixToImportList();

		if (importPrefixList == null || importPrefixList.isEmpty()) {
			log.info(e-> "Global method calls in clean import process", true);
			importer.restorePreDeletedUsers();
			importer.deleteOldProfileAttachments();
			importer.countUsersInGroups();
			importer.removeOldFunctionalGroup();
			importer.removeEmptyClasses();
		}
	}

	@Override
	public void process(JsonObject object) {
	}

	@Override
	protected String getFileRegex() {
		return "";
	}
}
