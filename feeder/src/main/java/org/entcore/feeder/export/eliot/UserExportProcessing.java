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
		attributes = new JsonArray(new ArrayList<>(exportMapping.fieldNames())).add("externalId");
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
