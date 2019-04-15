/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.user;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserInfos {

	public static class Action {
		private String name;
		private String displayName;
		private String type;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

	public static class Child {
		private String firstName;
		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

	public static class Application {
		private String name;
		private String address;
		private String icon;
		private String target;
		private String displayName;
		private boolean display;
		private String prefix;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public String getDisplayName() { return displayName; }

		public void setDisplayName(String displayName) { this.displayName = displayName; }

		public boolean isDisplay() {
			return display;
		}

		public void setDisplay(boolean display) {
			this.display = display;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}
	}

	public static class Widget {
		private String id;
		private String name;
		private String path;
		private String js;
		private String i18n;
		private String application;
		private boolean mandatory;

		public String getId(){
			return id;
		}
		public void setId(String id){
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getJs() {
			return js;
		}
		public void setJs(String js) {
			this.js = js;
		}
		public String getI18n() {
			return i18n;
		}
		public void setI18n(String i18n) {
			this.i18n = i18n;
		}
		public String getApplication() {
			return application;
		}
		public void setApplication(String application) {
			this.application = application;
		}
		public boolean isMandatory() {
			return mandatory;
		}
		public void setMandatory(boolean mandatory) {
			this.mandatory = mandatory;
		}
	}

	public static class Function {
		private String code;
		private String functionName;
		private List<String> scope;
		private List<String> structureExternalIds;
		private Map<String, Subject> subjects;

		public List<String> getScope() {
			return scope;
		}

		public void setScope(List<String> scope) {
			this.scope = scope;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getFunctionName() {
			return functionName;
		}

		public void setFunctionName(String functionName) {
			this.functionName = functionName;
		}

		public Map<String, Subject> getSubjects() {
			return subjects;
		}

		public void setSubjects(Map<String, Subject> subjects) {
			this.subjects = subjects;
		}

		public List<String> getStructureExternalIds() {
			return structureExternalIds;
		}

		public void setStructureExternalIds(List<String> structureExternalIds) {
			this.structureExternalIds = structureExternalIds;
		}
	}
	private Boolean hasApp;
	private String userId;
	private String externalId;
	private String firstName;
	private String lastName;
	private String username;
	private String birthDate;
	private List<String> classNames;
	private List<String> realClassNames;
	private List<String> structureNames;
	private List<String> uai;
	private List<String> childrenIds;
	private Map<String, Child> children;
	private String level;
	private String type;
	private String login;
	private List<Action> authorizedActions;
	private List<Application> apps;
	private List<String> groupsIds;
	private List<String> classes;
	private List<String> structures;
	private Map<String, Object> cache;
	private Boolean federated;
	private List<Widget> widgets;
	private Map<String, Object> otherProperties = new HashMap<>();

	public Map<String, Child> getChildren() { return children; }

	public void setChildren(Map<String, Child> children) { this.children = children; }

	public Boolean getHasApp(){
		return this.hasApp;
	}

	public void setHasApp(Boolean hasApp){
		this.hasApp = hasApp;
	}

	public Map<String, Function> getFunctions() {
		return functions;
	}

	public void setFunctions(Map<String, Function> functions) {
		this.functions = functions;
	}

	private Map<String, Function> functions;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public List<Action> getAuthorizedActions() {
		return authorizedActions;
	}

	public void setAuthorizedActions(List<Action> authorizedActions) {
		this.authorizedActions = authorizedActions;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	// keep only for compatibility with other modules
	@Deprecated
	public List<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(List<String> classNames) {
		this.classNames = classNames;
	}

	public List<String> getRealClassNames() {
		return realClassNames;
	}

	public void setRealClassNames(List<String> realClassNames) {
		this.realClassNames = realClassNames;
	}

	public List<String> getStructureNames() {
		return structureNames;
	}

	public void setStructureNames(List<String> structureNames) {
		this.structureNames = structureNames;
	}

	public List<String> getUai() {
		return uai;
	}

	public void setUai(List<String> uai) {
		this.uai = uai;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<Application> getApps() {
		return apps;
	}

	public void setApps(List<Application> apps) {
		this.apps = apps;
	}

	public List<String> getGroupsIds() {
		return groupsIds;
	}

	// keep only for compatibility with other modules
	@Deprecated
	public List<String> getProfilGroupsIds() {
		return groupsIds;
	}

	@Deprecated
	public void setProfilGroupsIds(List<String> profilGroupsIds) {
		this.groupsIds = profilGroupsIds;
	}

	public void setGroupsIds(List<String> groupsIds) {
		this.groupsIds = groupsIds;
	}

	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}

	public Map<String, Object> getCache() {
		return cache;
	}

	public void setCache(Map<String, Object> cache) {
		this.cache = cache;
	}

	public Object getAttribute(String attribute) {
		if (cache != null) {
			return cache.get(attribute);
		}
		return null;
	}

	public List<String> getStructures() {
		return structures;
	}

	public void setStructures(List<String> structures) {
		this.structures = structures;
	}

	public List<String> getChildrenIds() {
		return childrenIds;
	}

	public void setChildrenIds(List<String> childrenIds) {
		this.childrenIds = childrenIds;
	}

	public Boolean getFederated() {
		return federated;
	}

	public void setFederated(Boolean federated) {
		this.federated = federated;
	}

	public List<Widget> getWidgets() {
		return widgets;
	}

	public void setWidgets(List<Widget> widgets) {
		this.widgets = widgets;
  }

	@JsonAnySetter
	public void setOtherProperty(String key, Object value) {
		otherProperties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getOtherProperties() {
		return otherProperties;
	}

	public static class Subject {
		private String subjectCode;
		private String subjectName;
		private List<String> scope;
		private List<String> structureExternalIds;

		public String getSubjectCode() {
			return subjectCode;
		}

		public void setSubjectCode(String subjectCode) {
			this.subjectCode = subjectCode;
		}

		public String getSubjectName() {
			return subjectName;
		}

		public void setSubjectName(String subjectName) {
			this.subjectName = subjectName;
		}

		public List<String> getScope() {
			return scope;
		}

		public void setScope(List<String> scope) {
			this.scope = scope;
		}

		public List<String> getStructureExternalIds() {
			return structureExternalIds;
		}

		public void setStructureExternalIds(List<String> structureExternalIds) {
			this.structureExternalIds = structureExternalIds;
		}
	}

}
