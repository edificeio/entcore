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
				a = new fr.wseduc.webutils.collections.JsonArray();
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
