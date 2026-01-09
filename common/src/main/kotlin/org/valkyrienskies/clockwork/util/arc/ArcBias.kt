package org.valkyrienskies.clockwork.util.arc

import net.minecraft.world.phys.Vec3

sealed interface ArcBias {
    object None : ArcBias
    data class Parabola(
        val direction: Vec3,   // world-space direction to bow toward (e.g. Vec3(0,-1,0))
        val strength: Double   // peak offset in blocks (e.g. 2.5)
    ) : ArcBias
    data class DoubleHump(
        val direction: Vec3,
        val strength: Double
    ) : ArcBias
    data class SineWave(
        val direction: Vec3,
        val strength: Double,
        val frequency: Double
    ) : ArcBias
    data class Spiral(
        val turns: Double = 2.0,            // how many rotations from start->end
        val radius: Double = 1.0,           // base radius in blocks
        val phase: Double = 0.0,            // radians
        val axis: Vec3? = null,             // if null, uses (end-start)
        val radiusStartMul: Double = 0.0,   // 0 = start tight, 1 = start full radius
        val radiusEndMul: Double = 1.0      // 0 = end tight, 1 = end full radius
    ) : ArcBias
    data class DoubleHelix(
        val turns: Double = 2.0,
        val radius: Double = 1.0,
        val phase: Double = 0.0,
        val axis: Vec3? = null,
        val strandSeparation: Double = 0.45 // 0..1, how strong the 2-strand modulation is
    ) : ArcBias
}

