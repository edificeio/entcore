package edu.one.core.blog.services;

import edu.one.core.common.user.UserInfos;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;

public interface BlogTimelineService {

	void notifyShare(HttpServerRequest request, String blogId, UserInfos user,
					 JsonArray recipients, String blogUri);

	void notifyUpdateBlog(HttpServerRequest request, String blogId, UserInfos user,
						  String blogUri);

	void notifyPublishPost(HttpServerRequest request, String blogId, String postId,
						   UserInfos user, String blogUri);

}
