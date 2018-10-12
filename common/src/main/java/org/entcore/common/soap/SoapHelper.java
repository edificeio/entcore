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

package org.entcore.common.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.entcore.common.soap.SoapHelper.SoapDescriptor.*;

public class SoapHelper {

	private static class Triplet<T, T2, T3>{
		public T _1; public T2 _2; public T3 _3;
		public Triplet(T a, T2 b, T3 c){
			this._1 = a; this._2 = b; this._3 = c;
		}
		public String toString(){
			return "<"+_1+","+_2+","+_3+">";
		}
	}
	private static class Pair<T, T2>{
		public T _1; public T2 _2;
		public Pair(T a, T2 b){
			this._1 = a; this._2 = b;
		}
		public String toString(){
			return "<"+_1+","+_2+">";
		}
	}

	/**
	 * Describes a SOAP RPC message.
	 */
	public static class SoapDescriptor{

		/**
		 * Tag attribute.
		 */
		public static class Attribute{
			private final String name, value, prefix, uri;

			/**
			 * Create a new tag attribute.
			 *
			 * @param name Attribute name.
			 * @param value Attribute value.
			 * @param prefix Namespace prefix.
			 * @param uri Namespace URI.
			 */
			public Attribute(String name, String value, String prefix, String uri){
				this.name = name;
				this.value = value;
				this.prefix = prefix;
				this.uri = uri;
			}
		}

		/**
		 * Soap element.
		 */
		public static class Element{
			private final String name, prefix, uri, text;
			private final ArrayList<Attribute> attributes = new ArrayList<>();
			private final ArrayList<Element> elements = new ArrayList<>();

			/**
			 * Create a new soap element.
			 * @param name Tag name.
			 * @param text Tag text contents.
			 * @param prefix Namespace prefix.
			 * @param uri Namespace URI.
			 */
			public Element(String name, String text, String prefix, String uri){
				this.name = name;
				this.prefix = prefix;
				this.uri = uri;
				this.text = text;
			}

			/**
			 * Add a new tag attribute.
			 *
			 * @param name Attribute name.
			 * @param value Attribute value.
			 * @return This element.
			 */
			public Element addAttribute(String name, String value){
				return addAttribute(name, value, "", "");
			}
			/**
			 * Add a new tag attribute.
			 *
			 * @param name Attribute name.
			 * @param value Attribute value.
			 * @param prefix Namespace prefix.
			 * @param uri Namespace URI.
			 * @return New element.
			 */
			public Element addAttribute(String name, String value, String prefix, String uri){
				attributes.add(new Attribute(name, value, "", ""));
				return this;
			}

			/**
			 * Creates a new child element.
			 *
			 * @param name Element name.
			 * @param text Element text contents.
			 * @return New element.
			 */
			public Element createElement(String name, String text){
				return createElement(name, text, "", "");
			}
			/**
			 * Creates a new child element.
			 *
			 * @param name Element name.
			 * @param text Element text contents.
			 * @param prefix Namespace prefix.
			 * @param uri Namespace URI.
			 * @return New element.
			 */
			public Element createElement(String name, String text, String prefix, String uri){
				Element e = new Element(name, text, prefix, uri);
				elements.add(e);
				return e;
			}

		}

		private Triplet<String, String, String> bodyTag;
		private final ArrayList<Element> elements = new ArrayList<>();
		private final ArrayList<Pair<String, String>> namespaces = new ArrayList<>();

		/**
		 * Creates a new descriptor.
		 * @param method RPC method name.
		 */
		public SoapDescriptor(String method){
			bodyTag = new Triplet<>("", method, "");
		}

		/**
		 * Sets the body tag namespace values.
		 *
		 * @param namespaceUri Namespace URI value.
		 * @param namespacePrefix Namespace prefix value.
		 * @return Itself
		 */
		public SoapDescriptor setBodyNamespace(String namespaceUri, String namespacePrefix){
			bodyTag._1 = namespaceUri;
			bodyTag._3 = namespacePrefix;
			return this;
		}

