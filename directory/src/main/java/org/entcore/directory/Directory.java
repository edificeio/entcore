package org.entcore.directory;

import edu.one.core.infra.request.filter.UserAuthFilter;
import edu.one.core.infra.security.oauth.DefaultOAuthResourceProvider;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.controllers.ClassController;
import org.entcore.directory.controllers.DirectoryController;
import org.entcore.directory.controllers.UserBookController;
import edu.one.core.infra.Server;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.http.Renders;
import org.entcore.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import org.entcore.directory.controllers.UserController;
import org.entcore.directory.security.DirectoryResourcesProvider;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Directory extends Server {

	@Override
	public void start() {
		super.start();

		rm.get("/userbook/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Renders.redirectPermanent(request, "/directory/i18n");
			}
		});

		DirectoryController directoryController = new DirectoryController(vertx, container, rm, securedActions, config);
		UserBookController userBookController = new UserBookController(vertx, container, rm, securedActions, config);
		ClassController classController = new ClassController(vertx, container, rm, securedActions);
		UserController userController = new UserController(vertx, container, rm, securedActions);

		directoryController.createSuperAdmin();
		directoryController.get("/admin", "directory")
				.get("/testbe1d", "testBe1d")
				.get("/api/ecole", "school")
				.get("/api/classes", "classes")
				.get("/api/personnes", "people")
				.get("/api/details", "details")
				.post("/api/user", "createUser")
				.get("/api/export", "export")
				.post("/school", "createSchool")
				.get("/school/:id", "getSchool")
				.post("/class/:schoolId", "createClass");

		userBookController.get("/mon-compte", "monCompte")
				.get("/annuaire", "annuaire")
				.get("/classAdmin", "classAdmin")
				.get("/birthday", "birthday")
				.get("/mood", "mood")
				.get("/user-preferences", "userPreferences")
				.get("/api/search","search")
				.get("/api/person", "person")
				.get("/api/class", "myClass")
				.get("/api/edit-user-info", "editUserInfo")
				.get("/api/edit-userbook-info", "editUserBookInfo")
				.get("/api/set-visibility", "setVisibility")
				.get("/api/edit-user-info-visibility", "editUserInfoVisibility")
				.get("/avatar/:id", "getAvatar")
				.get("/person/birthday", "personBirthday")
				.getWithRegEx(".*", "proxyDocument");

		classController
				.put("/class/:classId", "update")
				.post("/class/:classId/user", "createUser")
				.get("/class/:classId/users", "findUsers")
				.post("/csv/:userType/class/:classId", "csv")
				.put("/class/:classId/add/:userId",  "addUser");

		userController
				.put("/user/:userId", "update")
				.put("/userbook/:userId", "updateUserBook");

		try {
			directoryController.registerMethod("directory", "directoryHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		try {
			userBookController.registerMethod("wse.activation.hack", "initUserBookNode");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.clearFilters();
		SecurityHandler.addFilter(
				new UserAuthFilter(new DefaultOAuthResourceProvider(Server.getEventBus(vertx))));
		List<Set<Binding>> securedUriBinding = new ArrayList<>();
		securedUriBinding.add(directoryController.securedUriBinding());
		securedUriBinding.add(userBookController.securedUriBinding());
		securedUriBinding.add(classController.securedUriBinding());
		securedUriBinding.add(userController.securedUriBinding());
		SecurityHandler.addFilter(new ActionFilter(securedUriBinding, getEventBus(vertx),
				new DirectoryResourcesProvider(new Neo(Server.getEventBus(vertx), container.logger())), true));
	}
}
