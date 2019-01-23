/**
 * Copyright 2019 Deere & Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.deere.isg.worktracker;

import com.deere.clock.Clock;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ZombieDetector implements AutoCloseable {
    public static final String ZOMBIE = "ZOMBIE";
    public static final String LONG_RUNNING = "LONG_RUNNING";

    private static final long SECOND_30 = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    private final OutstandingWork<?> outstanding;

    private ZombieLogger logger = ZombieLogger.getLogger();
    private ScheduledFuture<?> future;

    public ZombieDetector(OutstandingWork<?> outstanding) {
        this.outstanding = outstanding;
    }

    public void start() {
        future = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                this::doWork, 30, 30, TimeUnit.SECONDS
        );
    }

    /**
     * For requests that have the potential of becoming zombies
     * (i.e. take more than max time to complete), it is best
     * to check if there are any zombie requests before the application
     * takes in more of similar kinds of requests.
     * <p>
     * You should call {@code killRunaway()} before you execute your logic
     * in order to kill any zombie requests that could eventually end up
     * taking too much resources and crash your application.
     * See documentation for more details
     * <p>
     * Example code:
     * <pre>{@code
     * public class ExampleService {
     *
     *  private final ZombieDetector detector;
     *
     *  public ExampleService(ZombieDetector detector) {
     *      this.detector = detector;
     *  }
     *
     *  public String doSomeHeavyWork(String id) {
     *      detector.killRunaway();
     *
     *      //do some heavy work
     *      //...
     *      return something;
     *  }
     * }
     * }</pre>
     */
    public void killRunaway() {
        if (outstanding.current().filter(Work::isZombie).isPresent()) {
            ZombieError error = new ZombieError("Request took too long");
            logger.logError(ZOMBIE, error);
            throw error;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        Optional.ofNullable(future).ifPresent(f -> f.cancel(true));
    }

    void doWork() {
        outstanding.stream()
                .filter(this::isLoggable)
                .peek(this::logZombie)
                .filter(Work::isZombie)
                .forEach(this::killZombie);
    }

    void setLogger(ZombieLogger logger) {
        this.logger = logger;
    }

    boolean isCancelled() {
        return future.isCancelled();
    }

    private void killZombie(Work work) {
        try {
            work.kill();
            logger.logKill(ZOMBIE + " killed at " + Clock.now(), work);
        } catch (SecurityException e) {
            logger.warnFailure(ZOMBIE + " failed to die", work);
        }
    }

    private boolean isLoggable(Work work) {
        return work.getElapsedMillis() > SECOND_30;
    }

    private void logZombie(Work work) {
        logger.logZombie(work.isZombie() ? ZOMBIE : LONG_RUNNING, work);
    }
}
