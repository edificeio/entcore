/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.directory;

import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.BasicFilter;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.remote.RemoteClient;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.FileStorage;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.directory.controllers.*;
import org.entcore.directory.security.DirectoryResourcesProvider;
import org.entcore.directory.security.UserbookCsrfFilter;
import org.entcore.directory.services.*;
import org.entcore.directory.services.impl.*;

import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

public class Directory extends BaseServer {

	public static final String FEEDER = "entcore.feeder";
	public static final String SLOTPROFILE_COLLECTION = "slotprofile";

	@Override
	protected void initFilters() {
		super.initFilters();
		addFilter(new UserbookCsrfFilter(getEventBus(vertx), securedUriBinding));
	}

	@Override
	public void start() throws Exception {
		final EventBus eb = getEventBus(vertx);
		super.start();
		MongoDbConf.getInstance().setCollection(SLOTPROFILE_COLLECTION);
		setDefaultResourceFilter(new DirectoryResourcesProvider());

		rm.get("/userbook/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				i18nMessages(request);
			}
		});
		Storage storageAvatar = new FileStorage(vertx, config.getString("avatar-path"),
				config.getBoolean("avatar-flat", false));
		Storage defaulStorage = new StorageFactory(vertx, config,
				new MongoDBApplicationStorage("documents", Directory.class.getSimpleName())).getStorage();
		WorkspaceHelper wsHelper = new WorkspaceHelper(vertx.eventBus(), defaulStorage);

		EmailFactory emailFactory = new EmailFactory(vertx, config);
		EmailSender emailSender = emailFactory.getSender();
		UserService userService = new DefaultUserService(emailSender, eb);
		UserBookService userBookService = new DefaultUserBookService(storageAvatar, wsHelper);
		TimelineHelper timeline = new TimelineHelper(vertx, eb, config);
		ClassService classService = new DefaultClassService(eb);
		SchoolService schoolService = new DefaultSchoolService(eb);
		GroupService groupService = new DefaultGroupService(eb);
		ConversationNotification conversationNotification = new ConversationNotification(vertx, eb, config);

		DirectoryController directoryController = new DirectoryController();
		directoryController.setClassService(classService);
		directoryController.setSchoolService(schoolService);
		directoryController.setUserService(userService);
		directoryController.setGroupService(groupService);
		directoryController.setSlotProfileService(new DefaultSlotProfileService(SLOTPROFILE_COLLECTION));
		addController(directoryController);
		vertx.setTimer(5000l, event -> directoryController.createSuperAdmin());


		UserBookController userBookController = new UserBookController();
		userBookController.setSchoolService(schoolService);
		userBookController.setUserBookService(userBookService);
		userBookController.setConversationNotification(conversationNotification);
		addController(userBookController);

		StructureController structureController = new StructureController();
		structureController.setStructureService(schoolService);
		structureController.setNotifHelper(emailSender);
		structureController.setMassMailService(new DefaultMassMailService(vertx,eb,emailSender,config));
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

		ImportController importController = new ImportController();
		importController.setImportService(new DefaultImportService(vertx, eb));
		importController.setSchoolService(schoolService);
		addController(importController);

		TimetableController timetableController = new TimetableController();
		timetableController.setTimetableService(new DefaultTimetableService(eb));
		addController(timetableController);

        ShareBookmarkController shareBookmarkController = new ShareBookmarkController();
        shareBookmarkController.setShareBookmarkService(new DefaultShareBookmarkService());
        addController(shareBookmarkController);

        SlotProfileController slotProfileController = new SlotProfileController(SLOTPROFILE_COLLECTION);
        slotProfileController.setSlotProfileService(new DefaultSlotProfileService(SLOTPROFILE_COLLECTION));
        addController(slotProfileController);

        addController(new CalendarController());

        vertx.eventBus().localConsumer("user.repository",
                new RepositoryHandler(new UserbookRepositoryEvents(userBookService), eb));

        final JsonObject remoteNodes = config.getJsonObject("remote-nodes");
        if (remoteNodes != null) {
			final RemoteClient remoteClient = new RemoteClient(vertx, remoteNodes);
			final RemoteUserService remoteUserService = new DefaultRemoteUserService();
			((DefaultRemoteUserService) remoteUserService).setRemoteClient(remoteClient);
			final RemoteUserController remoteUserController = new RemoteUserController();
			remoteUserController.setRemoteUserService(remoteUserService);
			addController(remoteUserController);
		}

	}

}
