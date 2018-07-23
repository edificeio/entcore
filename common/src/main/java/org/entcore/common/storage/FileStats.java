package org.entcore.common.storage;

import java.util.Date;

public class FileStats {
	private Date createdAt;
	private Date lastModified;
	private long sizeInBytes;

	public FileStats() {
	}

	public FileStats(long createdAt, long lastModified, long size) {
		super();
		this.createdAt = new Date(createdAt);
		this.lastModified = new Date(lastModified);
		this.sizeInBytes = size;
	}

	public FileStats(Date createdAt, Date lastModified, long size) {
		super();
		this.createdAt = createdAt;
		this.lastModified = lastModified;
		this.sizeInBytes = size;
	}

	public long getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

}
