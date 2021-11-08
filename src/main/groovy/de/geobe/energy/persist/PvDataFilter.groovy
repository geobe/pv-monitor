package de.geobe.energy.persist

import de.geobe.energy.acquire.PvMonitor
import de.geobe.energy.acquire.PvRecorder
import de.geobe.energy.acquire.Reading
import de.geobe.energy.acquire.Terminator
import groovyx.gpars.actor.DefaultActor

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

import static java.lang.Math.min
import static java.lang.Math.max

class PvDataFilter extends DefaultActor {
    static final int STORAGE_INTERVAL = 1
    static final int SAMPLE_SIZE = STORAGE_INTERVAL * 3
    static final int MINUTES = 60
    static final int ZERO_INTERVAL = 10

    private LocalDateTime lastSaved

    PvMonitor pvMonitor
    PvDb pvDb = new PvDb()

    PvDataFilter(PvMonitor monitor) {
        pvMonitor = monitor
    }

    void afterStart() {
        println "${this.class.name} actor started"
    }

    void afterStop() {
        pvMonitor << new Terminator()
        println "Pv Data Filter actor stoped"
    }

    void act() {
        loop {
            react { Object msg ->
                switch (msg) {
                    case List:
                        println "${msg.class.name} of ${msg.size()} elements"
                        msg.each {
                            println it
                        }
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
        def latest = msg[0].timestamp
        def oldest = msg[-1].timestamp
        if (msg.size() < SAMPLE_SIZE) {
            return      // too few samples in msg
        }
        def second = latest.second
        def minute = latest.minute
        def timestamp = latest.truncatedTo(ChronoUnit.MINUTES)
        if (second >= MINUTES - ZERO_INTERVAL) {
            minute = minute + 1     // round to next minute
            timestamp = timestamp.plusMinutes(1)
        } else if (second > ZERO_INTERVAL) {
            return      // latest sample not at full minute
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
        def data = new PvData()
        def zoneOffset = ZoneId.systemDefault().getRules().getOffset(timestamp)
        println "ZoneOffset: $zoneOffset"
        data.recordedAt = timestamp.toInstant(zoneOffset).epochSecond
        data.battLoad = sample[0].batteryState
        data.prodMin = sample[0].production
        data.consMin = sample[0].consumption
        data.gridMin = sample[0].gridPower
        data.battMin = sample[0].batteryPower
        println "saved at $timestamp:"
        sample.each { reading ->
            println "\t $reading"
            data.prodAvg += reading.production
            data.prodMax = max(data.prodMax, reading.production)
            data.prodMin = min(data.prodMin, reading.production)

            data.consAvg += reading.consumption
            data.consMax = max(data.consMax, reading.consumption)
            data.consMin = min(data.consMin, reading.consumption)

            data.gridAvg += reading.gridPower
            data.gridMax = max(data.gridMax, reading.gridPower)
            data.gridMin = min(data.gridMin, reading.gridPower)
            if(reading.gridPower > 0) {
                data.fromGrid += reading.gridPower
            } else {
                data.toGrid += reading.gridPower
            }

            data.battAvg += reading.batteryPower
            data.battMax = max(data.battMax, reading.batteryPower)
            data.battMin = min(data.battMin, reading.batteryPower)
        }
        data.prodAvg = data.prodAvg.intdiv(sample.size())
        data.consAvg = data.consAvg.intdiv(sample.size())
        data.gridAvg = data.gridAvg.intdiv(sample.size())
        data.battAvg = data.battAvg.intdiv(sample.size())

        pvDb.saveToDb(data)
    }

}
