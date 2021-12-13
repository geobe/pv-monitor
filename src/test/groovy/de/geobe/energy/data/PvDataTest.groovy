package de.geobe.energy.data

import de.geobe.energy.acquire.PvRecorder
import de.geobe.energy.acquire.Reading
import spock.lang.Shared
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PvDataTest extends Specification {

    // configure gpars classes
    @Shared
    def recorder = new PvRecorder(720)
    // open db
    @Shared
    def db = PvDb.getPvDatabase(true)

    @Shared
    Map<LocalDateTime, List<Reading>> rawIntervals
    @Shared
    List<PvData> level1Data

    def setupSpec() {
        // read a nice bunch of readings
        def readings = db.readingDao.find('from Reading r where r.id >= 2547')
        // put them into the PvRecorder instance
        readings.each { reading ->
            recorder << { addReading(reading) }
        }
    }

    def cleanupSpec() {
        // close db
        db.shutdown()
    }

    def 'we get independent lists of all selected readings'() {
        println 'we get independent lists of all selected readings'
        when: 'we retrieve all records from recorder'
        def notall = recorder.values
        notall.removeLast()
        notall.remove(0)
        def all = recorder.values
        then: 'size, first and last element should fit for right test data'
        notall.size() == 1060
        all.size() == 1062
        all[0].id == 3968
        all[-1].id == 2547
    }

    def 'split readings nicely into 5 minute slices'() {
        println 'split readings nicely into 5 minute slices'
        when: 'we retrieve all records from recorder'
        def all = recorder.values
        and: 'split them into 5 min lists'
        def slices = PvData.intervalFilter(all, 5)
        rawIntervals = slices
        def indexes = slices.keySet()
        def index0 = indexes[-1]
        def list0 = slices[index0]
        def indexEnd = indexes[0]
        def listEnd = slices[indexEnd]
        def sum = slices.values().collect { it.size() }.sum()
        then:
        sum == all.size() - 11 // strip 6 leading and 5 trailing outside 5 min intervals
        list0[0].id == 3959
        list0[-1].id == 3943
        listEnd[0].id == 2566
        listEnd[-1].id == 2553
        slices.every {
            def key = it.key
            def val = it.value
            def diff0 = key.until(val[0].timestamp, ChronoUnit.SECONDS)
            def diff1 = key.until(val[-1].timestamp, ChronoUnit.SECONDS)
//            println "start $diff0, end $diff1, values ${val.size()}"
            diff0 <= PvData.ZERO_INTERVAL &&
                    diff1 >= -(5 * 60 - PvData.ZERO_INTERVAL)
        }
    }

    def 'successfully build first interval only'() {
        when: 'we simulate reading PvData values one by one'
        def leading = recorder.values[0..34]
        def intervals = PvData.intervalFilter(leading, 5)
        def timestamps = intervals.keySet().asList()
        def i5 = PvData.intervalFilter(leading[6..18], 5, true)
        def i0 = PvData.intervalFilter(leading[18..30], 5, true)
        def i1 = PvData.intervalFilter(leading[19..30], 5, true)
        def i2 = PvData.intervalFilter(leading[18..29], 5, true)
        def i3 = PvData.intervalFilter(leading[6..19], 5, true)
        def i4 = PvData.intervalFilter(leading[5..18], 5, true)
        then:
        intervals.size() == 2
        PvData.intervalFilter(leading[21..34], 5, true).size() == 0
        PvData.intervalFilter(leading[0..34], 5, true).size() == 1
        PvData.intervalFilter(leading[17..34], 5, true).keySet().asList()[0] == timestamps[0]
        PvData.intervalFilter(leading[19..30], 5, true).size() == 0
        PvData.intervalFilter(leading[18..29], 5, true).size() == 0
        PvData.intervalFilter(leading[18..30], 5, true).size() == 1
        PvData.intervalFilter(leading[18..30], 5, true).keySet().asList()[0] == timestamps[0]
        PvData.intervalFilter(leading[0..17], 5, true).size() == 0
        PvData.intervalFilter(leading[6..18], 5, true).size() == 1
    }

    def 'aggregate Reading lists into meaningful values'() {
        println 'aggregate Reading lists into meaningful values'
        when: 'all raw value intervals are processed'
        List<PvData> aggregates = []
        if (rawIntervals) {
            rawIntervals.each {
                def timestamp = it.key
                def rawList = it.value
                def pvData = PvData.fromReadings(rawList, timestamp)
                aggregates << pvData
            }
        }
        level1Data = aggregates
        then: 'aggregation shall producew correct values'
        rawIntervals
        aggregates.size() == 99
        aggregates[11].prodAvg == 1160
        aggregates[11].prodMax == 1224
        aggregates[11].prodMin == 1114
        aggregates[11].consAvg == 3189
        aggregates[11].consMax == 4623
        aggregates[11].consMin == 369
        aggregates[11].battAvg == 251
        aggregates[11].battMax == 765
        aggregates[11].battMin == 0
        aggregates[11].battLoad == 2
        aggregates[11].gridAvg == 2279
        aggregates[11].gridMax == 3468
        aggregates[11].gridMin == -1
        aggregates[11].toGrid == -1
        aggregates[11].fromGrid == 13679
    }

    def 'PvData values can be aggregated correctly'() {
        println 'PvData values can be aggregated correctly'
        when: 'we aggregate some pv values'
        def shortList = level1Data[10..15]
        def size = shortList.size()
        def aggPv = PvData.aggregate(shortList)
        then:
        level1Data
        aggPv.recordedAt == shortList[-1].recordedAt
        aggPv.prodAvg == (shortList.collect { it.prodAvg }.sum()).intdiv(size)
        aggPv.prodMax == shortList.collect { it.prodMax }.max()
        aggPv.prodMin == shortList.collect { it.prodMin }.min()
        aggPv.consAvg == (shortList.collect { it.consAvg }.sum()).intdiv(size)
        aggPv.consMax == shortList.collect { it.consMax }.max()
        aggPv.consMin == shortList.collect { it.consMin }.min()
        aggPv.battAvg == (shortList.collect { it.battAvg }.sum()).intdiv(size)
        aggPv.battMax == shortList.collect { it.battMax }.max()
        aggPv.battMin == shortList.collect { it.battMin }.min()
        aggPv.battLoad == shortList[-1].battLoad
        aggPv.gridAvg == (shortList.collect { it.gridAvg }.sum()).intdiv(size)
        aggPv.gridMax == shortList.collect { it.gridMax }.max()
        aggPv.gridMin == shortList.collect { it.gridMin }.min()
        aggPv.toGrid == shortList.collect { it.toGrid }.sum()
        aggPv.fromGrid == shortList.collect { it.fromGrid }.sum()
    }

}
