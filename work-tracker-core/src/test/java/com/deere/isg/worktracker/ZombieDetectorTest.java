/**
 * Copyright 2018 Deere & Company
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.deere.isg.worktracker.MockTimeUtils.freezeClock;
import static com.deere.isg.worktracker.MockTimeUtils.freezeClockOffset;
import static com.deere.isg.worktracker.ZombieDetector.LONG_RUNNING;
import static com.deere.isg.worktracker.ZombieDetector.ZOMBIE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZombieDetectorTest {
    private static final int SIZE = 10;
    private static final List<MockWork> WORK_LIST = MockWorkUtils.createMockWorkList(SIZE);

    @Mock
    private OutstandingWork<MockWork> outstanding;
    @Mock
    private ZombieLogger logger;

    private ZombieDetector detector;

    @Before
    public void setUp() {
        detector = new ZombieDetector(outstanding);
        detector.start();
        detector.setLogger(logger);

        when(outstanding.stream()).thenAnswer(invocation -> WORK_LIST.stream());
    }

    @After
    public void tearDown() {
        detector.close();
        Clock.clear();
    }

    @Test
    public void assertWorksAreZombies() {
        freezeClockOffset(1L);

        int count = (int) outstanding.stream()
                .filter(Work::isZombie)
                .count();

        assertThat(count, is(WORK_LIST.size()));
    }

    @Test
    public void shortRunningWorksNotLogged() {
        freezeClock(TimeUnit.MILLISECONDS.convert(30 - 1, TimeUnit.SECONDS));

        detector.doWork();

        verify(logger, times(0)).logKill(eq(ZOMBIE), any());
    }

    @Test
    public void longRunningWorksAreLogged() {
        System.out.println("Starting longRunningWorksAreLogged");
        freezeClock(TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS));

        detector.doWork();

        verify(logger, times(SIZE)).logZombie(eq(LONG_RUNNING), any());
    }

    @Test
    public void zombiesAreLogged() {

        System.out.println("Starting zombiesAreLogged");
        freezeClockOffset(1L);

        detector.doWork();

        verify(logger, times(SIZE)).logZombie(eq(ZOMBIE), any());
    }

    @Test
    public void zombiesAreKilled() {

        System.out.println("Starting zombiesAreKilled");
        freezeClockOffset(1L);

        detector.doWork();

        int count = (int) outstanding.stream()
                .filter(Work::isZombie)
                .count();

        assertThat(count, is(SIZE));
        verify(logger, times(SIZE)).logZombie(eq(ZOMBIE), any());
        verify(logger, times(SIZE)).logKill(startsWith(ZOMBIE + " killed at "), any());
        verify(logger, times(0)).warnFailure(eq(ZOMBIE + " failed to die"), any());
    }

    @Test
    public void nonZombiesAreNotLogged() {
        verify(logger, times(0)).logZombie(eq(ZOMBIE), any());
    }

    @Test
    public void closeCancelsFutureThreads() {
        assertThat(detector.isCancelled(), is(false));

        detector.close();

        assertThat(detector.isCancelled(), is(true));
    }

    @Test(expected = ZombieError.class)
    public void killRunawayThrowsError() {
        setupAZombie();

        detector.killRunaway();
    }

    @Test
    public void errorLogForZombieErrorHappens() {
        setupAZombie();

        try {
            detector.killRunaway();
        } catch (ZombieError e) {
            verify(logger).logError(ZOMBIE, e);
        }
    }

    @Test
    public void killRunawayDoesNothingIfNotZombie() {
        when(outstanding.current().filter(Work::isZombie)).thenReturn(Optional.of(new MockWork()));

        failOnZombieError();
    }

    @Test
    public void killRunawayDoesNothingIfNotZombieEmptyOp() {
        when(outstanding.current().filter(Work::isZombie)).thenReturn(Optional.empty());

        failOnZombieError();
    }

    private void failOnZombieError() {
        try {
            detector.killRunaway();
        } catch (ZombieError e) {
            fail("Should not throw ZombieError since it's not a zombie");
        }
    }

    private void setupAZombie() {
        when(outstanding.current().filter(Work::isZombie)).thenReturn(Optional.of(new MockWork()));
        freezeClockOffset(1L);
    }
}
