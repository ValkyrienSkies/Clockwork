package org.valkyrienskies.clockwork

import net.minecraft.world.damagesource.DamageSource

object ClockworkDamageSources {

    @JvmStatic
    val GAS_EXPLOSION = ClockworkDamageSource("gas_explosion")

    @JvmStatic
    val GAS_BURN = ClockworkDamageSource("gas_burn")

    fun init() {
        GAS_EXPLOSION.setExplosion()
    }

    class ClockworkDamageSource: DamageSource {
        constructor(name: String) : super(name)
    }
}