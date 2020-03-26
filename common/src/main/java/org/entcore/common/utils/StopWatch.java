package org.entcore.common.utils;

import java.time.Duration;

public class StopWatch {
    private final long start;
    
    public StopWatch() {
        start = System.currentTimeMillis();
    }

    public long elapsedTimeMillis() {
        long now = System.currentTimeMillis();
        return now - start;
    }

    public long elapsedTimeSeconds(){
        return Duration.ofMillis(elapsedTimeMillis()).getSeconds();
    }
}
