package org.entcore.archive.services.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.currentTimeMillis;

public class UserExport implements Shareable, Serializable {

	private static final long serialVersionUID = 42L;

  private long start;
	private final AtomicLong progress;
	private final AtomicInteger counter;
	private final Set<String> expectedExport;
	private final String exportId;

	public UserExport(Set<String> expectedExport, String exportId) {
    this.start = currentTimeMillis();
		this.progress = new AtomicLong(this.start);
		this.counter = new AtomicInteger(0);
		this.expectedExport = Collections.unmodifiableSet(expectedExport);
		this.exportId = exportId;
	}

  @JsonCreator
  public UserExport(@JsonProperty("progress") final long progress,
                    @JsonProperty("counter") final int counter,
                    @JsonProperty("expectedExport") final Set<String> expectedExport,
                    @JsonProperty("exportId") final String exportId,
                    @JsonProperty("start") final long start) {
    this.progress = new AtomicLong(progress);
    this.counter = new AtomicInteger(counter);
    this.expectedExport = expectedExport;
    this.exportId = exportId;
    this.start = start;
  }

  public static UserExport fromJson(final JsonObject jsonObject) {
      return jsonObject == null ? null : jsonObject.mapTo(UserExport.class);
  }

  public long getStart() {
    return start;
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

    public int getCounter() {
        return counter.get();
    }
}
