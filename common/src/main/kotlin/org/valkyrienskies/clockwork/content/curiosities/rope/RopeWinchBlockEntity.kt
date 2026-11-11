package org.valkyrienskies.clockwork.content.curiosities.rope

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks

class RopeWinchBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(type, pos, state) {
    lateinit var maximum: ScrollValueBehaviour
    private var ropeId: java.util.UUID? = null

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        maximum = ScrollValueBehaviour(Component.translatable("block.vs_clockwork.rope_winch.maximum"), this, Slot())
            .between(1, 128).withFormatter { "$it m" }.requiresWrench()
        maximum.setValue(32)
        behaviours.add(maximum)
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        val data = WandLinks.get(world)
        val link = data.links[ropeId] ?: data.links.values.firstOrNull { !it.spring && it.physical &&
            ((it.a.winch && it.a.pos == blockPos) || (it.b.winch && it.b.pos == blockPos)) } ?: return
        ropeId = link.id
        data.updateWinchRelease(world, link)
        if (link.releasedMaximum != null) return
        val next = nextLength(link.length, speed.toDouble(), maximum.value)
        if (next == link.length) return
        data.resizeRope(world, link, next)
    }

    override fun calculateStressApplied(): Float {
        lastStressApplied = 8f
        return lastStressApplied
    }

    class Slot : ValueBoxTransform.Sided() {
        override fun getSouthLocation() = Vec3(0.5, 0.5, 1.0)
        override fun isSideActive(state: BlockState, direction: Direction) =
            direction.axis.isHorizontal && direction.axis != state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)
    }

    companion object {
        fun releasedLength(length: Double, distance: Double, maximum: Double): Double =
            if (!distance.isFinite()) length else maxOf(length, distance.coerceAtMost(maximum))

        fun nextLength(length: Double, rpm: Double, maximum: Int): Double {
            if (!rpm.isFinite() || rpm == 0.0) return length
            val delta = (rpm / 512.0).coerceIn(-0.25, 0.25)
            return if (delta > 0) (length + delta).coerceAtMost(maxOf(length, maximum.toDouble()))
                else (length + delta).coerceAtLeast(0.5)
        }
    }
}
