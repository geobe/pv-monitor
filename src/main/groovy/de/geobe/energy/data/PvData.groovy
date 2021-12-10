package de.geobe.energy.data


import de.geobe.energy.acquire.Reading
import groovy.transform.AutoClone

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static java.lang.Math.max
import static java.lang.Math.min
import static de.geobe.energy.acquire.PvRecorder.STORAGE_INTERVAL

/**
 * Datatype class that holds average, min and max values of photovoltaic (pv)
 * production, consumption, battery state etc. aggregate over some time interval.<br>
 * This class can generate a db table where its objects can be stored
 */
@Entity
@AutoClone
class PvData {
//    static final int STORAGE_INTERVAL = 720 // store 12 hours
    static final int SECONDS_PER_MINUTE = 60
    static final int ZERO_INTERVAL = 10

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id
    LocalDateTime recordedAt
    /** pv production: average, min, max */
    Integer prodAvg = 0
    Integer prodMin = 0
    Integer prodMax = 0
    /** power consumption  */
    Integer consAvg = 0
    Integer consMin = 0
    Integer consMax = 0
    /** power from/to grid */
    Integer gridAvg = 0
    Integer gridMin = 0
    Integer gridMax = 0
    /** total power delivered to grid */
    Integer toGrid = 0
    /** total power obtained from grid */
    Integer fromGrid = 0
    /** loading/unloading battery */
    Integer battAvg = 0
    Integer battMin = 0
    Integer battMax = 0
    /** battery loading state [%] */
    Integer battLoad = 0

    /**
     * Aggregate a list of readings from S10 web interface to average, min and max values
     * @param sample a list of readings
     * @param timestamp externally calculated timestamp for this data
     * @return a new PvData object generated from the sample
     */
    static PvData fromReadings(List<Reading> sample, LocalDateTime timestamp) {
        def pvData = new PvData()
        pvData.recordedAt = timestamp
        pvData.battLoad = sample[0].batteryState
        pvData.prodMin = sample[0].production
        pvData.consMin = sample[0].consumption
        pvData.gridMin = sample[0].gridPower
        pvData.battMin = sample[0].batteryPower
        pvData.battLoad = sample[-1].batteryState
        sample.each { reading ->
            pvData.prodAvg += reading.production
            pvData.prodMax = max(pvData.prodMax, reading.production)
            pvData.prodMin = min(pvData.prodMin, reading.production)

            pvData.consAvg += reading.consumption
            pvData.consMax = max(pvData.consMax, reading.consumption)
            pvData.consMin = min(pvData.consMin, reading.consumption)

            pvData.gridAvg += reading.gridPower
            pvData.gridMax = max(pvData.gridMax, reading.gridPower)
            pvData.gridMin = min(pvData.gridMin, reading.gridPower)
            if (reading.gridPower > 0) {
                pvData.fromGrid += reading.gridPower
            } else {
                pvData.toGrid += reading.gridPower
            }
            pvData.battAvg += reading.batteryPower
            pvData.battMax = max(pvData.battMax, reading.batteryPower)
            pvData.battMin = min(pvData.battMin, reading.batteryPower)
        }

        pvData.prodAvg = pvData.prodAvg.intdiv(sample.size())
        pvData.consAvg = pvData.consAvg.intdiv(sample.size())
        pvData.gridAvg = pvData.gridAvg.intdiv(sample.size())
        pvData.battAvg = pvData.battAvg.intdiv(sample.size())

        pvData
    }

    /**
     * Aggregate a List of PvData objects, e.g. from database, into one.
     * @param pvDataList successive PvData objects to be aggregated
     * @return a new PvData object
     */
    static PvData aggregate(List<PvData> pvDataList) {
        def pvData = pvDataList[0].clone()
        pvData.battLoad = pvDataList[-1].battLoad
        pvDataList[1..<pvDataList.size()].each {
            pvData.prodAvg += it.prodAvg
            pvData.prodMin = min(pvData.prodMin, it.prodMin)
            pvData.prodMax = max(pvData.prodMax, it.prodMax)

            pvData.consAvg += it.consAvg
            pvData.consMin = min(pvData.consMin, it.consMin)
            pvData.consMax = max(pvData.consMax, it.consMax)

            pvData.gridAvg += it.gridAvg
            pvData.gridMin = min(pvData.gridMin, it.gridMin)
            pvData.gridMax = max(pvData.gridMax, it.gridMax)

            pvData.battAvg += it.battAvg
            pvData.battMin = min(pvData.battMin, it.battMin)
            pvData.battMax = max(pvData.battMax, it.battMax)

            pvData.fromGrid += it.fromGrid
            pvData.toGrid += it.toGrid

        }

        pvData.prodAvg = pvData.prodAvg.intdiv(pvDataList.size())
        pvData.consAvg = pvData.consAvg.intdiv(pvDataList.size())
        pvData.gridAvg = pvData.gridAvg.intdiv(pvDataList.size())
        pvData.battAvg = pvData.battAvg.intdiv(pvDataList.size())

        pvData
    }

    /**
     * Split a list of readings into several lists that contain only those readings read during an
     * interval of intervalMinutes. Intervals are tried to be adjusted to full hours + n*intervalMinutes.
     * The interval tag is taken as the full minute ending the interval
     * @param readings incoming list of readings, most recent first
     * @param intervalMinutes interval size in minutes
     * @return map of reading lists indexed with timestamp of last reading rounded to closest full minute
     */
    static Map<LocalDateTime, List<Reading>> intervalFilter(List<Reading> readings,
                                                            int intervalMinutes = STORAGE_INTERVAL) {
        def intervals = new TreeMap<LocalDateTime, List<Reading>>()
        // find start time
        def timesplit = readings[0].timestamp
        def second = timesplit.second
        def minute = timesplit.minute
        if (second > SECONDS_PER_MINUTE - ZERO_INTERVAL) {
            minute++
        }
        def tickOffset = minute % intervalMinutes
        // first minute that is a "tick" on the time axis
        def startMinute = minute - tickOffset
        timesplit = timesplit.minusMinutes(tickOffset).truncatedTo(ChronoUnit.MINUTES)
        // strip off leading records before the first tick interval
        def truncatedList = readings.dropWhile {
            it.timestamp > timesplit.plusSeconds(ZERO_INTERVAL)
        }
        while (truncatedList[-1].timestamp <= timesplit.minusMinutes(intervalMinutes).plusSeconds(ZERO_INTERVAL)) {
            def timeIndex = timesplit
            timesplit = timesplit.minusMinutes(intervalMinutes)
            def splitter = truncatedList.split {
                it.timestamp >= timesplit.plusSeconds(ZERO_INTERVAL)
            }
            intervals[timeIndex] = splitter[0]
            truncatedList = splitter[1]
        }
        intervals
    }
}