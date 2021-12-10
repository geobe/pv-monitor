package de.geobe.energy.data

import de.geobe.energy.acquire.PvRecorder
import spock.lang.Shared
import spock.lang.Specification

import java.time.temporal.ChronoUnit

class PvDataTest extends Specification {

    // configure gpars classes
    @Shared
    def recorder = new PvRecorder(720)
    // open db
    @Shared
    def db = PvDb.getPvDatabase(true)

    def setupSpec() {
        // read a nice bunch of readings
        def readings = db.readingDao.find('from Reading r where r.id >= 2547')
        // put them into the PvRecorder instance
        readings.each {reading ->
            recorder << { addReading(reading) }
        }
    }

    def cleanupSpec() {
        // close db
        db.shutdown()
    }

    def 'we get independent lists of all selected readings'() {
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
        when: 'we retrieve all records from recorder'
        def all = recorder.values
        and: 'split them into 5 min lists'
        def slices = PvData.intervalFilter(all, 5)
        def indexes = slices.keySet()
        def index0 = indexes[-1]
        def list0 = slices[index0]
        def indexEnd= indexes[0]
        def listEnd = slices[indexEnd]
        def sum = slices.values().collect {it.size()}.sum()
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
            def diff1 = key.until(val[-1].timestamp , ChronoUnit.SECONDS)
//            println "start $diff0, end $diff1, values ${val.size()}"
             diff0 <= PvData.ZERO_INTERVAL &&
                    diff1 >= -(5 * 60 - PvData.ZERO_INTERVAL)
        }
    }

}
