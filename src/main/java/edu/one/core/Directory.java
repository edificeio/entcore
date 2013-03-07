package edu.one.core;

import com.hazelcast.config.Config;
import edu.one.core.infra.Neo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

public class Directory extends Verticle implements Handler<Message<String>> {

	private Logger log;
	private JsonObject config;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		config = container.getConfig();
		log.info(config.getString("test"));

		// Test
		vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
			public void handle(final NetSocket socket) {
				socket.dataHandler(new Handler<Buffer>() {
					public void handle(Buffer b) {
						JsonObject query = new JsonObject()
								.putString("action", "execute")
								.putString("query", b.toString());
//					JsonObject result = new Neo(vertx.eventBus(),"wse.neo4j.persistor").send(query);
//						log.info("REPLY " + result);
					}
				});
			}
		}).listen(1234);
	}

	@Override
	public void handle(Message<String> event) {
		log.info(event.body);
	}
}