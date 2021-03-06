/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.energy.acquire

import de.geobe.energy.data.PvDb
import geb.Browser
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

import java.time.LocalDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Actor periodically polls S10 web interface, checks if read data are new
 * and passes updated dataset as message to a processing actor.<br>
 * Class responsibilities:
 * <ul>
 *     <li>Periodically poll for data for nearly periodic data updates;</li>
 *     </li>maintain timer for scheduling;</li>
 *     <li>filter out all data already sent;</li>
 *     <li>store new data in PvRecorder agent;</li>
 *     <li>pass copy of agent dataset to all registered actors.</li>
 * </ul>
 */
class PvMonitor extends DefaultActor {

    private static final int STARTUP_DELAY = 3
    /** Delay between two read operations, trying ~ 2 times to find new value */
    private static final int READ_DELAY = 60 / (2 * PvRecorder.UPDATE_RATE) + 1

    /** send messages to these Actors */
    private List<Actor> pvDataProcessors = []

    def addPvDataProcessor(Actor processor) {
        pvDataProcessors << processor
    }

    /** used to schedule repeated S10 readout */
    private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1)

    private PvRecorder recorder
    private S10Access s10Access
    private Browser browser
    private LocalDateTime lastTimestamp
    private TimedPvReadout readout = new TimedPvReadout()

    /**
     * initialize S10 website using S10Access
     */
    void afterStart() {
        s10Access = new S10Access(S10Access.CONFIGFILE)
        recorder = new PvRecorder()
        browser = s10Access.openSite()
        timer.schedule(readout, STARTUP_DELAY, TimeUnit.SECONDS)
        println "${this.class.name} actor started"
    }

    /** cleanup and close browser before leaving */
    void afterStop() {
        s10Access.doLogout()
        browser.close()
    }

    /**
     * wait for messages and handle them
     */
    void act() {
        loop {
            react { Object msg ->
                switch (msg) {
                    case Reading:
                        def timestamp = ((Reading) msg).timestamp
                        if (!lastTimestamp || lastTimestamp.compareTo(timestamp) < 0) {
                            recorder << { addReading(msg) }
                            def recording = recorder.values
                            lastTimestamp = timestamp
                            pvDataProcessors.each { processor ->
                                processor.send(recording)
                            }
                        }
                        timer.schedule(readout, READ_DELAY, TimeUnit.SECONDS)
                        break
                    case Terminator:
                        timer.shutdownNow()
                        pvDataProcessors.each {actor ->
                            actor.send(new Terminator())
                        }
                        s10Access.doLogout()
                        PvDb.pvDatabase.shutdown()
                        reply('done')
                        terminate()
                }
            }
        }
    }

    def getRecorder() {
        recorder
    }

    /**
     * Runnable object gets periodically scheduled by timer to readout S10 web interface
     */
    class TimedPvReadout implements Runnable {

        @Override
        void run() {
            def v = s10Access.readCurrentValues()
            def r = new Reading(v)
            println "read $r"
            PvMonitor.this.send(r)
        }
    }
}

class Terminator {}
