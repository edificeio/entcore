/*
 * Copyright © "Open Digital Education", 2016
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
				a = new fr.wseduc.webutils.collections.JsonArray();
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
