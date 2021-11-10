package de.geobe.energy.acquire

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Datatype class containing data of a single reading from S10 web interface
 */
@Entity
class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id
    LocalDateTime timestamp
    int production
    int consumption
    int batteryPower
    int batteryState
    int gridPower
    static formatter = DateTimeFormatter.ofPattern('d.M.yyyy, HH:mm:ss')

    /**
     * Initialize from map read from the web interface
     * @param v reding of web page
     */
    Reading(Map v) {
        timestamp = LocalDateTime.parse(v.timestamp, formatter)
        production = Integer.parseInt(v.solarProd)
        consumption = Integer.parseInt(v.consumption)
        batteryPower = Integer.parseInt(v.batPower)
        batteryState = Integer.parseInt(v.batState)
        gridPower = Integer.parseInt(v.gridPower)
    }

    Reading() {}

    @Override
    String toString() {
        return "@ $timestamp: p = $production, c = $consumption, load = $batteryPower," +
                " state = $batteryState %, grid = $gridPower"
    }
}