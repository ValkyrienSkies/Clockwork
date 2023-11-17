package org.valkyrienskies.clockwork.util

import net.minecraft.util.Mth
import org.joml.Quaternionf


object SternerCopiumUtils {

    fun oldQuaternionf(pX: Float, pY: Float, pZ: Float): Quaternionf {
        val f = Mth.sin(0.5f * pX)
        val f1 = Mth.cos(0.5f * pX)
        val f2 = Mth.sin(0.5f * pY)
        val f3 = Mth.cos(0.5f * pY)
        val f4 = Mth.sin(0.5f * pZ)
        val f5 = Mth.cos(0.5f * pZ)
        val x = f * f3 * f5 + f1 * f2 * f4
        val y = f1 * f2 * f5 - f * f3 * f4
        val z = f * f2 * f5 + f1 * f3 * f4
        val w = f1 * f3 * f5 - f * f2 * f4
        return Quaternionf(x, y, z, w)
    }
}