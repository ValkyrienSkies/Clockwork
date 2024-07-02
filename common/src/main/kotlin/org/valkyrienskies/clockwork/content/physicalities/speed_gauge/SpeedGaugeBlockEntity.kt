package org.valkyrienskies.clockwork.content.physicalities.speed_gauge

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.Components
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.max
import kotlin.math.min

class SpeedGaugeBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?): SmartBlockEntity(typeIn, pos, state), IHaveGoggleInformation {

    var current = 0.0;
    var target = 0.0;

    val maxSpeed = 100.0;
    val changeSpeed = 0.15;

    override fun addToGoggleTooltip(tooltip: MutableList<Component?>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Components.empty())
        tooltip.add(Components.literal("Speed:")
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Components.literal(getShipSpeed().toInt().toString()+" m/s")
            .withStyle(ChatFormatting.GOLD))


        return true
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()
        println("Hello")

        target = getShipSpeed()/maxSpeed

        current +=  max(min(target-current,changeSpeed),-changeSpeed)

        print(target)
        print(" ")
        println(current)


    }



    fun getShipSpeed(): Double {
        var ship = level.getShipManagingPos(blockPos)
        if (ship != null) {
            return ship.velocity.length()
        }
        return 0.0
    }

}