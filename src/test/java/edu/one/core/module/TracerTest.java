/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.testtools.TestVerticle;
import static org.vertx.testtools.VertxAssert.*;
/**
 *
 * @author dwak
 */
public class TracerTest extends TestVerticle {
	//Logger log = container.getLogger();
	JsonObject config = null;//container.getConfig();

	@Test
	public void tracerTest() {
		org.vertx.testtools.VertxAssert.initialize(vertx);

		System.out.println("hello");
		assertEquals("foo", "foo");
		//log.info("hello");
		//config.putString("log-address", "vertx-bus-address");
		/*container.deployVerticle(edu.one.core.module.Tracer.class.getName(), new Handler<String>(){
			@Override
			public void handle(String event) {
				assertNotNull(event);
			}
		});
		*/
		/*
		vertx.eventBus().publish(config.getString("log-address"), new JsonObject().putString("appli", "directory").putString("message", "testing Tracer from Directory"));
		vertx.eventBus().publish(config.getString("log-address"), new JsonObject().putString("appli", "sync").putString("message", "testing Tracer from Sync"));
		vertx.eventBus().publish(config.getString("log-address"), new JsonObject().putString("appli", "history").putString("message", "testing Tracer from History"));
		
		vertx.fileSystem().readFile("./data/dev/sync.trace", new AsyncResultHandler<Buffer>(){
			@Override
			public void handle(AsyncResult<Buffer> event) {
				log.info(event.result);
				assertTrue(event.toString().contains("testing Tracer from Sync"));
			}
		});
		
		vertx.fileSystem().readFile("./data/dev/history.trace", new AsyncResultHandler<Buffer>(){
			@Override
			public void handle(AsyncResult<Buffer> event) {
				log.info(event.result);
				assertTrue(event.toString().contains("testing Tracer from History"));
			}
		});
		
		vertx.fileSystem().readFile("./data/dev/directory.trace", new AsyncResultHandler<Buffer>(){
			@Override
			public void handle(AsyncResult<Buffer> event) {
				log.info(event.result);
				assertTrue(event.toString().contains("testing Tracer from Directory"));
			}
		});
		
		vertx.fileSystem().readFile("./data/dev/all.trace", new AsyncResultHandler<Buffer>(){
			@Override
			public void handle(AsyncResult<Buffer> event) {
				log.info(event.result);
				assertTrue(event.toString().contains("testing Tracer from Directory"));
				assertTrue(event.toString().contains("testing Tracer from History"));
				assertTrue(event.toString().contains("testing Tracer from Sync"));
			}
		});
		*/
		testComplete();
			
	}

}
