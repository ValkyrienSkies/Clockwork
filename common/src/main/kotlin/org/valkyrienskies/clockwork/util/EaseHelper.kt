package org.valkyrienskies.clockwork.util

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object EaseHelper {
    /**
     * Ease In Sine + Ease Out Sine + Ease In/Out Sine
     */
    // Sine In
    fun easeInSine(x: Float): Float {
        return (1 - cos(x * Math.PI / 2)).toFloat()
    }

    // Sine Out
    fun easeOutSine(x: Float): Float {
        return sin(x * Math.PI / 2).toFloat()
    }

    // Sine In/Out
    fun easeInOutSine(x: Float): Float {
        return (-(cos(Math.PI * x) - 1) / 2).toFloat()
    }

    /**
     * Ease In Quad + Ease Out Quad + Ease In/Out Quad
     */
    // Quad In
    fun easeInQuad(x: Float): Float {
        return x * x
    }

    // Quad Out
    fun easeOutQuad(x: Float): Float {
        return 1 - (1 - x) * (1 - x)
    }

    // Quad In/Out
    fun easeInOutQuad(x: Float): Float {
        return if (x < 0.5) 2 * x * x else (1 - (-2 * x + 2).toDouble().pow(2.0) / 2).toFloat()
    }

    /**
     * Ease In Exponential + Ease Out Exponential + Ease In/Out Exponential
     */
    // Exponential In
    fun easeInExpo(x: Float): Float {
        return if (x == 0f) 0f else 2.0.pow((10 * x - 10).toDouble()).toFloat()
    }

    // Exponential Out
    fun easeOutExpo(x: Float): Float {
        return if (x == 1f) 1f else (1 - 2.0.pow((-10 * x).toDouble())).toFloat()
    }

    // Exponential In/Out
    fun easeInOutExpo(x: Float): Float {
        return if (x == 0f) {
            0f
        } else {
            if (x == 1f)
                1.0f
            else if (x < 0.5)
                2.0.pow((20 * x - 10).toDouble()).toFloat() / 2
            else
                (2 - 2.0.pow((-20 * x + 10).toDouble()).toFloat()) / 2
        }

    }

    /**
     * Ease In Overshoot + Ease Out Overshoot + Ease In/Out Overshoot
     */
    // Overshoot In
    fun easeInOvershoot(x: Float): Float {
        val c1 = 1.70158
        val c3 = c1 + 1
        return (c3 * x * x * x - c1 * x * x).toFloat()
    }

    // Overshoot Out
    fun easeOutOvershoot(x: Float): Float {
        val c1 = 1.70158
        val c3 = c1 + 1
        return (1 + c3 * (x - 1).toDouble().pow(3.0) + c1 * (x - 1).toDouble().pow(2.0)).toFloat()
    }

    // Overshoot In/Out
    fun easeInOutOvershoot(x: Float): Float {
        val c1 = 1.70158
        val c2 = c1 * 1.525
        return if (x < 0.5) ((2 * x).toDouble()
            .pow(2.0) * ((c2 + 1) * 2 * x - c2)).toFloat() / 2 else ((2 * x - 2).toDouble()
            .pow(2.0) * ((c2 + 1) * (x * 2 - 2) + c2) + 2).toFloat() / 2
    }

    /**
     * Ease In Elastic + Ease Out Elastic + Ease In/Out Elastic
     */
    // Elastic In
    fun easeInElastic(x: Float): Float {
        val c4 = 2 * Math.PI / 3
        return if (x == 0f) 0f else if (x == 1f) 1f else -(Math.pow(
            2.0,
            (10 * x - 10).toDouble()
        ) * Math.sin((x * 10 - 10.75) * c4)).toFloat()
    }

    // Elastic Out
    fun easeOutElastic(x: Float): Float {
        val c4 = 2 * Math.PI / 3
        return if (x == 0f) 0f else if (x == 1f) 1f else (2.0.pow((-10 * x).toDouble()) * sin((x * 10 - 0.75) * c4) + 1).toFloat()
    }

    // Elastic In/Out
    fun easeInOutElastic(x: Float): Float {
        val c5 = 2 * Math.PI / 4.5
        return if (x == 0f) 0f else if (x == 1f) 1f else if (x < 0.5) -(2.0.pow((20 * x - 10).toDouble()) * sin((20 * x - 11.125) * c5)).toFloat() / 2 else (2.0.pow(
            (-20 * x + 10).toDouble()
        ) * sin((20 * x - 11.125) * c5)).toFloat() / 2 + 1
    }

    /**
     * Bounce In + Bounce Out + Bounce In/Out
     */
    // Bounce In
    fun easeInBounce(x: Float): Float {
        return 1 - easeOutBounce(1 - x)
    }

    // Bounce Out
    fun easeOutBounce(x: Float): Float {
        val n1 = 7.5625
        val d1 = 2.75
        return if (x < 1 / d1) {
            (n1 * x * x).toFloat()
        } else if (x < 2 / d1) {
            (n1 * (x - 1.5 / d1) * x + 0.75).toFloat()
        } else if (x < 2.5 / d1) {
            (n1 * (2.25 / d1.let { (x - it).toFloat(); x }) * x + 0.9375).toFloat()
        } else {
            (n1 * (2.625 / d1.let { (x - it).toFloat(); x }) * x + 0.984375).toFloat()
        }
    }

    // Bounce In/Out
    fun easeInOutBounce(x: Float): Float {
        return if (x < 0.5) (1 - easeOutBounce(1 - 2 * x)) / 2 else (1 + easeOutBounce(2 * x - 1)) / 2
    }
}