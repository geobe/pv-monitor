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

/**
 *
 */
class PvRecorder extends Agent<ArrayDeque<Reading>> {
    static final int STORAGE_INTERVAL = 60
    static final int UPDATE_RATE = 3
    private static int MAX_READINGS = STORAGE_INTERVAL * UPDATE_RATE
    static getMaxReadings() {MAX_READINGS}

    PvRecorder(int storage = STORAGE_INTERVAL) {
        super(new ArrayDeque<>(storage * UPDATE_RATE + 1))
        MAX_READINGS = storage * UPDATE_RATE
    }

    /**
     * add new reading to the data log and remove oldest reading, if MAX_READINGS +1 is
     * exceeded. I.e. data array contains values for at least STORAGE_INTERVAL minutes.
     * As sometimes readings are lost, data can cover some minutes more. So look
     * at the timestamps.
     * @param reading values read from web interface
     */
    private addReading(Reading reading) {
        if (data.size() >= MAX_READINGS) {
            data.removeLast()
        }
        data.addFirst reading
        return
    }

    /**
     * Threadsafe access to the latest recorded PV readings with the most actual reading in front.
     * @return a new arraylist of reading values, newest at index [0]
     */
    def getValues() {
//        def value = val
//        new ArrayList<Reading>(value.toArray() as Collection<? extends Reading>).reverse(true)
        new ArrayList<Reading>(val)
    }
}


