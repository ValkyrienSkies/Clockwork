package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.mod.common.toWorldCoordinates
import java.util.UUID
import kotlin.math.abs

class SteeringWheelBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), IThinkable {
    lateinit var range: ScrollValueBehaviour
        private set
    lateinit var centring: ScrollOptionBehaviour<Centring>
        private set
    var driver: UUID? = null
        private set
    var angle = 0f
        private set
    private var previousAngle = 0f
    private var displayedAngle = 0f
    private var input = 0
    private var throttle = 0
    private var brakeInput = false
    private var throttleInput = 0
    val handbrakeHeld get() = driver != null && brakeInput
    val throttleAdjustment get() = if (driver != null) throttleInput else 0
    var driveThrottle = 0f
        private set
    private var lastInput = 0L

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        range = object : ScrollValueBehaviour(Component.translatable("block.vs_clockwork.steering_wheel.range"), this, SettingSlot(false)) {
            override fun getType() = RANGE
            override fun netId() = 0
            override fun getClipboardKey() = "SteeringRange"
            override fun write(tag: CompoundTag, clientPacket: Boolean) { tag.putInt(clipboardKey, value) }
            override fun read(tag: CompoundTag, clientPacket: Boolean) {
                if (tag.contains(clipboardKey)) value = tag.getInt(clipboardKey).coerceIn(5, 180)
            }
        }.between(5, 180).withFormatter { "$it°" }
        range.setValue(45)
        range.requiresWrench()
        centring = object : ScrollOptionBehaviour<Centring>(Centring::class.java,
            Component.translatable("block.vs_clockwork.steering_wheel.centring"), this, SettingSlot(true)) {
            override fun getType() = MODE
            override fun netId() = 1
            override fun getClipboardKey() = "SteeringReturn"
            override fun write(tag: CompoundTag, clientPacket: Boolean) { tag.putInt(clipboardKey, value) }
            override fun read(tag: CompoundTag, clientPacket: Boolean) {
                if (tag.contains(clipboardKey)) value = tag.getInt(clipboardKey).coerceIn(0, Centring.entries.lastIndex)
            }
        }
        centring.requiresWrench()
        behaviours.add(range)
        behaviours.add(centring)
    }

    override fun providesThinkable(side: Direction) = true
    override fun thinkableReadings(side: Direction) = listOf(
        ThinkableReading("steering_angle", Component.translatable("thinkable.vs_clockwork.port.steering_angle")),
        ThinkableReading("throttle", Component.translatable("thinkable.vs_clockwork.reading.throttle")))
    override fun thinkableValue(side: Direction) = angle.toDouble()
    override fun thinkableValue(side: Direction, reading: String): Double? = when (reading) {
        "", "steering_angle" -> angle.toDouble()
        "throttle" -> throttle.toDouble()
        else -> null
    }
    override fun thinkablePortName(side: Direction): Component = Component.translatable("thinkable.vs_clockwork.port.driving_controls")

    fun canReach(player: Player): Boolean = player.isAlive && !player.isSpectator && player.level() === level &&
        player.eyePosition.distanceToSqr(level!!.toWorldCoordinates(Vec3.atCenterOf(blockPos))) <= 36.0

    fun use(player: Player) {
        if (!canReach(player)) return
        if (driver != null && driver != player.uuid) {
            player.displayClientMessage(Component.translatable("block.vs_clockwork.steering_wheel.occupied"), true)
            return
        }
        driver = if (driver == player.uuid) null else player.uuid
        input = 0
        throttle = 0
        brakeInput = false
        throttleInput = 0
        lastInput = level!!.gameTime
        sendData()
    }

    fun control(player: Player, direction: Int, throttle: Int, braking: Boolean = false, throttleAdjustment: Int = 0) {
        if (driver != player.uuid || !canReach(player)) return
        if (direction == 2) {
            driver = null
            input = 0
            this.throttle = 0
            sendData()
        } else {
            brakeInput = braking
            throttleInput = throttleAdjustment.coerceIn(-1, 1)
            input = direction.coerceIn(-1, 1)
            val nextThrottle = throttle.coerceIn(-1, 1)
            if (this.throttle != nextThrottle) {
                this.throttle = nextThrottle
                sendData()
            }
            lastInput = level!!.gameTime
        }
    }

    override fun tick() {
        super.tick()
        val world = level ?: return
        if (world.isClientSide) {
            previousAngle = displayedAngle
            displayedAngle += (angle - displayedAngle) * .5f
            return
        }
        val player = driver?.let(world::getPlayerByUUID)
        if (driver != null && (player == null || !canReach(player) || world.gameTime - lastInput > 20)) {
            driver = null
            input = 0
            throttle = 0
            sendData()
        }
        val targetThrottle = if (driver == null) 0f else throttle.toFloat()
        driveThrottle = if (driveThrottle * targetThrottle < 0f || targetThrottle == 0f)
            Mth.approach(driveThrottle, 0f, .25f)
        else Mth.approach(driveThrottle, targetThrottle, .1f)

        val limit = range.value.toFloat()
        val target = when {
            input != 0 && driver != null -> input * limit
            centring.get() == Centring.AUTO_CENTRE -> 0f
            else -> angle.coerceIn(-limit, limit)
        }
        val delta = (target - angle).coerceIn(-4.5f, 4.5f)
        if (abs(delta) >= .001f) {
            angle += delta
            setChanged()
            sendData()
        }
    }

    fun renderedAngle(partialTicks: Float) = Mth.lerp(partialTicks, previousAngle, displayedAngle)

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putFloat("SteeringAngle", angle)
        if (clientPacket) {
            driver?.let { tag.putUUID("SteeringDriver", it) }
            tag.putInt("Throttle", throttle)
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        angle = tag.getFloat("SteeringAngle").takeIf { it.isFinite() }?.coerceIn(-180f, 180f) ?: 0f
        driver = if (clientPacket && tag.hasUUID("SteeringDriver")) tag.getUUID("SteeringDriver") else null
        throttle = if (clientPacket && driver != null) tag.getInt("Throttle").coerceIn(-1, 1) else 0
        if (!clientPacket) {
            input = 0
            driveThrottle = 0f
            previousAngle = angle
            displayedAngle = angle
        }
    }

    enum class Centring(private val icon: AllIcons) : INamedIconOptions {
        AUTO_CENTRE(AllIcons.I_CART_ROTATE), HOLD(AllIcons.I_CONFIG_LOCKED);
        override fun getIcon() = icon
        override fun getTranslationKey() = "block.vs_clockwork.steering_wheel.${name.lowercase()}"
    }

    private class SettingSlot(private val mode: Boolean) : ValueBoxTransform.Sided() {
        override fun isSideActive(state: BlockState, side: Direction) = side.axis.isHorizontal
        override fun getSouthLocation() = Vec3(if (mode) .7 else .3, .35, .7)
    }

    companion object {
        private val RANGE = BehaviourType<ScrollValueBehaviour>("steering_range")
        private val MODE = BehaviourType<ScrollValueBehaviour>("steering_return")
    }
}
