package org.valkyrienskies.clockwork

import org.valkyrienskies.clockwork.kelvin.api.GasGraph
import org.valkyrienskies.clockwork.kelvin.impl.GasGraphImpl
import org.valkyrienskies.clockwork.kelvin.api.GasSimResultFrame
import org.valkyrienskies.clockwork.kelvin.impl.logger
import java.util.*
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.system.measureNanoTime

class KelvinBackground(var idealTickRate: Int = 40, var subSteps: Int = 10): Runnable {

    val gasGraph: GasGraph = GasGraphImpl()

    var crashed = false

    private var killTask = false

    private var lostTime: Long = 0
    private val prevKelvinTicksTimeMillis: Queue<Long> = LinkedList()

    val pauseLock = ReentrantLock()
    val shouldUnpauseKelvinTick = pauseLock.newCondition()

    override fun run() {
        try {
            while (true) {
                if (killTask) {
                    break
                }

                if (!ClockworkMod.isKelvinRunning) {
                    lostTime = 0
                    prevKelvinTicksTimeMillis.clear()

                    pauseLock.withLock {
                        shouldUnpauseKelvinTick.await()
                    }
                }

                crashed = false

                val timeToSimulateNs = 1e9 / idealTickRate.toDouble()
                val timeStep = timeToSimulateNs / 1e9
                var results: GasSimResultFrame
                val timeToRunSplitTick = measureNanoTime {
                    // Run the tick
                    results = gasGraph.tick(timeStep, subSteps)
                }

                //logger.warn("Time to run Split Tick: $timeToRunSplitTick")

                // Push the results to the main thread

                trackTickEnd()
                sleepOnLostTime(timeToSimulateNs, timeToRunSplitTick)
            }
        } catch (e: Exception) {
            logger.error("Error in Kelvin pipeline task", e)
            repeat(10) { logger.error("!!!!!!! KELVIN THREAD CRASHED !!!!!!!") }
            crashed = true
        }
        logger.info("Kelvin thread ending")
    }

    fun tellTaskToKillItself() {
        killTask = true
    }

    private fun trackTickEnd() {
        // Keep track of when physics tick finished
        val currentTimeMillis = System.currentTimeMillis()
        prevKelvinTicksTimeMillis.add(currentTimeMillis)
        // Remove physics ticks that were over [PHYS_TICK_AVERAGE_WINDOW_MS] ms ago
        while (prevKelvinTicksTimeMillis.isNotEmpty() &&
            prevKelvinTicksTimeMillis.peek() + KELVIN_TICK_AVERAGE_WINDOW_MS < currentTimeMillis
        ) {
            prevKelvinTicksTimeMillis.remove()
        }
    }

    private fun sleepOnLostTime(timeToSimulateNs: Double, timeToRunPhysTick: Long) {
        // Ideal time minus actual time to run physics tick
        val timeDif = timeToSimulateNs - timeToRunPhysTick

        if (timeDif < 0) {
            // Physics tick took too long, store some lost time to catch up
            lostTime = min(lostTime - timeDif.toLong(), MAX_LOST_TIME)
        } else {
            if (lostTime > timeDif) {
                // Catch up
                lostTime -= timeDif.toLong()
            } else {
                val timeToWait = timeDif - lostTime
                lostTime = 0
                sleepExact(timeToWait.toLong())
            }
        }
    }

    private fun sleepExact(sleepTimeNanos: Long) {
        val startTime = System.nanoTime()
        val timeToSleep = sleepTimeNanos - 1_000_000

        LockSupport.parkNanos(timeToSleep)
    }

    companion object {
        private const val MAX_LOST_TIME: Long = 1e9.toLong()
        private const val KELVIN_TICK_AVERAGE_WINDOW_MS: Long = 500
        private val logger by logger("The Cooker")
    }
}