package org.entcore.admin;

import org.entcore.admin.controllers.AdminController;
import org.entcore.admin.controllers.PlateformeInfoController;
import org.entcore.common.http.BaseServer;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;

public class Admin extends BaseServer {

	 @Override
	 public void start() {
		 super.start();
		 
		 addController(new AdminController());
		 
		 // check if sms module activated
		 String smsAddress = "";
		 String smsProvider = "";
		 ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		 if(server != null && server.get("smsProvider") != null) {
			 smsProvider = (String) server.get("smsProvider");
			 final String node = (String) server.get("node");
			 smsAddress = (node != null ? node : "") + "entcore.sms";
		 } else {
			 smsAddress = "entcore.sms";
		 }
		 
		 JsonObject pingAction = new JsonObject()
				 .putString("provider", smsProvider)
				 .putString("action", "ping");
		 
		 vertx.eventBus().sendWithTimeout(smsAddress, pingAction, 5000l, 
				new Handler<AsyncResult<Message<JsonObject>>>() {
					@Override
					public void handle(AsyncResult<Message<JsonObject>> res) {
						PlateformeInfoController plateformeInfoController = new PlateformeInfoController();
						if (res != null && res.succeeded()) {
							if ("ok".equals(res.result().body().getString("status"))) {
								plateformeInfoController.setSmsModule(true);
							}
						}
						addController(plateformeInfoController);
					}
				}
		 );
	 }

}
