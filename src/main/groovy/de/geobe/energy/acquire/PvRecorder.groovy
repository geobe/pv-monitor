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

import groovyx.gpars.agent.Agent

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Temporal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 *
 */
class PvRecorder extends Agent<ArrayDeque<Reading>> {
    static final int SHORT_INTERVAL = 5
    static final int LONG_INTERVAL = 15
    static final int UPDATE_RATE = 3
    static final int MAX_READINGS = LONG_INTERVAL * UPDATE_RATE

    private data = new ArrayDeque<>(MAX_READINGS + 1)

    /**
     * add new reading to the data log and remove oldest reading, if it is
     * older then LONG_INTERVAL minutes
     * @param reading values read from web interface
     */
    def addReading(Reading reading) {
        if (data.size() >= MAX_READINGS) {
            data.remove()
        }
        data.add reading
        return
    }

    def val() {
        new ArrayList<Reading>(data.toArray() as Collection<? extends Reading>).reverse(true)
    }
}


