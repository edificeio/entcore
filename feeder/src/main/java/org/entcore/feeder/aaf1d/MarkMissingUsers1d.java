package org.entcore.feeder.aaf1d;

import java.util.Set;

import org.entcore.feeder.aaf.ImportProcessing;
import org.entcore.feeder.aaf.StudentImportProcessing2;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MarkMissingUsers1d extends StudentImportProcessing2 {

    protected MarkMissingUsers1d(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	protected void preCommit() {
        log.info(e -> "Mark missing users 1d", true);
	}

	@Override
	public void process(JsonObject object) {
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		initAcademyPrefix(path);
		importer.markMissingUsers(null, getAcademyPrefix(), new Handler<Void>() {
			@Override
			public void handle(Void event) {
				parse(handler, getNextImportProcessing());
			}
		});
	}

	@Override
	public String getMappingResource() {
		return "";
	}

    @Override
	protected String getFileRegex() {
		return "";
    }

    @Override
	protected ImportProcessing getNextImportProcessing() {
		return new StudentImportProcessing1d2(path, vertx);
    }

}
