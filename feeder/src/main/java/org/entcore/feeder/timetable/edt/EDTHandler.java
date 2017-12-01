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

package org.entcore.feeder.timetable.edt;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class EDTHandler extends DefaultHandler {

	private static final Logger log = LoggerFactory.getLogger(EDTHandler.class);
	private String currentTag = "";
	private String currentEntityType = "";
	private JsonObject currentEntity;
	private final EDTImporter edtImporter;
	private boolean firstCours = true;
	private final boolean persEducNatOnly;

	public EDTHandler(EDTImporter edtImporter, boolean persEducNatOnly) {
		this.edtImporter = edtImporter;
		this.persEducNatOnly = persEducNatOnly;
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

		if (persEducNatOnly) {
			switch (localName) {
				case "Professeur":
				case "Personnel":
				case "Cours":
				case "Absence":
					currentEntityType = localName;
					currentEntity = o;
					break;
			}
		} else {
			switch (localName) {
				case "Cours":
					if (firstCours) {
						firstCours = false;
					} else {
						currentEntityType = localName;
						currentEntity = o;
					}
					break;
				case "Matiere":
				case "Eleve":
//				case "Professeur":
//				case "Personnel":
				case "Classe":
				case "Groupe":
				case "Salle":
				case "Materiel":
				case "GrilleHoraire":
				case "AnneeScolaire":
					currentEntityType = localName;
					currentEntity = o;
					break;
			}
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
//			if (currentEntity.containsKey("SemainesAnnulation")) {
//				log.info(currentEntity.encode());
//			}
			if (persEducNatOnly) {
				switch (localName) {
					case "Professeur":
						edtImporter.addProfesseur(currentEntity);
						break;
					case "Personnel":
						edtImporter.addPersonnel(currentEntity);
						break;
					case "Cours":
					case "Absence":
						break;
				}
			} else {
				switch (localName) {
					case "Cours":
						edtImporter.addCourse(currentEntity);
						break;
					case "Matiere":
						edtImporter.addSubject(currentEntity);
						break;
					case "Eleve":
						edtImporter.addEleve(currentEntity);
						break;
//					case "Professeur":
//						edtImporter.addProfesseur(currentEntity);
//						break;
					case "Classe":
						edtImporter.addClasse(currentEntity);
						break;
					case "Groupe":
						edtImporter.addGroup(currentEntity);
						break;
					case "Salle":
						edtImporter.addRoom(currentEntity);
						break;
					case "Materiel":
						edtImporter.addEquipment(currentEntity);
						break;
//					case "Personnel":
//						edtImporter.addPersonnel(currentEntity);
//						break;
					case "GrilleHoraire":
						edtImporter.initSchedule(currentEntity);
						break;
					case "AnneeScolaire":
						edtImporter.initSchoolYear(currentEntity);
						break;
				}
			}
			currentEntity = null;
		}
	}

}
