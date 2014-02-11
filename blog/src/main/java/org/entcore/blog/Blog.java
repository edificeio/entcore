package org.entcore.blog;

import edu.one.core.infra.MongoDb;
import org.entcore.blog.controllers.BlogController;
import org.entcore.blog.controllers.PostController;
import org.entcore.blog.security.BlogResourcesProvider;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Blog extends Server {

	@Override
	public void start() {
		super.start();

		final MongoDb mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));

		BlogController blogController = new BlogController(vertx, container, rm, securedActions, mongo);

		blogController.get("", "blog");
		blogController.post("", "create");
		blogController.get("/share/json/:blogId", "shareJson");
		blogController.put("/share/json/:blogId", "shareJsonSubmit");
		blogController.put("/share/remove/:blogId", "removeShare");
		blogController.put("/:blogId", "update");
		blogController.delete("/:blogId", "delete");
		blogController.get("/list/all", "list");
		blogController.get("/:blogId", "get");
		blogController.get("/blog/availables-workflow-actions", "getActionsInfos");

		PostController postController = new PostController(vertx, container, rm, securedActions, mongo);
		postController.post("/post/:blogId", "create");
		postController.put("/post/:blogId/:postId", "update");
		postController.delete("/post/:blogId/:postId", "delete");
		postController.get("/post/list/all/:blogId", "list");
		postController.get("/post/:blogId/:postId", "get");
		postController.put("/post/submit/:blogId/:postId", "submit");
		postController.put("/post/publish/:blogId/:postId", "publish");
		postController.put("/post/unpublish/:blogId/:postId", "unpublish");
		postController.post("/comment/:blogId/:postId", "comment");
		postController.delete("/comment/:blogId/:postId/:commentId", "deleteComment");
		postController.get("/comments/:blogId/:postId", "comments");
		postController.put("/comment/:blogId/:postId/:commentId", "publishComment");


		List<Set<Binding>> securedUriBinding = new ArrayList<>();
		securedUriBinding.add(blogController.securedUriBinding());
		securedUriBinding.add(postController.securedUriBinding());

		SecurityHandler.addFilter(new ActionFilter(securedUriBinding,
				Server.getEventBus(vertx), new BlogResourcesProvider(mongo)));
	}

}
