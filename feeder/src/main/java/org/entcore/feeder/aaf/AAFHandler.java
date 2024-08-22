/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.utils.JsonUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;
import java.util.List;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.feeder.utils.AAFUtil.convertDate;

public final class AAFHandler extends DefaultHandler {

	private String currentTag = "";
	private String currentAttribute = "";
	private StringBuilder s;
	private JsonObject currentStructure;
	private final JsonObject mapping;
	private final ImportProcessing processing;
	private final List<String> allowEmptyUpdate = Arrays.asList(
			"ENTAuxEnsClassesPrincipal", "mobile", "ENTPersonMobileSMS", "ENTPersonAdresse",
			"ENTPersonCodePostal", "ENTPersonVille", "ENTPersonPays", "ENTAuxEnsMEF", "ENTEleveMEF",
			"ENTEleveLibelleMEF", "ENTEleveCodeEnseignements", "ENTEleveEnseignements");

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
		if (s == null || (s.isEmpty() && !allowEmptyUpdate.contains(currentAttribute))) {
			return;
		}
		JsonObject j = mapping.getJsonObject(currentAttribute);
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
			JsonArray a = currentStructure.getJsonArray(attribute);
			if (a == null) {
				a = new fr.wseduc.webutils.collections.JsonArray();
				currentStructure.put(attribute, a);
			}
			if (!s.isEmpty()) {
				a.add(JsonUtil.convert(s, type, (prefix ? processing.getAcademyPrefix() : null)));
			}
		} else {
			Object v = JsonUtil.convert(s, type, (prefix ? processing.getAcademyPrefix() : null));
			if (!(v instanceof JsonUtil.None)) {
				currentStructure.put(attribute, v);
			}
		}
	}

	private void addExternalId(String s) throws SAXException {
		if (currentStructure != null) {
			currentStructure.put("externalId",
					(isNotEmpty(processing.getAcademyPrefix()) ? processing.getAcademyPrefix() + s : s));
		} else {
			throw new SAXException("Id is found but structure isn't defined.");
		}
	}

}
