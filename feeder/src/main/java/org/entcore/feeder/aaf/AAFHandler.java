/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.utils.JsonUtil;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.feeder.utils.AAFUtil.convertDate;

public final class AAFHandler extends DefaultHandler {

	private String currentTag = "";
	private String currentAttribute = "";
	private StringBuilder s;
	private JsonObject currentStructure;
	private final JsonObject mapping;
	private final ImportProcessing processing;

	public AAFHandler(ImportProcessing processing) {
		this.processing = processing;
		this.mapping = JsonUtil.loadFromResource(processing.getMappingResource());
		this.s = new StringBuilder();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		s = new StringBuilder();
		currentTag = localName;
		switch (localName) {
			case "addRequest" :
				currentStructure = new JsonObject();
				break;
			case "attr" :
				currentAttribute = attributes.getValue(0);
				break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (currentTag) {
			case "id" : addExternalId(s.toString());
				break;
			case "value" : addValueInAttribute(s.toString());
				break;
		}
		currentTag = "";
		switch (localName) {
			case "addRequest" :
				processing.process(currentStructure);
				break;
			case "attr" :
				currentAttribute = "";
				break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		s.append(new String(ch, start, length));
	}

	private void addValueInAttribute(String s) throws SAXException {
		if (s == null || (s.isEmpty() && !"ENTAuxEnsClassesPrincipal".equals(currentAttribute) &&
				!"mobile".equals(currentAttribute) && !"ENTPersonMobileSMS".equals(currentAttribute))) {
			return;
		}
		JsonObject j = mapping.getObject(currentAttribute);
		if (j == null) {
			throw new SAXException("Unknown attribute " + currentAttribute);
		}
		if (currentStructure == null) {
			throw new SAXException("Value is found but structure isn't defined.");
		}
		String type = j.getString("type");
		String attribute = j.getString("attribute");
		final boolean prefix = j.getBoolean("prefix", false);
		if ("birthDate".equals(attribute) && !s.isEmpty()) {
			s = convertDate(s);
		}
		if (type != null && type.contains("array")) {
			JsonArray a = currentStructure.getArray(attribute);
			if (a == null) {
				a = new JsonArray();
				currentStructure.putArray(attribute, a);
			}
			if (!s.isEmpty()) {
				a.add(JsonUtil.convert(s, type, (prefix ? processing.getAcademyPrefix() : null)));
			}
		} else {
			Object v = JsonUtil.convert(s, type, (prefix ? processing.getAcademyPrefix() : null));
			if (!(v instanceof JsonUtil.None)) {
				currentStructure.putValue(attribute, v);
			}
		}
	}

	private void addExternalId(String s) throws SAXException {
		if (currentStructure != null) {
			currentStructure.putString("externalId",
					(isNotEmpty(processing.getAcademyPrefix()) ? processing.getAcademyPrefix() + s : s));
		} else {
			throw new SAXException("Id is found but structure isn't defined.");
		}
	}

}
