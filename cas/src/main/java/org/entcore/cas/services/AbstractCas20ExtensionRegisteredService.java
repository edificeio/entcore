package org.entcore.cas.services;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public abstract class AbstractCas20ExtensionRegisteredService extends DefaultRegisteredService {

	protected static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";
	protected static final String CAS_PREFIX = "cas";

	@Override
	protected void prepareUser(User user, String userId, JsonObject data) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			doc.createAttributeNS(CAS_NAMESPACE, CAS_PREFIX);

			List<Element> additionnalAttributes = new ArrayList<Element>();
			user.setAdditionnalAttributes(additionnalAttributes);

			prepareUserCas20(user, userId, data, doc, additionnalAttributes);

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

	protected abstract void prepareUserCas20(User user, String userId, JsonObject data, Document doc, List<Element> additionnalAttributes);
}
