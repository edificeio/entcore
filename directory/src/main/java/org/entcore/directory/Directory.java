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
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.directory.controllers.*;
import org.entcore.directory.security.DirectoryResourcesProvider;
import org.entcore.directory.services.*;
import org.entcore.directory.services.impl.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;

public class Directory extends BaseServer {

	public static final String FEEDER = "entcore.feeder";

	@Override
	public void start() {
		final EventBus eb = getEventBus(vertx);
		clearFilters();
		setOauthClientGrant(true);
		addFilter(new UserAuthFilter(new DefaultOAuthResourceProvider(eb)));
		super.start();
		setDefaultResourceFilter(new DirectoryResourcesProvider());

		rm.get("/userbook/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Renders.renderJson(request, I18n.getInstance().load(request.headers().get("Accept-Language")));
			}
		});

		NotificationHelper notification = new NotificationHelper(vertx, eb, container);
		UserService userService = new DefaultUserService(notification, eb);
		UserBookService userBookService = new DefaultUserBookService();
		TimelineHelper timeline = new TimelineHelper(vertx, eb, container);
		ClassService classService = new DefaultClassService(eb);
		SchoolService schoolService = new DefaultSchoolService(eb);
		GroupService groupService = new DefaultGroupService(eb);
		ConversationNotification conversationNotification = new ConversationNotification(vertx, eb, container);

		DirectoryController directoryController = new DirectoryController();
		directoryController.setClassService(classService);
		directoryController.setSchoolService(schoolService);
		directoryController.setUserService(userService);
		directoryController.setGroupService(groupService);
		addController(directoryController);
		directoryController.createSuperAdmin();

		UserBookController userBookController = new UserBookController();
		userBookController.setSchoolService(schoolService);
		addController(userBookController);

		StructureController structureController = new StructureController();
		structureController.setStructureService(schoolService);
		addController(structureController);

		ClassController classController = new ClassController();
		classController.setClassService(classService);
		classController.setConversationNotification(conversationNotification);
		classController.setSchoolService(schoolService);
		classController.setUserService(userService);
		addController(classController);

		UserController userController = new UserController();
		userController.setNotification(timeline);
		userController.setUserBookService(userBookService);
		userController.setUserService(userService);
		addController(userController);

		ProfileController profileController = new ProfileController();
		profileController.setProfileService(new DefaultProfileService(eb));
		addController(profileController);

		GroupController groupController = new GroupController();
		groupController.setGroupService(groupService);
		addController(groupController);

		TenantController tenantController = new TenantController();
		tenantController.setTenantService(new DefaultTenantService(eb));
		addController(tenantController);

		vertx.eventBus().registerLocalHandler("user.repository",
				new RepositoryHandler(new UserbookRepositoryEvents(), eb));
	}

}
