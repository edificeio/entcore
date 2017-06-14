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
			"structures",
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
		@XmlElement(required = false)
		protected String structures;

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

		public String getStructures() {
			return structures;
		}

		public void setStructures(String structures) {
			this.structures = structures;
		}

	}

}
