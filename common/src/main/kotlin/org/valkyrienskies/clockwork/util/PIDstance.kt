package org.valkyrienskies.clockwork.util

class PIDstance(val p: Double = 1.0, val i: Double = 0.1, val d: Double = 0.05) {
    private var integral = 0.0
    private var lastError = 0.0

    val isIntegralReset: Boolean
        get() = integral == 0.0

    fun resetIntegral() {
        integral = 0.0
    }

    fun control(targetPressure: Double, currentPressure: Double, Kp: Double = p, Ki: Double = i, Kd: Double = d): Double {
        val error = targetPressure - currentPressure

        val proportional = Kp * error

        integral += error / 20.0
        val integralTerm = Ki * integral

        val derivative = (error - lastError) / 20.0
        val derivativeTerm = Kd * derivative

        lastError = error

        return proportional + integralTerm + derivativeTerm
    }
}