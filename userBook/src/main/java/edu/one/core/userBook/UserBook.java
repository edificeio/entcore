package edu.one.core.userBook;

import edu.one.core.infra.Server;
import edu.one.core.userbook.controllers.UserBookController;

public class UserBook extends Server {

	@Override
	public void start() {
		super.start();
		UserBookController userBook = new UserBookController(vertx, container, rm, securedActions, config);

		userBook.get("/mon-compte", "monCompte")
				.get("/annuaire", "annuaire")
				.get("/api/search","search")
				.get("/api/person", "person")
				.get("/api/account", "account")
				.get("/api/class", "myClass")
				.get("/api/edit-userbook-info", "editUserBookInfo")
				.get("/api/set-visibility", "setVisibility")
				.postWithRegEx(".*", "proxyDocument")
				.getWithRegEx(".*", "proxyDocument");
	}
}