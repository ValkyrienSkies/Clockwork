package org.valkyrienskies.clockwork

import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object ClockworkDamageTypes {
    private val WANDERLITE_LIGHTNING_KEY = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation(ClockworkMod.MOD_ID, "wanderlite_lightning")
    )

    private val SOLVER_KEY = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation(ClockworkMod.MOD_ID, "solver")
    )

    fun wanderliteLightning(registryAccess: RegistryAccess, victim: Entity?): DamageSource {
        val holder = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WANDERLITE_LIGHTNING_KEY)
        return DamageSource(holder, victim)
    }

    fun solver(registryAccess: RegistryAccess, victim: Entity?): DamageSource {
        val holder = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOLVER_KEY)
        return DamageSource(holder, victim)
    }

    fun init() {

    }
}
