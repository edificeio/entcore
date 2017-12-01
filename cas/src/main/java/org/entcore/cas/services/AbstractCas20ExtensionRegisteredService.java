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
