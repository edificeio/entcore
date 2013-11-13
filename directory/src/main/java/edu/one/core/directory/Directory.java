package edu.one.core.directory;

import edu.one.core.directory.controllers.DirectoryController;
import edu.one.core.directory.controllers.UserBookController;
import edu.one.core.infra.Server;
import edu.one.core.infra.http.Binding;
import edu.one.core.infra.http.Renders;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
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

		directoryController.createSuperAdmin();
		directoryController.get("/admin", "directory")
				.get("/testbe1d", "testBe1d")
				.get("/api/ecole", "school")
				.get("/api/classes", "classes")
				.get("/api/personnes", "people")
				.get("/api/details", "details")
				.post("/api/user", "createUser")
				.get("/api/export", "export")
				.post("/api/group-profil", "groupProfile");

		userBookController.get("/mon-compte", "monCompte")
				.get("/annuaire", "annuaire")
				.get("/birthday", "birthday")
				.get("/api/search","search")
				.get("/api/person", "person")
				.get("/api/class", "myClass")
				.get("/api/edit-user-info", "editUserInfo")
				.get("/api/edit-userbook-info", "editUserBookInfo")
				.get("/api/set-visibility", "setVisibility")
				.get("/avatar/:id", "getAvatar")
				.get("/person/birthday", "personBirthday")
				.postWithRegEx(".*", "proxyDocument")
				.getWithRegEx(".*", "proxyDocument");

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

		List<Set<Binding>> securedUriBinding = new ArrayList<>();
		securedUriBinding.add(directoryController.securedUriBinding());
		securedUriBinding.add(userBookController.securedUriBinding());
		SecurityHandler.addFilter(new ActionFilter(securedUriBinding, getEventBus(vertx)));
	}
}
