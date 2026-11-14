package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

class HarpoonBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(type, pos, state) {
    var loadedHarpoon: ItemStack = ItemStack.EMPTY
        private set
    var charge = 0.0
        private set
    var ropeId: UUID? = null
    private var firedThisActivation = false

    fun load(stack: ItemStack): Boolean {
        if (!loadedHarpoon.isEmpty || stack.isEmpty || !stack.`is`(ClockworkItems.HARPOON.get())) return false
        loadedHarpoon = stack.copyWithCount(1)
        setChanged()
        sendData()
        return true
    }

    fun unload(): ItemStack {
        val result = loadedHarpoon
        loadedHarpoon = ItemStack.EMPTY
        setChanged()
        sendData()
        return result
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        val links = WandLinks.get(world)
        if (ropeId != null && links.links[ropeId]?.a?.pos != blockPos) {
            ropeId = null
            setChanged()
            sendData()
        }
        val rpm = if (isOverStressed) 0.0 else getSpeed().toDouble()
        val rope = links.links[ropeId]
        if (rope != null) {
            // Release first on a falling edge; an attached rope uses shaft power to reel instead of charge.
            redstoneChanged()
            if (ropeId == rope.id && !rope.suspended) {
                val length = RopeWinchBlockEntity.nextLength(rope.length, -abs(rpm), HarpoonBallistics.MAX_RANGE.toInt())
                if (length != rope.length) links.resizeRope(world, rope, length)
            }
            return
        }
        val next = nextCharge(charge, rpm)
        if (next != charge) {
            charge = next
            setChanged()
            if (world.gameTime % 5L == 0L || charge == 1.0) sendData()
        }
        redstoneChanged()
    }

    /** Also called directly by neighbor updates so a short falling edge releases immediately. */
    fun redstoneChanged() {
        val world = level as? ServerLevel ?: return
        val powered = world.hasNeighborSignal(blockPos)
        if (blockState.getValue(HarpoonBlock.POWERED) != powered) {
            world.setBlock(blockPos, blockState.setValue(HarpoonBlock.POWERED, powered), 2)
        }
        if (!powered) {
            if (firedThisActivation) {
                firedThisActivation = false
                setChanged()
            }
            if (ropeId != null || HarpoonProjectiles.hasProjectile(world, blockPos)) release()
            return
        }
        if (!canFire(powered, firedThisActivation, !loadedHarpoon.isEmpty, charge) ||
            ropeId != null || HarpoonProjectiles.hasProjectile(world, blockPos)) return
        if (!HarpoonProjectiles.fire(world, this, charge)) return
        // Consume ammunition and charge only after physics accepted the launch.
        loadedHarpoon = ItemStack.EMPTY
        charge = 0.0
        firedThisActivation = true
        setChanged()
        sendData()
    }

    fun release() {
        val world = level as? ServerLevel ?: return
        HarpoonProjectiles.detach(world, this)
    }

    override fun invalidate() {
        (level as? ServerLevel)?.let { HarpoonProjectiles.cancelProjectile(it, this) }
        super.invalidate()
    }

    override fun calculateStressApplied(): Float {
        lastStressApplied = 8f
        return lastStressApplied
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking)
        tooltip.add(Component.translatable("tooltip.vs_clockwork.harpoon.charge", (charge * 100).roundToInt()))
        tooltip.add(Component.translatable("tooltip.vs_clockwork.harpoon.${if (loadedHarpoon.isEmpty) "empty" else "loaded"}"))
        return true
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.put("Harpoon", loadedHarpoon.save(CompoundTag()))
        tag.putDouble("Charge", charge)
        tag.putBoolean("Fired", firedThisActivation)
        ropeId?.let { tag.putUUID("Rope", it) }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        loadedHarpoon = ItemStack.of(tag.getCompound("Harpoon")).let {
            if (it.`is`(ClockworkItems.HARPOON.get())) it.copyWithCount(1) else ItemStack.EMPTY
        }
        charge = sanitizeCharge(tag.getDouble("Charge"))
        firedThisActivation = tag.getBoolean("Fired")
        ropeId = if (tag.hasUUID("Rope")) tag.getUUID("Rope") else null
    }

    companion object {
        const val MIN_CHARGE = 0.1

        fun sanitizeCharge(charge: Double) = if (charge.isFinite()) charge.coerceIn(0.0, 1.0) else 0.0

        /** Five seconds at 128 RPM; either rotation direction winds the launcher. */
        fun nextCharge(charge: Double, rpm: Double): Double {
            val current = sanitizeCharge(charge)
            if (!rpm.isFinite()) return current
            return (current + abs(rpm) / 12800.0).coerceAtMost(1.0)
        }

        fun canFire(powered: Boolean, fired: Boolean, loaded: Boolean, charge: Double) =
            powered && !fired && loaded && charge.isFinite() && charge >= MIN_CHARGE
    }
}
