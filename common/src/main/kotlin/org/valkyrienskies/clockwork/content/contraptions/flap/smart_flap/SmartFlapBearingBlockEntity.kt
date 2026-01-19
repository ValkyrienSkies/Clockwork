package org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkBehaviour
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.FlapBearingFrequencySlot

class SmartFlapBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState): FlapBearingBlockEntity(type,pos,state) {

    var firstReceivedSignal = 0
    var secondReceivedSignal = 0

    private var linkFirst: DualLinkBehaviour? = null
    private var linkSecond: DualLinkBehaviour? = null

    override fun getPower(): Int {
        if (firstReceivedSignal == 0 && secondReceivedSignal == 0) return super.getPower()
        if (!blockState.hasProperty(BlockStateProperties.FACING)) return 0

        // This needs to be done, otherwise flap contraption will turn the wrong way
        val facing = blockState.getValue(BlockStateProperties.FACING)
        if (facing == Direction.EAST || facing == Direction.SOUTH) return secondReceivedSignal-firstReceivedSignal


        return firstReceivedSignal-secondReceivedSignal
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        createSmartFlap()
        behaviours.add(linkFirst!!)
        behaviours.add(linkSecond!!)
    }

    private fun createSmartFlap() {
        val valueBoxes = ValueBoxTransform.Dual.makeSlots { first: Boolean -> FlapBearingFrequencySlot(first, true) }
        val valueBoxesSecond = ValueBoxTransform.Dual.makeSlots { first: Boolean -> FlapBearingFrequencySlot(first, false) }

        linkFirst = DualLinkBehaviour(this, valueBoxes, {setFirstSignal(it)}, true)
        linkSecond = DualLinkBehaviour(this, valueBoxesSecond, {setSecondSignal(it)},false)
    }

    fun setFirstSignal(power: Int) {
        firstReceivedSignal = power
    }

    fun setSecondSignal(power: Int) {
        secondReceivedSignal = power
    }

    override fun getMaxSize(): Int {
        return ClockworkConfig.SERVER.smartFlapBearingMaxSize
    }

}