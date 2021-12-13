package de.geobe.energy.data

import de.geobe.energy.acquire.PvRecorder
import spock.lang.Shared
import spock.lang.Specification

class PvDataFilterTest extends Specification {

    // configure gpars classes
    @Shared
    def recorder = new PvRecorder(60)
    def filter = new PvDataFilter()
    // open db
    @Shared
    def db = PvDb.getPvDatabase(true)

    def setupSpec() {
        // put them into the PvRecorder instance
    }

    def cleanupSpec() {
        // close db
        db.shutdown()
    }

    def 'intervalFilter skips readings until correct starting minute'() {

    }
}
