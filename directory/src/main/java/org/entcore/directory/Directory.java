/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.directory;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import fr.wseduc.webutils.validation.JsonSchemaValidator;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.directory.controllers.*;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import org.entcore.directory.security.DirectoryResourcesProvider;
import org.entcore.directory.services.impl.UserbookRepositoryEvents;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Directory extends Server {

	public static final String FEEDER = "entcore.feeder";

	@Override
	public void start() {
		super.start();
		Neo4j.getInstance().init(getEventBus(vertx),
				config.getString("neo4j-address", "wse.neo4j.persistor"));
		JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
		validator.setAddress("json.schema.validator");
		validator.setEventBus(getEventBus(vertx));
		validator.loadJsonSchema(getPathPrefix(config), vertx);

		rm.get("/userbook/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Renders.renderJson(request, I18n.getInstance().load(request.headers().get("Accept-Language")));
			}
		});

		DirectoryController directoryController = new DirectoryController(vertx, container, rm, securedActions, config);
		UserBookController userBookController = new UserBookController(vertx, container, rm, securedActions, config);
		StructureController structureController = new StructureController(vertx, container, rm, securedActions);
		ClassController classController = new ClassController(vertx, container, rm, securedActions);
		UserController userController = new UserController(vertx, container, rm, securedActions);
		ProfileController profileController = new ProfileController(vertx, container, rm, securedActions);
		GroupController groupController = new GroupController(vertx, container, rm, securedActions);
		TenantController tenantController = new TenantController(vertx, container, rm, securedActions);

		vertx.eventBus().registerHandler("user.repository",
				new RepositoryHandler(new UserbookRepositoryEvents()));

		directoryController.createSuperAdmin();
		directoryController.get("/admin", "directory")
				.post("/import", "launchImport")
				.post("/transition", "launchTransition")
				.post("/export", "launchExport")
				.get("/annuaire", "annuaire")
				.get("/schools", "schools")
				.get("/api/ecole", "school")
				.get("/api/classes", "classes")
				.get("/api/personnes", "people")
				.get("/api/details", "details")
				.post("/api/user", "createUser")
				.get("/api/export", "export")
				.post("/school", "createSchool")
				.get("/school/:id", "getSchool")
				.post("/class/:schoolId", "createClass")
				.get("/users", "users");

		userBookController.get("/mon-compte", "monCompte")
				.get("/annuaire", "annuaire")
				.get("/classAdmin", "classAdmin")
				.get("/birthday", "birthday")
				.get("/mood", "mood")
				.get("/user-preferences", "userPreferences")
				.get("/api/search","search")
				.get("/api/person", "person")
				.get("/structures", "showStructures")
				.get("/structure/:structId", "showStructure")
				.get("/api/class", "myClass")
				.get("/api/edit-userbook-info", "editUserBookInfo")
				.get("/api/set-visibility", "setVisibility")
				.get("/api/edit-user-info-visibility", "editUserInfoVisibility")
				.get("/avatar/:id", "getAvatar")
				.get("/person/birthday", "personBirthday")
				.get("/preference/:application", "getPreference")
				.put("/preference/:application", "updatePreference")
				.getWithRegEx(".*", "proxyDocument");

		classController
				.get("/class/:classId", "get")
				.put("/class/:classId", "update")
				.post("/class/:classId/user", "createUser")
				.put("/class/:classId/link/:userId", "linkUser")
				.delete("/class/:classId/unlink/:userId", "unlinkUser")
				.get("/class/:classId/users", "findUsers")
				.post("/csv/:userType/class/:classId", "csv")
				.put("/class/:classId/add/:userId",  "addUser")
				.put("/class/:classId/apply", "applyComRulesAndRegistryEvent")
				.get("/class/admin/list", "listAdmin");

		structureController
				.put("/structure/:structureId/link/:userId", "linkUser")
				.delete("/structure/:structureId/unlink/:userId", "unlinkUser")
				.get("/structure/admin/list", "listAdmin");

		userController
				.get("/user/:userId", "get")
				.put("/user/:userId", "update")
				.delete("/user/:userId", "delete")
				.get("/userbook/:userId", "getUserBook")
				.put("/userbook/:userId", "updateUserBook")
				.put("/avatar/:userId", "updateAvatar")
				.get("/list/isolated", "listIsolated")
				.get("/export/users", "export")
				.post("/user/function/:userId", "addFunction")
				.delete("/user/function/:userId/:function", "removeFunction")
				.post("/user/group/:userId/:groupId", "addGroup")
				.delete("/user/group/:userId/:groupId", "removeGroup")
				.get("/user/admin/list", "listAdmin");

		profileController
				.post("/function/:profile", "createFunction")
				.delete("/function/:function", "deleteFunction")
				.post("/functiongroup", "createFunctionGroup")
				.delete("/functiongroup/:groupId", "deleteFunctionGroup");

		groupController
				.get("/group/admin/list", "listAdmin");

		tenantController
				.post("/tenant", "create");

		try {
			directoryController.registerMethod("directory", "directoryHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		try {
			userBookController.registerMethod("activation.ack", "initUserBookNode");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.clearFilters();
		SecurityHandler.addFilter(
				new UserAuthFilter(new DefaultOAuthResourceProvider(Server.getEventBus(vertx))));
		List<Set<Binding>> securedUriBinding = new ArrayList<>();
		securedUriBinding.add(directoryController.securedUriBinding());
		securedUriBinding.add(userBookController.securedUriBinding());
		securedUriBinding.add(structureController.securedUriBinding());
		securedUriBinding.add(classController.securedUriBinding());
		securedUriBinding.add(userController.securedUriBinding());
		securedUriBinding.add(profileController.securedUriBinding());
		securedUriBinding.add(groupController.securedUriBinding());
		securedUriBinding.add(tenantController.securedUriBinding());
		SecurityHandler.addFilter(new ActionFilter(securedUriBinding, getEventBus(vertx),
				new DirectoryResourcesProvider(new Neo(Server.getEventBus(vertx), container.logger())), true));
	}
}
