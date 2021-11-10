package de.geobe.energy.persist;

import de.geobe.architecture.persist.DaoHibernate
import de.geobe.architecture.persist.DbHibernate
import de.geobe.energy.acquire.Reading
import org.h2.tools.Server;

public class PvDb {

    Server tcpServer
    Server webServer
    DbHibernate pvDb
    DaoHibernate<PvData> pvDao
    DaoHibernate<Reading> readingDao

    static private PvDb pvDatatbase

    static synchronized PvDb getPvDatabase() {
        if (!pvDatatbase) {
            pvDatatbase = new PvDb()
        }
        pvDatatbase
    }

    /**
     * Default constructor starts H2 server and binds Entity class for PV data records
     * to the database using hibernate.
     */
    PvDb() {
        runServer()
        pvDb = new DbHibernate(['de.geobe.energy.persist.PvData', 'de.geobe.energy.acquire.Reading'])
        pvDao = new DaoHibernate<PvData>(PvData.class, pvDb)
        readingDao = new DaoHibernate<Reading>(Reading.class, pvDb)
    }

    /**
     * start H2 tcp and web server
     * @return
     */
    def runServer() {
        try {
            tcpServer = Server.createTcpServer('-baseDir', '~/H2', '-tcpAllowOthers',
                    '-tcpDaemon', '-ifNotExists', '-tcpPort', '9092')
            if (!tcpServer.isRunning(true)) {
                tcpServer.start()
                println "tcpServer startet"
            } else {
                println "tcpServer already running"
            }
        } catch (Exception ex) {
            println "tcpServer already running"
        }
        try {
            webServer = Server.createWebServer('-baseDir', '~/H2', '-webAllowOthers',
                    '-webDaemon', '-ifNotExists')
            if (!webServer.isRunning(true)) {
                webServer.start()
                println "webserver startet"
            } else {
                println "webServer already running"
            }
        } catch (Exception ex) {
            println "webServer already running"
        }
    }

    def saveToDb(PvData pvData) {
        pvDao.save(pvData)
        pvDao.commit()
    }

    def saveToDb(Reading reading) {
        readingDao.save(reading)
        readingDao.commit()
    }

    def clearPvData() {
        pvDao.deleteAll()
    }

}