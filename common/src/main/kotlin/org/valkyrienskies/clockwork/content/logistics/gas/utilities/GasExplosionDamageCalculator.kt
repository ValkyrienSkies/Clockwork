package org.valkyrienskies.clockwork.content.logistics.gas.utilities

import net.minecraft.core.BlockPos
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import java.util.*

class GasExplosionDamageCalculator : ExplosionDamageCalculator() {

    override fun getBlockExplosionResistance(
        explosion: Explosion,
        reader: BlockGetter,
        pos: BlockPos,
        state: BlockState,
        fluid: FluidState
    ): Optional<Float> {
        if (state.block is INodeBlock) {
            return Optional.of(0.0f)
        }
        return super.getBlockExplosionResistance(explosion, reader, pos, state, fluid)
    }

    override fun shouldBlockExplode(
        explosion: Explosion,
        reader: BlockGetter,
        pos: BlockPos,
        state: BlockState,
        power: Float
    ): Boolean {
        if (state.block is INodeBlock) {
            return true
        }
        return super.shouldBlockExplode(explosion, reader, pos, state, power)
    }

    companion object {
        //val GAS_EXPLOSION : DamageSource
    }
}