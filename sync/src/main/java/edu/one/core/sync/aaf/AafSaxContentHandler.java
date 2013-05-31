package edu.one.core.sync.aaf;

import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.infra.TracerHelper;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author bperez
 */
public class AafSaxContentHandler extends DefaultHandler {
	private TracerHelper trace;
	public Operation oc;
	public List<Operation> operations;
	public List<Operation> operationsInvalides;
	private long startDoc;
	private long endDoc;
	private Dictionary d;

	public AafSaxContentHandler(TracerHelper trace, Dictionary d) {
		this.trace = trace;
		this.d = d;
		operations = new ArrayList<>();
		operationsInvalides = new ArrayList<>();
	}

	public void reset() {
		operations = new ArrayList<>();
		operationsInvalides = new ArrayList<>();
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		startDoc = System.currentTimeMillis();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		endDoc = System.currentTimeMillis();
		trace.info("Traitement du document = " + (endDoc - startDoc) + " ms");
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

		String valeur = new String(ch, start, length).trim();
		switch (oc.etatAvancement) {
			case TYPE_ENTITE :
				oc.typeEntite = Operation.TypeEntite.valueOf(valeur.toUpperCase());
			case ID :
				oc.id = valeur;
				if (oc.typeEntite.equals(Operation.TypeEntite.ETABEDUCNAT)) {
					oc.creerAttributCourant(AafConstantes.STRUCTURE_JOINTURE_ATTR);
				} else {
					oc.creerAttributCourant(AafConstantes.PERSONNE_JOINTURE_ATTR);
				}
				oc.ajouterValeur(AafConstantes.TYPE_IMPORT + "-" + oc.id);
				oc.ajouterAttributCourant();
			case ATTRIBUTS :
				if (Operation.EtatAvancement.ATTRIBUTS.equals(oc.etatAvancement)) {
					oc.ajouterValeur(valeur);
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
				// gestion du profil
				oc.creerAttributCourant(AafConstantes.PERSONNE_PROFIL_ATTR);
				switch (oc.typeEntite) {
					case ELEVE :
						oc.ajouterValeur(AafConstantes.PROFIL_ELEVE);
						break;
					case PERSRELELEVE :
						oc.ajouterValeur(AafConstantes.PROFIL_PARENT);
						break;
					case PERSEDUCNAT :
						if (oc.attributs.containsKey(AafConstantes.CLASSES_ATTR)) {
							oc.ajouterValeur(AafConstantes.PROFIL_ENSEIGNANT);
						} else {
							oc.ajouterValeur(AafConstantes.PROFIL_NON_ENSEIGNANT);
						}
						break;
					default:
						break;
				}
				oc.ajouterAttributCourant();

				// Attributs générés par le dictionnaire de données
				oc.attributs = d.generateField(oc.attributs);
				// Contrôle de validité de l'opération
				if (d.validateFieldsList(oc.attributs).containsValue(false)) {
					operationsInvalides.add(oc);
				} else {
					operations.add(oc);
				}
				break;
			case AafConstantes.UPDATE_TAG :
				// TODO : action de mise à jour
				break;
			case AafConstantes.DELETE_TAG :
				// TODO : action de suppression
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