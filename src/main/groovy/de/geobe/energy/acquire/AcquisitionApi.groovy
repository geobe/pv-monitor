package de.geobe.energy.acquire

import de.geobe.energy.data.PvDataFilter
import de.geobe.energy.data.PvDb

class AcquisitionApi {

    def pvMonitor = new PvMonitor()
    def dataFilter = new PvDataFilter()

    private static AcquisitionApi acquisitionApi

    static synchronized getApi() {
        if(!acquisitionApi) {
            acquisitionApi = new AcquisitionApi()
        }
        acquisitionApi
    }

    private AcquisitionApi() {
        PvDb.getPvDatabase()      // init singleton
        pvMonitor.addPvDataProcessor(dataFilter)
        dataFilter.start()
        pvMonitor.start()
    }

    ArrayList<Reading> getValues() {
        pvMonitor?.recorder.values()
    }

    def stop() {
        pvMonitor.sendAndWait(new Terminator())
    }

    static void main(String[] args) {

    }
}
