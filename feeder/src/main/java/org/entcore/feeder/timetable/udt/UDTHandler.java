/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.feeder.timetable.udt;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class UDTHandler extends DefaultHandler {

	private String currentTag = "";
	private String currentEntityType = "";
	private JsonObject currentEntity;
	private final UDTImporter udtImporter;

	public UDTHandler(UDTImporter udtImporter) {
		this.udtImporter = udtImporter;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if ("annee".equals(currentTag)) {
			udtImporter.setYear(new String(ch, start, length));
		} else if ("fin_eleve".equals(currentTag)) {
			udtImporter.setEndStudents(new String(ch, start, length));
		} else if ("debut_eleve".equals(currentTag)) {
			udtImporter.setStartDateStudents(new String(ch, start, length));
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		currentTag = localName;
		final JsonObject o = attributesToJsonObject(attributes);
		if (isNotEmpty(currentEntityType)) {
			JsonArray a = currentEntity.getJsonArray(currentTag);
			if (a == null) {
				a = new JsonArray();
				currentEntity.put(currentTag, a);
			}
			a.add(o);
			return;
		}

		switch (localName) {
			case "ligfiche":
			case "fiche":
			case "rgpmt":
			case "mat":
			case "ele_gpe":
			case "eleve":
			case "prof":
			case "div":
			case "gpe":
			case "salle":
			case "coens":
			case "demi_seq":
			case "init":
			case "semaines":
			case "fermeture":
				currentEntityType = localName;
				currentEntity = o;
				break;
		}
	}

	private JsonObject attributesToJsonObject(Attributes attributes) {
		final JsonObject j = new JsonObject();
		for (int i = 0; i < attributes.getLength(); i++) {
			j.put(attributes.getLocalName(i), attributes.getValue(i));
		}
		return j;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		currentTag = "";
		if (localName.equals(currentEntityType)) {
			currentEntityType = "";
			switch (localName) {
				case "ligfiche":
					udtImporter.addCourse(currentEntity);
					break;
				case "fiche":
					udtImporter.addFicheT(currentEntity);
					break;
				case "rgpmt":
					udtImporter.addGroup2(currentEntity);
					break;
				case "mat":
					udtImporter.addSubject(currentEntity);
					break;
				case "ele_gpe":
					udtImporter.addEleve(currentEntity);
					break;
				case "eleve":
					udtImporter.eleveMapping(currentEntity);
					break;
				case "prof":
					udtImporter.addProfesseur(currentEntity);
					break;
				case "div":
					udtImporter.addClasse(currentEntity);
					break;
				case "gpe":
					udtImporter.addGroup(currentEntity);
					break;
				case "salle":
					udtImporter.addRoom(currentEntity);
					break;
				case "coens":
					udtImporter.addCoens(currentEntity);
					break;
				case "demi_seq":
					udtImporter.initSchedule(currentEntity);
					break;
				case "init":
					udtImporter.initSchoolYear(currentEntity);
					break;
				case "semaines":
					udtImporter.initPeriods(currentEntity);
					break;
				case "fermeture":
					udtImporter.initHolidays(currentEntity);
					break;
			}
			currentEntity = null;
		}
	}

}
