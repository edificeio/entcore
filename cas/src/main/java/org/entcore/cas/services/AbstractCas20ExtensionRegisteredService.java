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

package org.entcore.cas.services;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.vertx.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public abstract class AbstractCas20ExtensionRegisteredService extends DefaultRegisteredService {

	protected static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";
	protected static final String CAS_PREFIX = "cas";

	@Override
	protected void prepareUser(User user, String userId, String service, JsonObject data) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			doc.createAttributeNS(CAS_NAMESPACE, CAS_PREFIX);

			List<Element> additionnalAttributes = new ArrayList<Element>();
			user.setAdditionnalAttributes(additionnalAttributes);

			prepareUserCas20(user, userId, service, data, doc, additionnalAttributes);

		} catch (ParserConfigurationException e) {
			log.error("Bad configuration for dom generator", e);
		}
	}

	protected Element createTextElement(String name, String value, Document doc) {
		Element element = doc.createElementNS(CAS_NAMESPACE, CAS_PREFIX + ":" + name);
		element.setTextContent(value);
		return element;
	}

	protected Element createElement(String name, Document doc) {
		Element element = doc.createElementNS(CAS_NAMESPACE, CAS_PREFIX + ":" + name);
		return element;
	}

	protected abstract void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes);
}
