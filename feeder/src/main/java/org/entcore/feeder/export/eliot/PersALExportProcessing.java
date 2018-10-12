/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.feeder.export.eliot;

import org.entcore.feeder.dictionary.structures.User;
import org.entcore.feeder.utils.Function;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import java.io.File;

public class PersALExportProcessing extends BaseExportProcessing {

	private static final JsonArray function = new fr.wseduc.webutils.collections.JsonArray().add("ADMIN_LOCAL");

	public PersALExportProcessing(String path, String date, boolean concat) {
		super("dictionary/export/eliot/PersAL.json", 5000, path + File.separator + "Complet_" + date + "_PersAL_", concat);
	}

	@Override
	public void start(Handler<Message<JsonObject>> handler) {
		export(handler, null);
	}

	@Override
	protected void process(XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
		writer.add(eventFactory.createAttribute("name", "categoriePersonne"));
		writer.add(eventFactory.createStartElement("", "", "value"));
		writer.add(eventFactory.createCharacters("PersAL"));
		writer.add(eventFactory.createEndElement("", "", "value"));
	}

	@Override
	protected void count(final Handler<Integer> handler) {
		handler.handle(1);
	}

	@Override
	protected void list(final Integer skip, final Integer limit, final Handler<JsonArray> handler) {
		TransactionManager.executeTransaction(new Function<TransactionHelper, Message<JsonObject>>() {
			@Override
			public void apply(TransactionHelper value) {
				User.listByFunctions(EliotExporter.ELIOT, function, value);
			}

			@Override
			public void handle(Message<JsonObject> result) {
				JsonArray r = result.body().getJsonArray("results");
				if ("ok".equals(result.body().getString("status")) && r != null && r.size() == 1) {
					JsonArray rs = r.getJsonArray(0);
					handler.handle(rs);
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
