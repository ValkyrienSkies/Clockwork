package org.valkyrienskies.clockwork.util.arc

import net.minecraft.world.phys.Vec3

data class LightningBolt(
    val startProvider: () -> Vec3,
    val endProvider: () -> Vec3,
    val seed: Long,
    val birthGameTime: Long,
    val lifeTicks: Int = 6,
    val thickness: Float = 0.06f,
    val maxOffset: Double = 0.8,
    val subdivisions: Int = 6,
    val branchChance: Double = 0.18,
    val branchScale: Double = 0.25,
    val branchTaper: Boolean = false,
    val arcBias: ArcBias = ArcBias.None
) {
    fun alive(now: Long) = (now - birthGameTime) <= lifeTicks
}
