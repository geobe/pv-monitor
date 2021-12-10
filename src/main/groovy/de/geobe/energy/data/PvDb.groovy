package de.geobe.energy.data;

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

    private static PvDb pvDatatbase
    private static debugMode = false
    static getDebugMode() {debugMode}

    static synchronized PvDb getPvDatabase(boolean debug = false) {
        if (!pvDatatbase) {
            pvDatatbase = new PvDb(debug)
        }
        pvDatatbase
    }

    /**
     * Default constructor starts H2 server and binds Entity class for PV data records
     * to the database using hibernate.
     */
    private PvDb(boolean debug) {
        debugMode = debug
        runServer(debug)
        if (debug) {
            pvDb = new DbHibernate(['de.geobe.energy.data.PvData', 'de.geobe.energy.acquire.Reading'])
            readingDao = new DaoHibernate<Reading>(Reading.class, pvDb)
        } else {
            pvDb = new DbHibernate(['de.geobe.energy.data.PvData'])
        }
        pvDao = new DaoHibernate<PvData>(PvData.class, pvDb)
    }

    /**
     * start H2 tcp server
     */
    def runServer(boolean debug) {
        try {
            if (debug) {
                tcpServer = Server.createTcpServer('-baseDir', './src/test/resources/h2', '-tcpAllowOthers',
                        '-tcpDaemon', '-ifNotExists', '-tcpPort', '9092')
            } else {
                tcpServer = Server.createTcpServer('-baseDir', '~/H2', '-tcpAllowOthers',
                        '-tcpDaemon', '-ifNotExists', '-tcpPort', '9092')
            }
            if (!tcpServer.isRunning(true)) {
                tcpServer.start()
                println "tcpServer startet"
            } else {
                println "tcpServer already running"
            }
        } catch (Exception ex) {
            println "tcpServer already running"
        }
        if (debug) {
            try {
                webServer = Server.createWebServer('-baseDir', './src/test/resources/h2', '-webAllowOthers',
                        '-webDaemon', '-webport', '8082', '-ifNotExists')
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
    }

    def shutdown() {
        tcpServer?.shutdown()
//        webServer?.shutdown()
        pvDb.closeDatabase()
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