		/**
		 * Creates a new child element.
		 *
		 * @param name Element name.
		 * @param text Element text contents.
		 * @return New element.
		 */
		public Element createElement(String name, String text){
			return createElement(name, text, "", "");
		}
		/**
		 * Creates a new child element.
		 *
		 * @param name Element name.
		 * @param text Element text contents.
		 * @param prefix Namespace prefix.
		 * @param uri Namespace URI.
		 * @return New element.
		 */
		public Element createElement(String name, String text, String prefix, String uri){
			Element e = new Element(name, text, prefix, uri);
			elements.add(e);
			return e;
		}

		/**
		 * Add a new spacespace declaration.
		 *
		 * @param prefix Namespace prefix.
		 * @param uri Namespace URI.
		 * @return This descriptor.
		 */
		public SoapDescriptor addNamespace(String prefix, String uri){
			namespaces.add(new Pair<>(prefix, uri));
			return this;
		}

		/**
		 * Returns the body tag name.
		 */
		public String getBodyTagName(){
			return this.bodyTag._2;
		}

		/**
		 * Returns the full body tag name & namespace.
		 */
		public String getBodyTag(){
			return this.bodyTag._3+":"+bodyTag._1+"#"+bodyTag._2;
		}

		/**
		 * Returns the soap elements.
		 */
		public ArrayList<Element> getElements(){
			return this.elements;
		}

	}

	/**
	 * Creates a new SOAP RPC message from a descriptor and returns it as an UTF-8 encoded string.
	 * @param messageDescriptor Descriptor object.
	 * @return An encoded UTF-8 String containing the SOAP message.
	 * @throws SOAPException when the message was badly constructed.
	 * @throws IOException if the String encoding of the message failed.
	 */
	public static String createSoapMessage(SoapDescriptor messageDescriptor) throws SOAPException, IOException{

		MessageFactory 	mf = MessageFactory.newInstance();
		SOAPMessage 	msg = mf.createMessage();
		SOAPPart 		part = msg.getSOAPPart();
		SOAPEnvelope 	env = part.getEnvelope();
		SOAPBody 		body = msg.getSOAPBody();

		//Useless header
		msg.getSOAPHeader().detachNode();

		//Adding envelope namespaces
		env.addNamespaceDeclaration("xsd","http://www.w3.org/2001/XMLSchema");
		env.addNamespaceDeclaration("xsi","http://www.w3.org/2001/XMLSchema-instance");
		env.addNamespaceDeclaration("soapenv","http://schemas.xmlsoap.org/soap/envelop/");
		for(Pair<String, String> namespace: messageDescriptor.namespaces){
			env.addNamespaceDeclaration(namespace._1, namespace._2);
		}
		env.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

		//Populating the body
		QName bodyName = new QName(messageDescriptor.bodyTag._1, messageDescriptor.bodyTag._2, messageDescriptor.bodyTag._3);
		SOAPBodyElement messageElement = body.addBodyElement(bodyName);

		//Adding elements
		LinkedBlockingQueue<Pair<Element, SOAPElement>> elementQueue = new LinkedBlockingQueue<>();
		for(Element e : messageDescriptor.elements)
			elementQueue.add(new Pair<Element, SOAPElement>(e, messageElement));

		while(elementQueue.size() > 0){
			Pair<Element, SOAPElement> current = elementQueue.remove();
			Element currentElement = current._1;
			QName name = new QName(currentElement.uri, currentElement.name, currentElement.prefix);
			SOAPElement node = current._2.addChildElement(name);
			node.setTextContent(currentElement.text);

			for(Element e : currentElement.elements)
				elementQueue.add(new Pair<Element, SOAPElement>(e, node));

			for(Attribute attribute : currentElement.attributes){
				node.addAttribute(new QName(attribute.uri, attribute.name, attribute.prefix), attribute.value);
			}

		}

		//Exporting as a String object
		ByteArrayOutputStream byteOutput =  new ByteArrayOutputStream();
		msg.writeTo(byteOutput);
		return new String(byteOutput.toByteArray(), "UTF-8");
	}

}
