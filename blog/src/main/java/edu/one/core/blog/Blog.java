package edu.one.core.blog;

import edu.one.core.blog.controllers.BlogController;
import edu.one.core.blog.security.BlogResourcesProvider;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.Server;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;

public class Blog extends Server {

	@Override
	public void start() {
		super.start();

		final MongoDb mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));

		BlogController blogController = new BlogController(vertx, container, rm, securedActions, mongo);

		blogController.get("", "blog");
		blogController.post("", "create");
	//	blogController.put("/:blogId/share", "share");
		blogController.put("/:blogId", "update");
		blogController.delete("/:blogId", "delete");
		blogController.get("/list/all", "list");
		blogController.get("/:blogId", "get");

		SecurityHandler.addFilter(new ActionFilter(blogController.securedUriBinding(),
				Server.getEventBus(vertx), new BlogResourcesProvider(mongo)));
	}

}
