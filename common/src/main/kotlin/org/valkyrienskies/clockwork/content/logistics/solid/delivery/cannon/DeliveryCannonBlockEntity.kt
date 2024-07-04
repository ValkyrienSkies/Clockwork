package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.FrequencySlotBehaviour

class DeliveryCannonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(type, pos,
    state
) {

    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour

    override fun tick() {
        super.tick()

        val chute = ActiveChutes.getNearestChuteWithFrequency(blockPos,100.0,frequencySlotBehaviour.frequency)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        frequencySlotBehaviour = FrequencySlotBehaviour(this, FrequencySlot())

        behaviours.add(frequencySlotBehaviour)
        super.addBehaviours(behaviours)
        return
    }


    public class FrequencySlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(state) else Vec3(.5, 10.5 / 16f, .5).add(
                VecHelper.rotate(
                    VecHelper.voxelSpace(0.0, 0.0, -5.0), angle(state).toDouble(), Direction.Axis.Y
                )
            )
        }

        override fun rotate(state: BlockState, ms: PoseStack) {
            if (direction != Direction.UP) {
                super.rotate(state, ms)
                return
            }
            TransformStack.cast(ms)
                .rotateY(angle(state).toDouble())
                .rotateX(90.0)
        }

        private fun angle(state: BlockState): Float {
            return if (AllBlocks.WEIGHTED_EJECTOR.has(state)) AngleHelper.horizontalAngle(state.getValue(EjectorBlock.HORIZONTAL_FACING)) else 0f
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            return direction != Direction.UP && direction != Direction.DOWN
        }

        override fun getSouthLocation(): Vec3 {
            return if (direction == Direction.UP) Vec3.ZERO else VecHelper.voxelSpace(8.0, 6.0, 15.5)
        }
    }

}