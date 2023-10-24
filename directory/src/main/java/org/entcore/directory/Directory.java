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

import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.remote.RemoteClientCluster;
import org.entcore.common.sms.SmsSenderFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.FileStorage;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.common.user.position.UserPositionService;
import org.entcore.common.user.position.impl.DefaultUserPositionService;
import org.entcore.directory.controllers.*;
import org.entcore.directory.security.DirectoryResourcesProvider;
import org.entcore.directory.security.UserbookCsrfFilter;
import org.entcore.directory.services.*;
import org.entcore.directory.services.impl.*;

public class Directory extends BaseServer {

	public static final String DIRECTORY_ADDRESS = "entcore.directory";

	public static final String FEEDER = "entcore.feeder";
	public static final String SLOTPROFILE_COLLECTION = "slotprofile";

	@Override
	protected void initFilters() {
		super.initFilters();
		addFilter(new UserbookCsrfFilter(getEventBus(vertx), securedUriBinding));
	}

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final EventBus eb = getEventBus(vertx);
		super.start(startPromise);
		MongoDbConf.getInstance().setCollection(SLOTPROFILE_COLLECTION);
		setDefaultResourceFilter(new DirectoryResourcesProvider());

		rm.get("/userbook/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				i18nMessages(request);
			}
		});
		final StorageFactory storageFactory = new StorageFactory(vertx, config,
				new MongoDBApplicationStorage("documents", Directory.class.getSimpleName()));
		final Storage storageAvatar = new FileStorage(vertx, config.getString("avatar-path"),
				config.getBoolean("avatar-flat", false), storageFactory.getMessagingClient());
		final Storage defaulStorage = storageFactory.getStorage();
		WorkspaceHelper wsHelper = new WorkspaceHelper(vertx.eventBus(), defaulStorage);

		EmailFactory emailFactory = new EmailFactory(vertx, config);
		EmailSender emailSender = emailFactory.getSender();
		SmsSenderFactory.getInstance().init(vertx, config);
		final JsonObject userBookData = config.getJsonObject("user-book-data");
		UserService userService = new DefaultUserService(emailSender, eb, userBookData);
		UserBookService userBookService = new DefaultUserBookService(eb, storageAvatar, wsHelper, userBookData);
		TimelineHelper timeline = new TimelineHelper(vertx, eb, config);
		ClassService classService = new DefaultClassService(eb);
		SchoolService schoolService = new DefaultSchoolService(eb).setListUserMode(config.getString("listUserMode", "multi"));
		GroupService groupService = new DefaultGroupService(eb);
		SubjectService subjectService = new DefaultSubjectService(eb);
		final JsonObject emptyJsonObject = new JsonObject();
		UserPositionService userPositionService = new DefaultUserPositionService(eb, config
			.getJsonObject("publicConf", emptyJsonObject)
			.getJsonObject("userPosition", emptyJsonObject)
			.getBoolean("restrictCRUDToADMC", false)
		);
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
		userBookController.setUserPositionService(userPositionService);
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

		MassMessagingController massMessagingController = new MassMessagingController();
		massMessagingController.setMassMesssagingService(new DefaultMassMessagingService(vertx, eb));
		addController(massMessagingController);

		TimetableController timetableController = new TimetableController();
		timetableController.setTimetableService(new DefaultTimetableService(eb));
		addController(timetableController);

        ShareBookmarkController shareBookmarkController = new ShareBookmarkController();
        shareBookmarkController.setShareBookmarkService(new DefaultShareBookmarkService());
        addController(shareBookmarkController);

        SlotProfileController slotProfileController = new SlotProfileController(SLOTPROFILE_COLLECTION);
        slotProfileController.setSlotProfileService(new DefaultSlotProfileService(SLOTPROFILE_COLLECTION));
        addController(slotProfileController);

		SubjectController subjectController = new SubjectController();
		subjectController.setSubjectService(subjectService);
		addController(subjectController);

        addController(new CalendarController());

		UserPositionController userPositionController = new UserPositionController(userPositionService);
		addController(userPositionController);

        vertx.eventBus().localConsumer("user.repository",
                new RepositoryHandler(new UserbookRepositoryEvents(userBookService), eb));

		MessageConsumer<JsonObject> consumer = eb.consumer(DIRECTORY_ADDRESS);
		consumer.handler(message -> {
			String action = message.body().getString("action", "action.not.specified");
			if (action.equals("get-users-displayNames")) {
				JsonArray userIds = message.body().getJsonArray("userIds");
				userService.getUsersDisplayNames(userIds)
						.onSuccess(message::reply)
						.onFailure(th -> {
							log.error("[Directory] unable to retrieve users' display names", th.getCause());
							message.fail(500, th.getCause().getMessage());
						});
			} else {
				message.fail(404, "[Directory] " + action);
			}
		});

        final JsonObject remoteNodes = config.getJsonObject("remote-nodes");
        if (remoteNodes != null) {
			final RemoteClientCluster remoteClientCluster = new RemoteClientCluster(vertx, remoteNodes);
			final RemoteUserService remoteUserService = new DefaultRemoteUserService(emailSender);
			((DefaultRemoteUserService) remoteUserService).setRemoteClientCluster(remoteClientCluster);
			final RemoteUserController remoteUserController = new RemoteUserController();
			remoteUserController.setRemoteUserService(remoteUserService);
			addController(remoteUserController);
		}

	}

}
