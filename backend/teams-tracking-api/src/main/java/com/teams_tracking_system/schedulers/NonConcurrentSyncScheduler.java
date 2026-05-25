package com.teams_tracking_system.schedulers;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class NonConcurrentSyncScheduler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String jobName;

    protected NonConcurrentSyncScheduler(String jobName) {
        this.jobName = jobName;
    }

    protected boolean runIfIdle(Runnable task) {
        if (!running.compareAndSet(false, true)) {
            logger.warn("{} sync skipped because a previous execution is still running.", jobName);
            return false;
        }

        try {
            task.run();
            logger.info("{} sync finished.", jobName);
            return true;
        } catch (RuntimeException exception) {
            logger.error("{} sync failed.", jobName, exception);
            return true;
        } finally {
            running.set(false);
        }
    }
}
