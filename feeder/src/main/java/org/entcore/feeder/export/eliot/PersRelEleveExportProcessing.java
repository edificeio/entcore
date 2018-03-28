/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.feeder.export.eliot;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PersRelEleveExportProcessing extends UserExportProcessing {

	private final String date;
	private final String stdPrefix;

	public PersRelEleveExportProcessing(String path, String date, String stdPrefix, boolean concat) {
		super("dictionary/export/eliot/PersRelEleve.json", 10000, path,
				new fr.wseduc.webutils.collections.JsonArray().add("Relative"), "PersRelEleve", date, stdPrefix, concat);
		this.date = date;
		this.stdPrefix = stdPrefix;
	}

	@Override
	public void start(Handler<Message<JsonObject>> handler) {
		export(handler, new PersEducNatExportProcessing(basePath, date, stdPrefix, concat));
	}

}
