package org.valkyrienskies.clockwork.util

import org.valkyrienskies.clockwork.content.forces.DragController

/**
 * Contains useful functions for features that need funny wind maths. Mainly drag and balloons.
 */
object AerodynamicUtils {

    /**
     * Returns the density of air at a Y value adapted to a real life altitude, where Y = 60 is sea level, and Y = 320 is the top of the Troposphere.
     *
     * Where:
     *
     * p = mass density (kg/m^3)
     *
     * pb = base density via b (kg/m^3)
     *
     * Tb = standard temperature via b (K)
     *
     * g0 = standard gravitational acceleration (m/s^2)
     *
     * h = altitude over sea level (m)
     *
     * hb = base altitude via b (m)
     *
     * R = universal gas constant (N-m/(mol-K))
     *
     * M = molar mass of Earth's air (kg/mol)
     *
     * L = temperature lapse rate (K/m)
     * @param y The Y value to get the air density for.
     * @param maxHeight The maximum Y value in the world.
     * @return The air density at the given Y value, in kg/m^3.
     * @see <a href="https://en.wikipedia.org/wiki/Barometric_formula">Wikipedia source for krabber patter formuler</a>
     */
    fun getAirDensityForY(y: Double, maxHeight: Double): Double {
        val worldScale = 11000.0 / (maxHeight - 63.0)

        val realAltitude = (y - 63.0) * worldScale

        val layer = when {
            realAltitude < 11000 -> 0
            realAltitude < 20000 -> 1
            realAltitude < 32000 -> 2
            realAltitude < 47000 -> 3
            realAltitude < 51000 -> 4
            realAltitude < 71000 -> 5
            else -> 6
        }

        val hb = when (layer) {
            0 -> 0.0
            1 -> 11000.0
            2 -> 20000.0
            3 -> 32000.0
            4 -> 47000.0
            5 -> 51000.0
            6 -> 71000.0
            else -> 0.0
        }

        val pb = when (layer) {
            0 -> 1.225
            1 -> 0.36391
            2 -> 0.08803
            3 -> 0.01322
            4 -> 0.00143
            5 -> 0.00086
            6 -> 0.000064
            else -> 0.0
        }

        val Tb = when (layer) {
            0 -> 288.15
            1 -> 216.65
            2 -> 216.65
            3 -> 228.65
            4 -> 270.65
            5 -> 270.65
            6 -> 214.65
            else -> 0.0
        }

        val g0 = GRAVITATIONAL_ACCELERATION

        val R = UNIVERSAL_GAS_CONSTANT

        val M = AIR_MOLAR_MASS

        val L = when (layer) {
            0 -> 0.0065
            1 -> 0.0
            2 -> -0.001
            3 -> -0.0028
            4 -> 0.0
            5 -> 0.0028
            6 -> 0.002
            else -> 0.0
        }

        return when (L != 0.0) {
            true -> pb * Math.pow((Tb - (realAltitude - hb) * L) / Tb, ((g0 * M) / (R * L) -1.0))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }
    }



    // useful values

    const val DRAG_COEFFICIENT = 3.15
    const val GRAVITATIONAL_ACCELERATION = 9.80665
    const val UNIVERSAL_GAS_CONSTANT = 8.3144598
    const val AIR_MOLAR_MASS = 0.0289644
}