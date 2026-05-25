package com.resumepipeline.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineTimer {

    private static final Logger log = LoggerFactory.getLogger(PipelineTimer.class);

    private final String label;
    private final long start;

    private PipelineTimer(String label) {
        this.label = label;
        this.start = System.currentTimeMillis();
        log.info("[PIPELINE] {} start", label);
    }

    public static PipelineTimer start(String label) {
        return new PipelineTimer(label);
    }

    public long stop() {
        long elapsed = System.currentTimeMillis() - start;
        log.info("[PIPELINE] {} done in {}ms", label, elapsed);
        return elapsed;
    }

    public long stop(String extra) {
        long elapsed = System.currentTimeMillis() - start;
        log.info("[PIPELINE] {} done in {}ms — {}", label, elapsed, extra);
        return elapsed;
    }
}
