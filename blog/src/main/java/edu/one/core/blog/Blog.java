package edu.one.core.blog;

import edu.one.core.blog.controllers.BlogController;
import edu.one.core.infra.Server;

public class Blog extends Server {
	@Override
	public void start() {
		super.start();
		BlogController blogController = new BlogController(vertx, container, rm, securedActions, config);

		blogController.get("/blog", "blog");

	}
}