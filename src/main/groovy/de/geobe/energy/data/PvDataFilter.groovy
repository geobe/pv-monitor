package de.geobe.energy.data


import de.geobe.energy.acquire.Reading
import de.geobe.energy.acquire.Terminator
import groovyx.gpars.actor.DefaultActor

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static de.geobe.energy.acquire.PvRecorder.UPDATE_RATE

/**
 * Recorded list of PV readings is passed thru PvDataFilter. Here, values recorded during non overlapping
 * time intervals of STORAGE_INTERVAL minutes are aggregated to average, minimal and maximal
 * values during their interval. Aggregated values are written to pvdata h2 database.
 */
class PvDataFilter extends DefaultActor {
    static final int STORAGE_INTERVAL = 5   // into database
    static final int SAMPLE_SIZE = STORAGE_INTERVAL * UPDATE_RATE
    static final int SECONDS_PER_MINUTE = 60
    static final int ZERO_INTERVAL = 10

    static recordReadings = false

    private LocalDateTime lastSaved
    private LocalDateTime lastTimestamp

    void afterStart() {
        println "${this.class.name} actor started"
    }

    void afterStop() {
        println "Pv Data Filter actor stoped"
    }

    void act() {
        loop {
            react { Object msg ->
                switch (msg) {
                    case List:
                        if (msg) {
                            evaluateMessage(msg)
                        }
                        break
                    case Terminator:
                        println 'Terminator got me'
                    default:
                        println 'terminating'
                        terminate()
                }
            }
        }
    }

    private evaluateMessage(List<Reading> msg) {
        if(recordReadings) {
            PvDb.pvDatabase.saveToDb(msg[0])
        }
        def latest = msg[0].timestamp
        if (msg.size() < SAMPLE_SIZE) {
            return      // too few samples in msg
        }
        def second = latest.second
        def minute = latest.minute
        def timestamp = latest.truncatedTo(ChronoUnit.MINUTES)
        if (second >= SECONDS_PER_MINUTE - ZERO_INTERVAL) {
            minute = minute + 1     // round to next minute
            timestamp = timestamp.plusMinutes(1)
        } else if (second > ZERO_INTERVAL) {
            // check if sample at full minute got lost (happens sometimes)
            if(!(minute > lastTimestamp?.minute)) {
                return      // latest sample not at full minute
            }
        } else if(timestamp.equals(lastTimestamp)) {
            return      // still too close to the last recorded minute
        }
        def modulo = minute % STORAGE_INTERVAL
        if (modulo != 0) {
            return      // latest sample not on right time scale position
        }
        List<Reading> sample
        if (lastSaved) {
            sample = msg.findAll {it.timestamp.isAfter(lastSaved)}
        } else {
            sample = msg[0..<SAMPLE_SIZE]
        }
        lastSaved = latest
        lastTimestamp = timestamp
        def pvData = PvData.fromReadings(sample, timestamp)
//        pvData.recordedAt = timestamp
//        pvData.battLoad = sample[0].batteryState
//        pvData.prodMin = sample[0].production
//        pvData.consMin = sample[0].consumption
//        pvData.gridMin = sample[0].gridPower
//        pvData.battMin = sample[0].batteryPower
//        println "saved at $timestamp:"
//        sample.each { reading ->
//            println "\t $reading"
//            pvData.prodAvg += reading.production
//            pvData.prodMax = max(pvData.prodMax, reading.production)
//            pvData.prodMin = min(pvData.prodMin, reading.production)
//
//            pvData.consAvg += reading.consumption
//            pvData.consMax = max(pvData.consMax, reading.consumption)
//            pvData.consMin = min(pvData.consMin, reading.consumption)
//
//            pvData.gridAvg += reading.gridPower
//            pvData.gridMax = max(pvData.gridMax, reading.gridPower)
//            pvData.gridMin = min(pvData.gridMin, reading.gridPower)
//            if(reading.gridPower > 0) {
//                pvData.fromGrid += reading.gridPower
//            } else {
//                pvData.toGrid += reading.gridPower
//            }
//
//            pvData.battAvg += reading.batteryPower
//            pvData.battMax = max(pvData.battMax, reading.batteryPower)
//            pvData.battMin = min(pvData.battMin, reading.batteryPower)
//        }
//        pvData.prodAvg = pvData.prodAvg.intdiv(sample.size())
//        pvData.consAvg = pvData.consAvg.intdiv(sample.size())
//        pvData.gridAvg = pvData.gridAvg.intdiv(sample.size())
//        pvData.battAvg = pvData.battAvg.intdiv(sample.size())

        PvDb.pvDatabase.saveToDb(pvData)
    }

}
