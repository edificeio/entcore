package org.entcore.common.validation;


import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ExtensionValidator extends FileValidator {

	private final JsonArray blockedExtension;

	public ExtensionValidator(JsonArray blockedExtension) {
		this.blockedExtension = blockedExtension;
	}

	@Override
	protected void validate(JsonObject metadata, JsonObject context, Handler<AsyncResult<Void>> handler) {
		final String filename = metadata.getString("filename", "");
		final int idx = filename.lastIndexOf('.');
		if (idx > 0 && idx < (filename.length() - 1) && blockedExtension.contains(filename.substring(idx + 1).toLowerCase())) {
			handler.handle(new DefaultAsyncResult<Void>(new ValidationException("blocked.extension")));
		} else {
			handler.handle(new DefaultAsyncResult<>((Void) null));
		}
	}

}
