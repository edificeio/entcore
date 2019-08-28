package org.entcore.archive.services.impl;

import io.vertx.core.shareddata.Shareable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UserExport implements Shareable, Serializable {

	private static final long serialVersionUID = 42L;

	private AtomicLong progress;
	private AtomicInteger counter;
	private final Set<String> expectedExport;
	private final String exportId;

	public UserExport(Set<String> expectedExport, String exportId) {
		this.progress = new AtomicLong(System.currentTimeMillis());
		this.counter = new AtomicInteger(0);
		this.expectedExport = Collections.unmodifiableSet(expectedExport);
		this.exportId = exportId;
	}

	public Long getProgress() {
		return this.progress.get();
	}

	public void setProgress(long progress) {
		this.progress.set(progress);
	}

	public int incrementAndGetCounter() {
		return this.counter.incrementAndGet();
	}

	public Set<String> getExpectedExport() {
		return this.expectedExport;
	}

    public String getExportId() {
        return exportId;
    }
}
