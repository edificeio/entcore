package edu.one.core.userBook;

import edu.one.core.infra.Server;
import edu.one.core.userbook.controllers.UserBookController;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserBook extends Server {

	@Override
	public void start() {
		super.start();
		UserBookController userBookController = new UserBookController(vertx, container, rm, securedActions, config);

		userBookController.get("/mon-compte", "monCompte")
				.get("/annuaire", "annuaire")
				.get("/api/search","search")
				.get("/api/person", "person")
				.get("/api/class", "myClass")
				.get("/api/edit-user-info", "editUserInfo")
				.get("/api/edit-userbook-info", "editUserBookInfo")
				.get("/api/set-visibility", "setVisibility")
				.postWithRegEx(".*", "proxyDocument")
				.getWithRegEx(".*", "proxyDocument");

		try {
			userBookController.registerMethod("wse.activation.hack", "initUserBookNode");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}
	}
}