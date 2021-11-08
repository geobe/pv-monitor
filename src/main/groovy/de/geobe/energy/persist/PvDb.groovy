package de.geobe.energy.persist;

import de.geobe.architecture.persist.DaoHibernate
import de.geobe.architecture.persist.DbHibernate
import org.h2.tools.Server;

public class PvDb {

    Server tcpServer
    Server webServer
    DbHibernate pvDb
    DaoHibernate<PvData> pvDao

    /**
     * Default constructor starts H2 server and binds Entity class for PV data records
     * to the database using hibernate.
     */
    PvDb() {
        runServer()
        pvDb = new DbHibernate(['de.geobe.energy.persist.PvData'])
        pvDao = new DaoHibernate<PvData>(PvData.class, pvDb)
    }

    /**
     * start H2 tcp and web server
     * @return
     */
    def runServer() {
        tcpServer = Server.createTcpServer('-baseDir', '~/H2', '-tcpAllowOthers',
                '-tcpDaemon', '-ifNotExists')
        if (!tcpServer.isRunning(true)) {
            tcpServer.start()
            println "tcpServer startet"
        } else {
            println "tcpServer already running"
        }
        webServer = Server.createWebServer('-baseDir', '~/H2', '-webAllowOthers',
                '-webDaemon', '-ifNotExists')
        if (!webServer.isRunning(true)) {
            webServer.start()
            println "webserver startet"
        } else {
            println "webServer already running"
        }
    }

    def saveToDb(PvData pvData) {
        pvDao.save(pvData)
        pvDao.commit()
    }

}