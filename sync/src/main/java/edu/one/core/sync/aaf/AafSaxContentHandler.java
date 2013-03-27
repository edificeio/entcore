package edu.one.core.sync.aaf;

import edu.one.core.infra.Neo;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author bperez
 */
public class AafSaxContentHandler extends DefaultHandler {
	private Logger log;
	public Operation oc;
	public StringBuffer operationsBuf;
	private Neo neo;
	public int total = 0;
	long startDoc;
	long endDoc;

	public AafSaxContentHandler(Logger log, EventBus eb) {
		this.log = log;
		this.neo = new Neo(eb, log);
		operationsBuf = new StringBuffer();

	}

	public int reset() {
		operationsBuf = new StringBuffer();
		int ret = total;
		total = 0;
		return ret;
	}

	public void sendRequest() {
		neo.sendMultiple(operationsBuf.toString(), AafConstantes.REQUEST_SEPARATOR,
							AafConstantes.ATTR_SEPARATOR, AafConstantes.VALUE_SEPARATOR);
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		startDoc = System.currentTimeMillis();
		operationsBuf = new StringBuffer();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		sendRequest();
		endDoc = System.currentTimeMillis();
		log.debug("Traitement du document = " + (endDoc - startDoc) + " ms");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		switch (qName) {
			case AafConstantes.ADD_TAG :
				oc = new Operation(qName);
				break;
			case AafConstantes.UPDATE_TAG :
				oc = new Operation(qName);
				break;
			case AafConstantes.DELETE_TAG :
				oc = new Operation(qName);
				break;
			case AafConstantes.OPERATIONAL_ATTRIBUTES_TAG :
				oc.etatAvancement = oc.etatAvancement.suivant();
				break;
			case AafConstantes.IDENTIFIER_TAG :
				oc.etatAvancement = oc.etatAvancement.suivant();
				break;
			case AafConstantes.ATTR_TAG :
				if (Operation.EtatAvancement.ATTRIBUTS.equals(oc.etatAvancement)) {
					oc.creerAttributCourant((String)attributes.getValue(AafConstantes.ATTRIBUTE_NAME_INDEX));
				}
				break;
			case AafConstantes.ATTRIBUTES_TAG :
				oc.etatAvancement = oc.etatAvancement.suivant();
				break;
			default :
				break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);

		String valeur = new String(ch, start, length);
		switch (oc.etatAvancement) {
			case TYPE_ENTITE :
				oc.typeEntite = Operation.TypeEntite.valueOf(valeur.toUpperCase());
			case ID :
				oc.id = valeur;
			case ATTRIBUTS :
				if (Operation.EtatAvancement.ATTRIBUTS.equals(oc.etatAvancement)) {
					oc.ajouterValeur(valeur.replaceAll("\\*", "-"));
				}
			default :
				break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);

		switch (qName) {
			case AafConstantes.ADD_TAG :
				total++;
				operationsBuf.append(oc.attributsBuf.toString()).append(AafConstantes.REQUEST_SEPARATOR);
				break;
			case AafConstantes.UPDATE_TAG :
				total++;
				operationsBuf.append(oc.attributsBuf.toString()).append(AafConstantes.REQUEST_SEPARATOR);
				break;
			case AafConstantes.DELETE_TAG :
				total++;
				operationsBuf.append(oc.attributsBuf.toString()).append(AafConstantes.REQUEST_SEPARATOR);
				break;
			case AafConstantes.ATTR_TAG :
				if (Operation.EtatAvancement.ATTRIBUTS.equals(oc.etatAvancement)) {
					oc.ajouterAttributCourant();
				}
				break;
			default :
				break;
		}
	}
}