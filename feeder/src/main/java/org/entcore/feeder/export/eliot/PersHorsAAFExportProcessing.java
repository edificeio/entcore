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

package org.entcore.feeder.export.eliot;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

public class PersHorsAAFExportProcessing extends UserExportProcessing {

	private final String date;
	private final String stdPrefix;

	public PersHorsAAFExportProcessing(String path, String date, String stdPrefix, boolean concat) {
		super("dictionary/export/eliot/PersHorsAAF.json", 10000, path,
				new fr.wseduc.webutils.collections.JsonArray().add("Guest"), "PersHorsAAF", date, stdPrefix, concat);
		this.date = date;
		this.stdPrefix = stdPrefix;
	}

	@Override
	protected void process(XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
		writer.add(eventFactory.createAttribute("name", "categoriePersonne"));
		writer.add(eventFactory.createStartElement("", "", "value"));
		writer.add(eventFactory.createCharacters("HorsAAF"));
		writer.add(eventFactory.createEndElement("", "", "value"));
	}

	@Override
	public void start(Handler<Message<JsonObject>> handler) {
		export(handler, new EtabEducNatExportProcessing(basePath, date, stdPrefix, concat));
	}

}
