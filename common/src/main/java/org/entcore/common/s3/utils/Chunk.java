package org.entcore.common.s3.utils;

import io.vertx.core.buffer.Buffer;

public class Chunk {

    private static final int DEFAULT_MAX_SIZE = 30 * 1024 * 1024; // 30Mo

    private int maxSize;

    private Buffer buffer;

    private int chunkNumber;

    private int chunkSize;

    public Chunk() {
        // TODO: get maxSize config
        this(DEFAULT_MAX_SIZE);
    }

    public Chunk(int maxSize) {
        this.maxSize = maxSize;

        this.buffer = Buffer.buffer();
        this.chunkNumber = 1;
        this.chunkSize = 0;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void appendBuffer(Buffer bufferPart) {
        buffer.appendBuffer(bufferPart);
        chunkSize += bufferPart.length();
    }

    public void nextChunk() {
        buffer = Buffer.buffer();
        chunkNumber++;
        chunkSize = 0;
    }



    
}
