package org.entcore.archive.services.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;

import java.beans.Transient;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;

public class UserExport implements Shareable, Serializable {

	private static final long serialVersionUID = 42L;

  private long start;
	private final AtomicLong progress;
    /** {@code true} or {@code} false for each apps that have to be exported.*/
	private final Map<String, Boolean> stateByModule;
	private final String exportId;

	public UserExport(Map<String, Boolean> stateByModule, String exportId) {
        this.start = currentTimeMillis();
		this.progress = new AtomicLong(this.start);
		this.stateByModule = new HashMap<>(stateByModule);
		this.exportId = exportId;
	}

    public UserExport(Collection<String> apps, String exportId) {
        this.start = currentTimeMillis();
        this.progress = new AtomicLong(this.start);
        this.stateByModule = apps.stream().collect(Collectors.toMap(e -> e, e -> false));
        this.exportId = exportId;
    }

  @JsonCreator
  public UserExport(@JsonProperty("progress") final long progress,
                    @JsonProperty("stateByModule") final Map<String, Boolean> stateByModule,
                    @JsonProperty("exportId") final String exportId,
                    @JsonProperty("start") final long start) {
    this.progress = new AtomicLong(progress);
    this.stateByModule = stateByModule;
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

    public String getExportId() {
        return exportId;
    }

    public Map<String, Boolean> getStateByModule() {
        return stateByModule;
    }

    @Transient
    public boolean isFinished() {
        return stateByModule.values().stream().allMatch(exported -> exported);
    }
}
