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
import java.util.ArrayList;

public abstract class UserExportProcessing extends BaseExportProcessing {

	private final JsonArray attributes;
	private final JsonArray profiles;
	private final String category;
	protected final String basePath;

	protected UserExportProcessing(String mapping, int nbByFile, String basePath, JsonArray profiles,
			String category, String date, String stdPrefix, boolean concat) {
		super(mapping, nbByFile, basePath + File.separator + stdPrefix + "_Complet_" + date + "_" + category + "_", concat);
		attributes = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(exportMapping.fieldNames())).add("externalId");
		this.profiles = profiles;
		this.category = category;
		this.basePath = basePath;
	}

	@Override
	protected void process(XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
		writer.add(eventFactory.createAttribute("name", "categoriePersonne"));
		writer.add(eventFactory.createStartElement("", "", "value"));
		writer.add(eventFactory.createCharacters(category));
		writer.add(eventFactory.createEndElement("", "", "value"));
	}

	@Override
	protected void count(final Handler<Integer> handler) {
		TransactionManager.executeTransaction(new Function<TransactionHelper, Message<JsonObject>>() {
			@Override
			public void apply(TransactionHelper value) {
				User.count(EliotExporter.ELIOT, profiles, value);
			}

			@Override
			public void handle(Message<JsonObject> result) {
				JsonArray r = result.body().getJsonArray("results");
				if ("ok".equals(result.body().getString("status")) && r != null && r.size() == 1) {
					JsonArray rs = r.getJsonArray(0);
					if (rs != null && rs.size() == 1) {
						JsonObject row = rs.getJsonObject(0);
						handler.handle(row.getInteger("nb", 0));
						return;
					}
				}
				handler.handle(null);
			}
		});
	}

	@Override
	protected void list(final Integer skip, final Integer limit, final Handler<JsonArray> handler) {
		TransactionManager.executeTransaction(new Function<TransactionHelper, Message<JsonObject>>() {
			@Override
			public void apply(TransactionHelper value) {
				User.list(EliotExporter.ELIOT, profiles, attributes, skip, limit, value);
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
