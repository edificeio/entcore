/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.auth.security;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.saml2.core.impl.AssertionUnmarshaller;
import org.opensaml.saml2.core.impl.AuthnRequestMarshaller;
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

}
