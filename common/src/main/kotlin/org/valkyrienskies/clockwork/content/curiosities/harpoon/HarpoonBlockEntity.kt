package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.item.TooltipHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.CommonComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import java.util.UUID
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.floor

class HarpoonBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(type, pos, state) {
    var loadedHarpoon: ItemStack = ItemStack.EMPTY
        private set
    var charge = 0.0
        private set
    var ropeId: UUID? = null
    private var firedThisActivation = false
    private var projectileInFlight = false
    private var ropeLength = 0.0
    private var tooltipSyncPending = false
    private var shotStartedAt = -1L
    private var projectileSpeed = 0.0
    private var launchSpeed = 0.0
    private var clientShotStartedAt = -1L
    private var cogAngle = 0.0
    private var previousCogAngle = 0.0

    fun projectileLaunched(speed: Double) {
        shotStartedAt = level?.gameTime ?: -1L
        projectileSpeed = speed.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        launchSpeed = projectileSpeed
        setProjectileInFlight(true)
    }

    /** Actual VS speed drives the payout cog, including changes during flight. */
    fun updateProjectileSpeed(speed: Double) {
        projectileSpeed = speed.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        if ((level?.gameTime ?: 0L) % 2L == 0L) sendData()
    }

    fun getCogAngle(partialTicks: Float): Float =
        (previousCogAngle + (cogAngle - previousCogAngle) * partialTicks).toFloat()

    fun getRecoil(partialTicks: Float): Float {
        if (clientShotStartedAt < 0) return 0f
        val elapsed = (level?.gameTime ?: 0L) - clientShotStartedAt + partialTicks.toDouble()
        if (elapsed < 0.0 || elapsed >= 12.0) return 0f
        val progress = if (elapsed < 3.0) {
            val t = elapsed / 3.0
            1.0 - (1.0 - t) * (1.0 - t)
        } else {
            val t = (elapsed - 3.0) / 9.0
            (1.0 - t) * (1.0 - t)
        }
        return (progress * 2.0 / 16.0).toFloat()
    }

    private fun tickAnimation() {
        previousCogAngle = cogAngle
        // Model cog radius is nine pixels. Convert flight metres/second to radians/tick.
        val shotAge = (level?.gameTime ?: 0L) - clientShotStartedAt
        // A nearby impact may arrive before the first client tick. Keep a brief launch impulse visible.
        val kick = if (clientShotStartedAt >= 0 && shotAge in 0L..3L) launchSpeed * (1.0 - shotAge / 4.0) else 0.0
        val payoutSpeed = if (projectileInFlight) projectileSpeed else kick
        val facingSign = blockState.getValue(BlockStateProperties.FACING).axisDirection.step
        cogAngle += if (projectileInFlight || kick > 0.0) payoutSpeed / (20.0 * 9.0 / 16.0)
            else if (!isOverStressed) getSpeed().toDouble() * facingSign * 2.0 * PI / 1200.0 else 0.0
        val turns = floor(cogAngle / (2.0 * PI)) * 2.0 * PI
        cogAngle -= turns
        previousCogAngle -= turns
    }

    val hasDeployedHarpoon: Boolean get() = ropeId != null || projectileInFlight

    fun canLoad(stack: ItemStack) = !hasDeployedHarpoon && loadedHarpoon.isEmpty &&
        !stack.isEmpty && stack.`is`(ClockworkItems.HARPOON.get())

    /** Flight is session-local; sync it to goggles and interaction prediction, never save it to disk. */
    fun setProjectileInFlight(inFlight: Boolean) {
        if (projectileInFlight == inFlight) return
        projectileInFlight = inFlight
        if (!inFlight) projectileSpeed = 0.0
        sendData()
    }

    fun load(stack: ItemStack): Boolean {
        if (!canLoad(stack)) return false
        loadedHarpoon = stack.copyWithCount(1)
        charge = 0.0
        setChanged()
        sendData()
        return true
    }

