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
import javax.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="user">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="externalId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="login" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="firstName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="lastName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"user"
})
@XmlRootElement(name = "users")
public class Users {

	protected List<Users.User> user;

	public Users() {}

	public Users(List<Users.User> user) {
		this.user = user;
	}

	/**
	 * Gets the value of the user property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the user property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getUser().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link Users.User }
	 *
	 *
	 */
	public List<Users.User> getUser() {
		if (user == null) {
			user = new ArrayList<Users.User>();
		}
		return this.user;
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
	 *         &lt;element name="externalId" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="login" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="firstName" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="lastName" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="emailAcademy" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="mobile" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="deleteDate" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="functions" type="{http://www.w3.org/2001/XMLSchema}string[]"/>
	 *         &lt;element name="displayName" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="profiles" type="{http://www.w3.org/2001/XMLSchema}string"/>
	 *         &lt;element name="classes" type="{http://www.w3.org/2001/XMLSchema}string[]"/>
	 *         &lt;element name="structures" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
			"externalId",
			"login",
			"firstName",
			"lastName",
			"email",
			"emailAcademy",
			"mobile",
			"deleteDate",
			"functions",
			"displayName",
			"profiles",
			"classes",
			"structures",
			"administrativeStructure"
	})
	public static class User {

		@XmlElement(required = true)
		protected String externalId;
		@XmlElement(required = true)
		protected String login;
		@XmlElement(required = true)
		protected String firstName;
		@XmlElement(required = true)
		protected String lastName;
		@XmlElement(required = false)
		protected String email;
		@XmlElement(required = false)
		protected String emailAcademy;
		@XmlElement(required = false)
		protected String mobile;
		@XmlElement(required = false)
		protected String deleteDate;
		@XmlElementWrapper(name="functions")
		@XmlElement(name="function", required = false)
		protected List<String> functions;
		@XmlElement(required = false)
		protected String displayName;
		@XmlElement(required = false)
		protected String profiles;
		@XmlElementWrapper(name="classes")
		@XmlElement(name="classe", required = false)
		protected List<String> classes;
		@XmlElement(required = false)
		protected String structures;
		@XmlElement(required = false)
		protected String administrativeStructure;

		/**
		 * Gets the value of the externalId property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getExternalId() {
			return externalId;
		}

		/**
		 * Sets the value of the externalId property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setExternalId(String value) {
			this.externalId = value;
		}

		/**
		 * Gets the value of the login property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getLogin() {
			return login;
		}

		/**
		 * Sets the value of the login property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setLogin(String value) {
			this.login = value;
		}

		/**
		 * Gets the value of the firstName property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getFirstName() {
			return firstName;
		}

		/**
		 * Sets the value of the firstName property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setFirstName(String value) {
			this.firstName = value;
		}

		/**
		 * Gets the value of the lastName property.
		 *
		 * @return
		 *     possible object is
		 *     {@link String }
		 *
		 */
		public String getLastName() {
			return lastName;
		}

		/**
		 * Sets the value of the lastName property.
		 *
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 *
		 */
		public void setLastName(String value) {
			this.lastName = value;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getEmailAcademy() {
			return emailAcademy;
		}

		public void setEmailAcademy(String emailAcademy) {
			this.emailAcademy = emailAcademy;
		}

		public String getMobile() {
			return mobile;
		}

		public void setMobile(String mobile) {
			this.mobile = mobile;
		}

		public String getDeleteDate() {
			return deleteDate;
		}

		public void setDeleteDate(String deleteDate) {
			this.deleteDate = deleteDate;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public List<String> getFunctions() {
			return functions;
		}

		public void setFunctions(List<String> functions) {
			this.functions = functions;
		}

		public String getProfiles() {
			return profiles;
		}

		public void setProfiles(String profiles) {
			this.profiles = profiles;
		}

		public List<String> getclasses() {
			return classes;
		}

		public void setClasses(List<String> classes) {
			this.classes = classes;
		}
		public String getStructures() {
			return structures;
		}

		public void setStructures(String structures) {
			this.structures = structures;
		}

		public String getAdministrativeStructure() {
			return administrativeStructure;
		}

		public void setAdministrativeStructure(String administrativeStructure) {
			this.administrativeStructure = administrativeStructure;
		}

	}

}
