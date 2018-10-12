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

package org.entcore.feeder.timetable.edt;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;



/**
 * <p>Classe Java pour anonymous complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="VERSION" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="LOGICIEL" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="CLES">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="PARTENAIRE" maxOccurs="2">
 *                     &lt;complexType>
 *                       &lt;simpleContent>
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *                           &lt;attribute name="NOM" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/extension>
 *                       &lt;/simpleContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="CONTENU">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>base64Binary">
 *                 &lt;attribute name="CompresseAvantChiffrement" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="VERIFICATION" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *         &lt;element name="DATEHEURE" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="UAI" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NOMETABLISSEMENT" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="CODEPOSTALVILLE" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"version",
		"logiciel",
		"cles",
		"contenu",
		"verification",
		"dateheure",
		"uai",
		"nometablissement",
		"codepostalville"
})
@XmlRootElement(name = "EXPORT_INDEX_EDUCATION")
public class EDTEncryptedExport {

	@XmlElement(name = "VERSION", required = true)
	protected String version;
	@XmlElement(name = "LOGICIEL", required = true)
	protected String logiciel;
	@XmlElement(name = "CLES", required = true)
	protected EDTEncryptedExport.CLES cles;
	@XmlElement(name = "CONTENU", required = true)
	protected EDTEncryptedExport.CONTENU contenu;
	@XmlElement(name = "VERIFICATION", required = true)
	protected byte[] verification;
	@XmlElement(name = "DATEHEURE", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar dateheure;
	@XmlElement(name = "UAI")
	protected String uai;
	@XmlElement(name = "NOMETABLISSEMENT")
	protected Object nometablissement;
	@XmlElement(name = "CODEPOSTALVILLE")
	protected Object codepostalville;

	/**
	 * Obtient la valeur de la propriété version.
	 *
	 * @return
	 *     possible object is
	 *     {@link String }
	 *
	 */
	public String getVERSION() {
		return version;
	}

	/**
	 * Définit la valeur de la propriété version.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *
	 */
	public void setVERSION(String value) {
		this.version = value;
	}

	/**
	 * Obtient la valeur de la propriété logiciel.
	 *
	 * @return
	 *     possible object is
	 *     {@link String }
	 *
	 */
	public String getLOGICIEL() {
		return logiciel;
	}

	/**
	 * Définit la valeur de la propriété logiciel.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *
	 */
	public void setLOGICIEL(String value) {
		this.logiciel = value;
	}

	/**
	 * Obtient la valeur de la propriété cles.
	 *
	 * @return
	 *     possible object is
	 *     {@link EDTEncryptedExport.CLES }
	 *
	 */
	public EDTEncryptedExport.CLES getCLES() {
		return cles;
	}

	/**
	 * Définit la valeur de la propriété cles.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link EDTEncryptedExport.CLES }
	 *
	 */
	public void setCLES(EDTEncryptedExport.CLES value) {
		this.cles = value;
	}

	/**
	 * Obtient la valeur de la propriété contenu.
	 *
	 * @return
	 *     possible object is
	 *     {@link EDTEncryptedExport.CONTENU }
	 *
	 */
	public EDTEncryptedExport.CONTENU getCONTENU() {
		return contenu;
	}

	/**
	 * Définit la valeur de la propriété contenu.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link EDTEncryptedExport.CONTENU }
	 *
	 */
	public void setCONTENU(EDTEncryptedExport.CONTENU value) {
		this.contenu = value;
	}

	/**
	 * Obtient la valeur de la propriété verification.
	 *
	 * @return
	 *     possible object is
	 *     byte[]
	 */
	public byte[] getVERIFICATION() {
		return verification;
	}

	/**
	 * Définit la valeur de la propriété verification.
	 *
	 * @param value
	 *     allowed object is
	 *     byte[]
	 */
	public void setVERIFICATION(byte[] value) {
		this.verification = value;
	}

	/**
	 * Obtient la valeur de la propriété dateheure.
	 *
	 * @return
	 *     possible object is
	 *     {@link XMLGregorianCalendar }
	 *
	 */
	public XMLGregorianCalendar getDATEHEURE() {
		return dateheure;
	}

	/**
	 * Définit la valeur de la propriété dateheure.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link XMLGregorianCalendar }
	 *
	 */
	public void setDATEHEURE(XMLGregorianCalendar value) {
		this.dateheure = value;
	}

	/**
	 * Obtient la valeur de la propriété uai.
	 *
	 * @return
	 *     possible object is
	 *     {@link String }
	 *
	 */
	public String getUAI() {
		return uai;
	}

	/**
	 * Définit la valeur de la propriété uai.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *
	 */
	public void setUAI(String value) {
		this.uai = value;
	}

	/**
	 * Obtient la valeur de la propriété nometablissement.
	 *
	 * @return
	 *     possible object is
	 *     {@link Object }
	 *
	 */
	public Object getNOMETABLISSEMENT() {
		return nometablissement;
	}

	/**
	 * Définit la valeur de la propriété nometablissement.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link Object }
	 *
	 */
	public void setNOMETABLISSEMENT(Object value) {
		this.nometablissement = value;
	}

	/**
	 * Obtient la valeur de la propriété codepostalville.
	 *
	 * @return
	 *     possible object is
	 *     {@link Object }
	 *
	 */
	public Object getCODEPOSTALVILLE() {
		return codepostalville;
	}

	/**
	 * Définit la valeur de la propriété codepostalville.
	 *
	 * @param value
	 *     allowed object is
	 *     {@link Object }
	 *
	 */
	public void setCODEPOSTALVILLE(Object value) {
		this.codepostalville = value;
	}


	/**
	 * <p>Classe Java pour anonymous complex type.
	 *
	 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="PARTENAIRE" maxOccurs="2">
	 *           &lt;complexType>
	 *             &lt;simpleContent>
	 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
	 *                 &lt;attribute name="NOM" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
	 *               &lt;/extension>
	 *             &lt;/simpleContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
			"partenaire"
	})
	public static class CLES {

		@XmlElement(name = "PARTENAIRE", required = true)
		protected List<EDTEncryptedExport.CLES.PARTENAIRE> partenaire;

		/**
		 * Gets the value of the partenaire property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list,
		 * not a snapshot. Therefore any modification you make to the
		 * returned list will be present inside the JAXB object.
		 * This is why there is not a <CODE>set</CODE> method for the partenaire property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 * <pre>
		 *    getPARTENAIRE().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list
		 * {@link EDTEncryptedExport.CLES.PARTENAIRE }
		 *
		 *
		 */
		public List<EDTEncryptedExport.CLES.PARTENAIRE> getPARTENAIRE() {
			if (partenaire == null) {
				partenaire = new ArrayList<EDTEncryptedExport.CLES.PARTENAIRE>();
			}
			return this.partenaire;
		}


		/**
		 * <p>Classe Java pour anonymous complex type.
		 *
		 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
		 *
		 * <pre>
		 * &lt;complexType>
		 *   &lt;simpleContent>
		 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
		 *       &lt;attribute name="NOM" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
		 *     &lt;/extension>
		 *   &lt;/simpleContent>
		 * &lt;/complexType>
		 * </pre>
		 *
		 *
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "", propOrder = {
				"value"
		})
		public static class PARTENAIRE {

			@XmlValue
			protected String value;
			@XmlAttribute(name = "NOM", required = true)
			protected String nom;

			/**
			 * Obtient la valeur de la propriété value.
			 *
			 * @return
			 *     possible object is
			 *     {@link String }
			 *
			 */
			public String getValue() {
				return value;
			}

			/**
			 * Définit la valeur de la propriété value.
			 *
			 * @param value
			 *     allowed object is
			 *     {@link String }
			 *
			 */
			public void setValue(String value) {
				this.value = value;
			}

			/**
			 * Obtient la valeur de la propriété nom.
			 *
			 * @return
			 *     possible object is
			 *     {@link String }
			 *
			 */
			public String getNOM() {
				return nom;
			}

			/**
			 * Définit la valeur de la propriété nom.
			 *
			 * @param value
			 *     allowed object is
			 *     {@link String }
			 *
			 */
			public void setNOM(String value) {
				this.nom = value;
			}

		}

	}


	/**
	 * <p>Classe Java pour anonymous complex type.
	 *
	 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;simpleContent>
	 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>base64Binary">
	 *       &lt;attribute name="CompresseAvantChiffrement" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
	 *     &lt;/extension>
	 *   &lt;/simpleContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
			"value"
	})
	public static class CONTENU {

		@XmlValue
		protected byte[] value;
		@XmlAttribute(name = "CompresseAvantChiffrement")
		protected Boolean compresseAvantChiffrement;

		/**
		 * Obtient la valeur de la propriété value.
		 *
		 * @return
		 *     possible object is
		 *     byte[]
		 */
		public byte[] getValue() {
			return value;
		}

		/**
		 * Définit la valeur de la propriété value.
		 *
		 * @param value
		 *     allowed object is
		 *     byte[]
		 */
		public void setValue(byte[] value) {
			this.value = value;
		}

		/**
		 * Obtient la valeur de la propriété compresseAvantChiffrement.
		 *
		 * @return
		 *     possible object is
		 *     {@link Boolean }
		 *
		 */
		public boolean isCompresseAvantChiffrement() {
			if (compresseAvantChiffrement == null) {
				return true;
			} else {
				return compresseAvantChiffrement;
			}
		}

		/**
		 * Définit la valeur de la propriété compresseAvantChiffrement.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link Boolean }
		 *
		 */
		public void setCompresseAvantChiffrement(Boolean value) {
			this.compresseAvantChiffrement = value;
		}

	}

}