    fun unload(): ItemStack {
        val result = loadedHarpoon
        loadedHarpoon = ItemStack.EMPTY
        charge = 0.0
        setChanged()
        sendData()
        return result
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) {
            tickAnimation()
            return
        }
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
            // Upgrade ropes fired before harpoons acquired their own, non-adjustable rope kind.
            if (!rope.harpoon) {
                rope.harpoon = true
                links.setDirty()
                WanderwandServer.syncLinks(world)
            }
            // Release first on a falling edge; an attached rope uses shaft power to reel instead of charge.
            redstoneChanged()
            if (ropeId == rope.id && !rope.suspended) {
                val length = RopeWinchBlockEntity.nextLength(rope.length, -abs(rpm), HarpoonProjectiles.maxRange.toInt())
                if (length != rope.length) links.resizeRope(world, rope, length)
            }
            if (ropeLength != rope.length) {
                ropeLength = rope.length
                tooltipSyncPending = true
            }
            syncTooltip(world)
            return
        }
        val next = if (loadedHarpoon.isEmpty || hasDeployedHarpoon) 0.0 else nextCharge(charge, rpm)
        if (next != charge) {
            charge = next
            setChanged()
            tooltipSyncPending = true
        }
        syncTooltip(world)
        redstoneChanged()
    }

    private fun syncTooltip(world: ServerLevel) {
        // Also flush the final partial charge/length after the shaft stops between update ticks.
        if (tooltipSyncPending && (world.gameTime % 5L == 0L || charge == 1.0)) {
            tooltipSyncPending = false
            sendData()
        }
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
                sendData()
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
        ClockworkLang.translate("gui.harpoon.title").forGoggles(tooltip)
        if (hasDeployedHarpoon) {
            ClockworkLang.translate("gui.harpoon.${if (ropeId != null) "attached" else "in_flight"}")
                .style(if (ropeId != null) ChatFormatting.GREEN else ChatFormatting.GOLD).forGoggles(tooltip)
            if (ropeId != null) {
                ClockworkLang.translate("gui.harpoon.rope_length", ClockworkLang.number(ropeLength).component())
                    .style(ChatFormatting.AQUA).forGoggles(tooltip, 1)
                ClockworkLang.translate("gui.harpoon.reel").style(ChatFormatting.GRAY).forGoggles(tooltip)
            }
            ClockworkLang.translate("gui.harpoon.release").style(ChatFormatting.GRAY).forGoggles(tooltip)
        } else {
            ClockworkLang.translate("gui.harpoon.charge").style(ChatFormatting.GRAY).forGoggles(tooltip)
            val progress = sanitizeCharge(charge)
            val color = when {
                loadedHarpoon.isEmpty || firedThisActivation || progress < MIN_CHARGE -> ChatFormatting.DARK_GRAY
                progress < 0.5 -> ChatFormatting.GOLD
                progress < 1.0 -> ChatFormatting.GREEN
                else -> ChatFormatting.AQUA
            }
            ClockworkLang.text(TooltipHelper.makeProgressBar(10, (progress * 10).toInt()))
                .style(color).forGoggles(tooltip, 1)
            ClockworkLang.translate("gui.harpoon.${if (loadedHarpoon.isEmpty) "load" else "loaded"}")
                .style(if (loadedHarpoon.isEmpty) ChatFormatting.GRAY else ChatFormatting.GREEN)
                .forGoggles(tooltip)
        }

        val kineticTooltip = mutableListOf<Component>()
        if (super.addToGoggleTooltip(kineticTooltip, isPlayerSneaking)) {
            tooltip.add(CommonComponents.EMPTY)
            tooltip.addAll(kineticTooltip)
        }
        return true
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.put("Harpoon", loadedHarpoon.save(CompoundTag()))
        tag.putDouble("Charge", charge)
        tag.putBoolean("Fired", firedThisActivation)
        ropeId?.let { tag.putUUID("Rope", it) }
        if (clientPacket) {
            tag.putBoolean("InFlight", projectileInFlight)
            tag.putLong("ShotStartedAt", shotStartedAt)
            tag.putDouble("ProjectileSpeed", projectileSpeed)
            tag.putDouble("LaunchSpeed", launchSpeed)
            val currentLength = (level as? ServerLevel)?.let { WandLinks.get(it).links[ropeId]?.length }
            tag.putDouble("RopeLength", currentLength ?: 0.0)
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        loadedHarpoon = ItemStack.of(tag.getCompound("Harpoon")).let {
            if (it.`is`(ClockworkItems.HARPOON.get())) it.copyWithCount(1) else ItemStack.EMPTY
        }
        charge = if (loadedHarpoon.isEmpty) 0.0 else sanitizeCharge(tag.getDouble("Charge"))
        firedThisActivation = tag.getBoolean("Fired")
        ropeId = if (tag.hasUUID("Rope")) tag.getUUID("Rope") else null
        if (clientPacket) {
            projectileInFlight = tag.getBoolean("InFlight")
            val receivedShot = if (tag.contains("ShotStartedAt")) tag.getLong("ShotStartedAt") else -1L
            val now = level?.gameTime ?: receivedShot
            if (receivedShot >= 0 && receivedShot != shotStartedAt && abs(now - receivedShot) <= 20L)
                clientShotStartedAt = now
            shotStartedAt = receivedShot
            launchSpeed = tag.getDouble("LaunchSpeed").let { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 }
            projectileSpeed = tag.getDouble("ProjectileSpeed").let { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 }
            ropeLength = tag.getDouble("RopeLength").let { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 }
        }
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
