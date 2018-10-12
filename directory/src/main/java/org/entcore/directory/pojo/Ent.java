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

package org.entcore.directory.pojo;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="etablissement" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="etablissement_id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="etablissement_uid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="code_porteur" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="nom_courant" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="adresse_plus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="code_postal" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="ville" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="telephone" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="fax" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
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
		"etablissement"
})
@XmlRootElement(name = "ent")
public class Ent {

	@XmlElement(required = true)
	protected List<Ent.Etablissement> etablissement;

	/**
	 * Gets the value of the etablissement property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the etablissement property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getEtablissement().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link Ent.Etablissement }
	 *
	 *
	 */
	public List<Ent.Etablissement> getEtablissement() {
		if (etablissement == null) {
			etablissement = new ArrayList<Ent.Etablissement>();
		}
		return this.etablissement;
	}


	/**
	 * <p>Java class for anonymous complex type.
	 *
	 * <p>The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="etablissement_id" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="etablissement_uid" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="code_porteur" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="nom_courant" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="adresse_plus" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="code_postal" type="{http://www.w3.org/2001/XMLSchema}int"/>
	 *         &lt;element name="ville" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="telephone" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="fax" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
			"etablissementId",
			"etablissementUid",
			"codePorteur",
			"nomCourant",
			"adressePlus",
			"codePostal",
			"ville",
			"telephone",
			"fax"
	})
	public static class Etablissement {

		@XmlElement(name = "etablissement_id", required = true)
		protected String etablissementId;
		@XmlElement(name = "etablissement_uid", required = true)
		protected String etablissementUid;
		@XmlElement(name = "code_porteur", required = true)
		protected String codePorteur;
		@XmlElement(name = "nom_courant", required = true)
		protected String nomCourant;
		@XmlElement(name = "adresse_plus", required = true)
		protected String adressePlus;
		@XmlElement(name = "code_postal")
		protected String codePostal;
		@XmlElement(required = true)
		protected String ville;
		@XmlElement(required = true)
		protected String telephone;
		@XmlElement(required = true)
		protected String fax;

		/**
		 * Gets the value of the etablissementId property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getEtablissementId() {
			return etablissementId;
		}

		/**
		 * Sets the value of the etablissementId property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setEtablissementId(String value) {
			this.etablissementId = value;
		}

		/**
		 * Gets the value of the etablissementUid property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getEtablissementUid() {
			return etablissementUid;
		}

		/**
		 * Sets the value of the etablissementUid property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setEtablissementUid(String value) {
			this.etablissementUid = value;
		}

		/**
		 * Gets the value of the codePorteur property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getCodePorteur() {
			return codePorteur;
		}

		/**
		 * Sets the value of the codePorteur property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setCodePorteur(String value) {
			this.codePorteur = value;
		}

		/**
		 * Gets the value of the nomCourant property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getNomCourant() {
			return nomCourant;
		}

		/**
		 * Sets the value of the nomCourant property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setNomCourant(String value) {
			this.nomCourant = value;
		}

		/**
		 * Gets the value of the adressePlus property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getAdressePlus() {
			return adressePlus;
		}

		/**
		 * Sets the value of the adressePlus property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setAdressePlus(String value) {
			this.adressePlus = value;
		}

		/**
		 * Gets the value of the codePostal property.
		 *
		 */
		public String getCodePostal() {
			return codePostal;
		}

		/**
		 * Sets the value of the codePostal property.
		 *
		 */
		public void setCodePostal(String value) {
			this.codePostal = value;
		}

		/**
		 * Gets the value of the ville property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getVille() {
			return ville;
		}

		/**
		 * Sets the value of the ville property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setVille(String value) {
			this.ville = value;
		}

		/**
		 * Gets the value of the telephone property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getTelephone() {
			return telephone;
		}

		/**
		 * Sets the value of the telephone property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setTelephone(String value) {
			this.telephone = value;
		}

		/**
		 * Gets the value of the fax property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getFax() {
			return fax;
		}

		/**
		 * Sets the value of the fax property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setFax(String value) {
			this.fax = value;
		}

	}

}