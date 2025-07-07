package org.valkyrienskies.clockwork.content.logistics.gas.exhaust

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.floor
import kotlin.random.Random

class ExhaustBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    fun randomPos(deviation: Double, random: RandomSource): Double {
        return (0.5-deviation/2.0)+random.nextDouble()*deviation
    }

    override fun tick() {
        super.tick()

        val network = KelvinMod.getKelvinByPlatform() ?: return
        val gasses = network.getGasMassAt(getDuctNodePosition())
        if (gasses.isEmpty()) return super.tick()

        if (level!!.isClientSide) {
            val facing = level!!.getBlockState(blockPos).getValue(BlockStateProperties.FACING)
            val random = level!!.random

            println(gasses.values.sum().toInt())
            for (i in 1..floor(gasses.values.sum()/10).toInt()) {
                KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, getDuctNodePosition(),
                    blockPos.toJOMLD().add(randomPos(0.3, random), randomPos(0.3, random), randomPos(0.3, random)),
                    facing.normal.toJOMLD().mul(0.1))
            }

            return super.tick()
        }

        for ((gas,value) in gasses) {
            network.modGasMass(getDuctNodePosition(), gas, -value)
        }



    }
}