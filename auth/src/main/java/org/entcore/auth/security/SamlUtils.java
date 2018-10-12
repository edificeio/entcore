/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.auth.security;

import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.*;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.StringWriter;

public final class SamlUtils {

	public static final String SIMPLE_RS = "SimpleRS";

	public static Response unmarshallResponse(String response) throws Exception {
		Document document = getDocumentFromString(response);
		UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
		Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(document.getDocumentElement());
		if (unmarshaller == null) {
			throw new ValidationException("Error receiving unmarshaller for this document.");
		}
		return  (Response) unmarshaller.unmarshall(document.getDocumentElement());
	}

	private static Document getDocumentFromString(final String xmlContent) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		return documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));
	}

	public static String getIssuer(Response response) {
		if (response == null || response.getIssuer() == null) {
			return null;
		}
		return response.getIssuer().getValue();
	}

	public static String marshallAssertion(Assertion assertion) throws MarshallingException {
		AssertionMarshaller marshaller = new AssertionMarshaller();
		Element plaintextElement = marshaller.marshall(assertion);
		return XMLHelper.nodeToString(plaintextElement);
	}

	public static String marshallAuthnRequest(AuthnRequest authnRequest) throws MarshallingException {
		AuthnRequestMarshaller marshaller = new AuthnRequestMarshaller();
		Element plaintextElement = marshaller.marshall(authnRequest);
		return XMLHelper.nodeToString(plaintextElement);
	}

	public static Assertion unmarshallAssertion(String assertion) throws Exception {
		Document document = getDocumentFromString(assertion);
		AssertionUnmarshaller unmarshaller = new AssertionUnmarshaller();
		return (Assertion) unmarshaller.unmarshall(document.getDocumentElement());
	}


	public static <T> T buildSAMLObjectWithDefaultName(final Class<T> clazz)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

		QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
		T object = (T) builderFactory.getBuilder(defaultElementName)
				.buildObject(defaultElementName);
		return object;
	}

	public static String marshallLogoutRequest(LogoutRequest logoutRequest) throws MarshallingException {
		Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(logoutRequest);
		Element authDOM = marshaller.marshall(logoutRequest);
		StringWriter rspWrt = new StringWriter();
		XMLHelper.writeNode(authDOM, rspWrt);
		return rspWrt.toString();
	}

	public static String marshallNameId(NameID nameID) throws MarshallingException {
		NameIDMarshaller marshaller = new NameIDMarshaller();
		Element plaintextElement = marshaller.marshall(nameID);
		return XMLHelper.nodeToString(plaintextElement);
	}

	public static NameID unmarshallNameId(String nameId) throws Exception {
		Document document = getDocumentFromString(nameId);
		NameIDUnmarshaller unmarshaller = new NameIDUnmarshaller();
		return (NameID) unmarshaller.unmarshall(document.getDocumentElement());
	}

}
