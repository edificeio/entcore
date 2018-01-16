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

package org.entcore.feeder.export.eliot;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

public class PersHorsAAFExportProcessing extends UserExportProcessing {

	private final String date;
	private final String stdPrefix;

	public PersHorsAAFExportProcessing(String path, String date, String stdPrefix, boolean concat) {
		super("dictionary/export/eliot/PersHorsAAF.json", 10000, path,
				new JsonArray().add("Guest"), "PersHorsAAF", date, stdPrefix, concat);
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
