package de.geobe.energy.acquire

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

/**
 * Convert an array of readings into arrays of time series of average, min, max data
 */
class PvDataSeries {
    LocalDateTime start
    LocalDateTime end
    Duration stepsize

    PvDataSeries(List<Reading> readings, long timeStep = 1, TemporalUnit timeUnit = ChronoUnit.MINUTES) {
        stepsize = Duration.of(timeStep, timeUnit)
        start = readings[0].timestamp.plus(stepsize).truncatedTo(timeUnit)
        end = readings[-1].timestamp.truncatedTo(timeUnit)

    }
}
