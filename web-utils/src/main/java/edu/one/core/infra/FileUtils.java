package edu.one.core.infra;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

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

}
