package org.entcore.common.s3.utils;

import io.vertx.core.buffer.Buffer;

public class Chunk {

    private static final long DEFAULT_MAX_SIZE = 30 * 1024 * 1024; // 30Mo

    private long maxSize;

    private Buffer buffer;

    private int chunkNumber;

    private long chunkSize;

    private int retryIndex;

    public Chunk() {
        // TODO: get maxSize config
        this(DEFAULT_MAX_SIZE);
    }

    public Chunk(long maxSize) {
        this.maxSize = maxSize;

        this.buffer = Buffer.buffer();
        this.chunkNumber = 1;
        this.chunkSize = 0;
        this.retryIndex = 0;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public int getRetryIndex() {
        return retryIndex;
    }

    public void appendBuffer(Buffer bufferPart) {
        buffer.appendBuffer(bufferPart);
        chunkSize += bufferPart.length();
    }

    public void nextChunk() {
        buffer = Buffer.buffer();
        chunkNumber++;
        chunkSize = 0;
        retryIndex = 0;
    }

    public void incrementRetryIndex() {
        retryIndex++;
    }
    
}
