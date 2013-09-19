package edu.one.core.infra;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.http.ETag;

public class FileUtils {

	public static JsonObject metadata(HttpServerFileUpload upload) {
		JsonObject metadata = new JsonObject();
		metadata.putString("name", upload.name());
		metadata.putString("filename", upload.filename());
		metadata.putString("content-type", upload.contentType());
		metadata.putString("content-transfer-encoding", upload.contentTransferEncoding());
		metadata.putString("charset", upload.charset().name());
		metadata.putNumber("size", upload.size());
		return metadata;
	}

	public static void writeUploadFile(final HttpServerRequest request, final String filePath,
			final Handler<JsonObject> handler) {
		request.expectMultiPart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload upload) {
				final String filename = filePath;
				upload.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						handler.handle(FileUtils.metadata(upload));
					}
				});
				upload.streamToFileSystem(filename);
			}
		});
	}

	public static void gridfsWriteUploadFile(final HttpServerRequest request, final EventBus eb,
			final String gridfsAddress, final Handler<JsonObject> handler) {
		request.expectMultiPart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload event) {
				final Buffer buff = new Buffer();
				event.dataHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer event) {
						buff.appendBuffer(event);
					}
				});
				event.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void end) {
						JsonObject save = new JsonObject();
						save.putString("action", "save");
						save.putString("content-type", event.contentType());
						save.putString("filename", event.filename());
						byte [] header = null;
						try {
							header = save.toString().getBytes("UTF-8");
						} catch (UnsupportedEncodingException e) {
							JsonObject json = new JsonObject().putString("status", "error")
									.putString("message", e.getMessage());
							handler.handle(json);
						}
						if (header != null) {
							buff.appendBytes(header).appendInt(header.length);
							eb.send("wse.gridfs.persistor", buff, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> message) {
									handler.handle(message.body()
											.putObject("metadata", metadata(event)));
								}
							});
						}
					}
				});
			}
		});
	}

	public static void gridfsReadFile(String id, final EventBus eb,
			final String gridfsAddress, final Handler<Buffer> handler) {
		JsonObject find = new JsonObject();
		find.putString("action", "findone");
		find.putObject("query", new JsonObject("{ \"_id\": \"" + id + "\"}"));
		byte [] header = null;
		try {
			header = find.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			handler.handle(new Buffer());
		}
		if (header != null) {
			Buffer buf = new Buffer(header);
			buf.appendInt(header.length);
			eb.send(gridfsAddress, buf, new  Handler<Message<Buffer>>() {
			@Override
			public void handle(Message<Buffer> res) {
				handler.handle(res.body());
			}
		});
		}
	}

	public static void gridfsSendFile(final String id, final String downloadName, final EventBus eb,
			final String gridfsAddress, final HttpServerResponse response, final boolean inline,
			final JsonObject metadata) {
		gridfsReadFile(id, eb, gridfsAddress, new Handler<Buffer>() {
			@Override
			public void handle(Buffer file) {
				if (!inline) {
					String name = downloadName;
					if (metadata != null && metadata.getString("filename") != null) {
						String filename = metadata.getString("filename");
						int fIdx = filename.lastIndexOf('.');
						String fExt = null;
						if (fIdx >= 0) {
							fExt = filename.substring(fIdx);
						}
						int dIdx = downloadName.lastIndexOf('.');
						String dExt = null;
						if (dIdx >= 0) {
							dExt = downloadName.substring(dIdx);
						}
						if (fExt != null && !fExt.equals(dExt)) {
							name += fExt;
						}
					}
					response.putHeader("Content-Disposition",
							"attachment; filename=" + name);
				} else {
					ETag.addHeader(response, id);
				}
				if (metadata != null && metadata.getString("content-type") != null) {
					response.putHeader("Content-Type", metadata.getString("content-type"));
				}
				response.end(file);
			}
		});
	}

	public static void gridfsRemoveFile(String id, EventBus eb, String gridfsAddress,
			final Handler<JsonObject> handler) {
		JsonObject find = new JsonObject();
		find.putString("action", "remove");
		find.putObject("query", new JsonObject("{ \"_id\": \"" + id + "\"}"));
		byte [] header = null;
		try {
			header = find.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			handler.handle(new JsonObject().putString("status", "error"));
		}
		if (header != null) {
			Buffer buf = new Buffer(header);
			buf.appendInt(header.length);
			eb.send(gridfsAddress, buf, new  Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					handler.handle(res.body());
				}
			});
		}
	}

	public static void gridfsCopyFile(String id, EventBus eb, String gridfsAddress,
			final Handler<JsonObject> handler) {
		JsonObject find = new JsonObject();
		find.putString("action", "copy");
		find.putObject("query", new JsonObject("{ \"_id\": \"" + id + "\"}"));
		byte [] header = null;
		try {
			header = find.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			handler.handle(new JsonObject().putString("status", "error"));
		}
		if (header != null) {
			Buffer buf = new Buffer(header);
			buf.appendInt(header.length);
			eb.send(gridfsAddress, buf, new  Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					handler.handle(res.body());
				}
			});
		}
	}

}
