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
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import java.util.UUID

class RopeWinchBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(type, pos, state), IThinkable {
    lateinit var maximum: ScrollValueBehaviour
    private var ropeId: UUID? = null

    override fun acceptsThinkable(side: Direction) = side == Direction.UP
    override fun providesThinkable(side: Direction) = side.axis.isHorizontal &&
        side.axis != blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)
    override fun thinkablePortName(side: Direction): Component? = when {
        acceptsThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.rope_target")
        providesThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.rope_length")
        else -> null
    }
    override fun thinkableValue(side: Direction): Double? {
        val world = level as? ServerLevel ?: return null
        return rope(WandLinks.get(world))?.takeUnless { it.suspended }?.length?.takeIf { providesThinkable(side) }
    }

    private fun rope(data: WandLinks): WandLinks.Link? {
        val link = data.links[ropeId] ?: data.links.values.firstOrNull { !it.spring && !it.thinkable && it.physical &&
            ((it.a.winch && it.a.pos == blockPos) || (it.b.winch && it.b.pos == blockPos)) }
        ropeId = link?.id
        return link
    }

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
        val link = rope(data) ?: return
        data.updateWinchRelease(world, link)
        if (link.releasedMaximum != null) return
        val input = ThinkableConnections.input(world, blockPos, Direction.UP)
        val next = if (input == null) nextLength(link.length, getSpeed().toDouble(), maximum.value)
            else nextTargetLength(link.length, getSpeed().toDouble(), maximum.value, input.value)
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
        fun nextTargetLength(length: Double, rpm: Double, maximum: Int, target: Double?): Double {
            if (!rpm.isFinite() || target == null || !target.isFinite()) return length
            val step = (kotlin.math.abs(rpm) / 512.0).coerceAtMost(0.25)
            return length + (target.coerceIn(0.5, maximum.toDouble()) - length).coerceIn(-step, step)
        }

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
