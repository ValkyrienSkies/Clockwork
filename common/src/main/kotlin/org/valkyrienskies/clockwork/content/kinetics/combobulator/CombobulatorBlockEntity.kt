package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.Create
import com.simibubi.create.content.redstone.link.IRedstoneLinkable
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.infrastructure.config.AllConfigs
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import net.minecraft.network.chat.Component
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.mod.common.toWorldCoordinates
import kotlin.math.roundToInt

class CombobulatorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), IThinkable, IHaveGoggleInformation {

    var program = CombobulatorProgram.controller()
        private set
    var revision = 0
        private set
    var activeStep = -1
        private set
    var fault: CombobulatorFault? = null
        private set
    val timedOut get() = fault == CombobulatorFault.TIMEOUT

    private var runtime = CombobulatorRuntime()
    private var operationRuntime = CombobulatorOperationRuntime()
    private var signals = DoubleArray(CombobulatorProgram.MAX_CHANNELS)
    private var previousSignals = signals.copyOf()
    private var evaluatedAt = Long.MIN_VALUE
    private val faceSignals = IntArray(Direction.entries.size)
    private val links = mutableMapOf<Int, ChannelLink>()
    private val cables = mutableMapOf<CombobulatorFace, WandAnchor>()
    private var linksReady = false
    private var syncPending = false
    private val network get() = Create.REDSTONE_LINK_NETWORK_HANDLER

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit

    override fun tick() {
        super.tick()
        val level = level as? ServerLevel ?: return
        if (!linksReady) registerLinks(level)
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        for ((face, source) in cables.toMap()) {
            if (!level.hasChunkAt(source.pos) || source.world(level) == null) continue
            if (thinkableLink(face.direction(front)) != null || connectThinkable(face.direction(front), source) ||
                level.getBlockState(source.pos).isAir) {
                cables.remove(face)
                setChanged()
            }
        }
        val next = DoubleArray(CombobulatorProgram.MAX_CHANNELS)
        for (channel in program.channels) {
            if (!channel.input) continue
            val face = channel.face?.direction(front)
            next[channel.id] = when {
                channel.constant != null -> channel.constant
                channel.thinkable -> channel.face?.let { sampleThinkable(level, it, front) } ?: Double.NaN
                channel.wireless -> links[channel.id]?.sample(level)?.toDouble() ?: 0.0
                channel.device && face != null -> {
                    val anchor = WandAnchor.at(level, worldPosition.relative(face), face.opposite)
                    if (thinkableLink(face) != null) sampleThinkable(level, channel.face!!, front)
                    else ThinkableItem.source(level, anchor) ?: Double.NaN
                }
                face != null -> level.getSignal(worldPosition.relative(face), face).toDouble()
                else -> if (program.operation != null) Double.NaN else 0.0
            }
        }
        val operation = program.operation
        if (operation != null) {
            operationRuntime.tick(operation, next)
            setChanged()
        } else runtime.tick(program, next, level.gameTime)
        for (channel in program.channels) {
            if (!channel.input && channel.bound) {
                val value = if (operation != null) operationRuntime.output else runtime.output(channel.id, level.gameTime)
                next[channel.id] = if (channel.thinkable || channel.device) value else redstone(value).toDouble()
            }
        }
        val previous = signals
        previousSignals = previous
        evaluatedAt = level.gameTime
        signals = next
        for (channel in program.channels.filter { !it.input && it.device }) {
            val side = channel.face?.direction(front) ?: continue
            val receiver = level.getBlockEntity(worldPosition.relative(side)) as? IThinkable ?: continue
            if (receiver.acceptsThinkable(side.opposite)) receiver.receiveThinkable(side.opposite,
                previousSignals[channel.id].takeIf { it.isFinite() })
        }
        for (link in links.values) {
            if (!link.isListening && next[link.id] != previous[link.id]) network.updateNetworkOf(level, link)
        }
        refreshFaces(level, front)
        val nextStep = if (operation != null) -1 else runtime.activeStep
        val nextFault = if (operation != null) operationRuntime.fault else runtime.fault
        syncPending = syncPending || activeStep != nextStep || fault != nextFault ||
            !previous.contentEquals(next)
        activeStep = nextStep
        fault = nextFault
        if (syncPending && level.gameTime % 4 == 0L) {
            syncPending = false
            sendData()
        }
    }

    fun signal(id: Int): Int = redstone(value(id))

    fun value(id: Int): Double = signals.getOrElse(id) { Double.NaN }

    private fun redstone(value: Double): Int =
        if (value.isFinite()) value.coerceIn(0.0, 15.0).roundToInt() else 0

    fun facePower(side: Direction, front: Direction): Int {
        val channel = program.channels.firstOrNull {
            !it.input && !it.wireless && !it.thinkable && !it.device && it.face?.direction(front) == side
        }
        return channel?.let { signal(it.id) } ?: 0
    }

    override fun thinkableValue(side: Direction): Double? {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val channel = program.channels.firstOrNull { !it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }
        return channel?.let {
            val snapshot = if (program.operation != null && evaluatedAt == level?.gameTime) previousSignals else signals
            snapshot[it.id].takeIf { value -> value.isFinite() }
        }
    }

    override fun providesThinkable(side: Direction): Boolean {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return program.channels.any { !it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }
    }

    override fun onThinkablePulse(side: Direction) {
        val level = level as? ServerLevel ?: return
        WandAnchor.at(level, worldPosition, side).world(level)?.let {
            ClockworkSounds.COMBOBULATOR_DATA.play(level, null, it, 0.45f, 1f)
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        val operation = program.operation ?: return false
        tooltip.add(Component.translatable("gui.vs_clockwork.combobulator.function.${operation.function.name.lowercase()}"))
        tooltip.add(Component.literal("Output: ${value(3)}"))
        fault?.let { tooltip.add(Component.translatable("gui.vs_clockwork.combobulator.fault.${it.name.lowercase()}")) }
        return true
    }

    fun connectThinkable(side: Direction, source: WandAnchor): Boolean {
        val level = level as? ServerLevel ?: return false
        return ThinkableConnections.connect(level, source, WandAnchor.at(level, worldPosition, side))
    }

    fun disconnectThinkable(side: Direction): Boolean {
        val level = level as? ServerLevel ?: return false
        return ThinkableConnections.disconnect(level, WandAnchor.at(level, worldPosition, side))
    }

    override fun acceptsThinkable(side: Direction): Boolean = inputFace(side) != null

    private fun thinkableLink(side: Direction): WandLinks.Link? {
        val level = level as? ServerLevel ?: return null
        val sink = WandAnchor.at(level, worldPosition, side)
        return WandLinks.get(level).links.values.firstOrNull { it.thinkable && it.b == sink }
    }

    private fun inputFace(side: Direction): CombobulatorFace? {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return program.channels.firstOrNull { it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }?.face
    }

    private fun sampleThinkable(level: ServerLevel, face: CombobulatorFace, front: Direction): Double {
        val link = thinkableLink(face.direction(front)) ?: return Double.NaN
        if (link.suspended) return Double.NaN
        return ThinkableItem.source(level, link.a) ?: Double.NaN
    }

    fun updateProgram(newProgram: CombobulatorProgram, expectedRevision: Int) {
        val level = level ?: return
        if (level.isClientSide || revision != expectedRevision) return
        if (newProgram.write() == program.write()) return
        clearDeviceOutputs()
        unregisterLinks(level)
        program = newProgram
        cables.keys.retainAll(program.channels.filter { it.thinkable && it.input }.mapNotNull { it.face }.toSet())
        revision++
        runtime = CombobulatorRuntime()
        operationRuntime = CombobulatorOperationRuntime()
        activeStep = -1
        fault = null
        signals.fill(0.0)
        previousSignals.fill(0.0)
        evaluatedAt = Long.MIN_VALUE
        registerLinks(level)
        refreshFaces(level, blockState.getValue(HorizontalDirectionalBlock.FACING))
        notifyUpdate()
    }

    private fun refreshFaces(level: Level, front: Direction) {
        val changed = Direction.entries.filter { face ->
            val signal = facePower(face, front)
            val changed = signal != faceSignals[face.ordinal]
            faceSignals[face.ordinal] = signal
            changed
        }
        if (changed.isEmpty()) return
        val block = blockState.block
        level.updateNeighborsAt(worldPosition, block)
        for (face in changed) level.updateNeighborsAt(worldPosition.relative(face), block)
    }

    private fun registerLinks(level: Level) {
        linksReady = true
        for (channel in program.channels) {
            if (!channel.wireless || !channel.bound) continue
            val link = ChannelLink(channel)
            links[channel.id] = link
            if (!channel.input) network.addToNetwork(level, link)
        }
    }

    private fun unregisterLinks(level: Level) {
        for (link in links.values) {
            if (!link.isListening) network.removeFromNetwork(level, link)
        }
        links.clear()
        linksReady = false
    }

    override fun invalidate() {
        clearDeviceOutputs()
        level?.takeUnless { it.isClientSide }?.let(::unregisterLinks)
        super.invalidate()
    }

    private fun clearDeviceOutputs() {
        val level = level as? ServerLevel ?: return
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        for (channel in program.channels.filter { !it.input && it.device }) {
            val side = channel.face?.direction(front) ?: continue
            if (level.hasChunkAt(worldPosition.relative(side)))
                (level.getBlockEntity(worldPosition.relative(side)) as? IThinkable)?.receiveThinkable(side.opposite, null)
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.put("Program", program.write())
        tag.putInt("Revision", revision)
        if (!clientPacket && program.operation != null) tag.put("OperationState", operationRuntime.write())
        tag.put("Thinkables", CompoundTag().apply {
            cables.forEach { (face, anchor) -> put(face.name, anchor.save()) }
        })
        if (clientPacket) {
            tag.putLongArray("Values", LongArray(signals.size) { signals[it].toRawBits() })
            tag.putInt("ActiveStep", activeStep)
            fault?.let { tag.putString("Fault", it.name) }
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("Program")) program = CombobulatorProgram.read(tag.getCompound("Program"))
        revision = tag.getInt("Revision")
        if (!clientPacket && tag.contains("OperationState")) {
            operationRuntime.read(tag.getCompound("OperationState"))
            val output = program.channels.firstOrNull { it.id == 3 && !it.input }
            signals[3] = if (output?.device == true || output?.thinkable == true) operationRuntime.output
                else redstone(operationRuntime.output).toDouble()
            previousSignals = signals.copyOf()
        }
        cables.clear()
        val savedCables = tag.getCompound("Thinkables")
        for (face in CombobulatorFace.entries) {
            if (savedCables.contains(face.name)) cables[face] = WandAnchor.load(savedCables.getCompound(face.name))
        }
        if (clientPacket) {
            val values = tag.getLongArray("Values")
            signals = DoubleArray(CombobulatorProgram.MAX_CHANNELS) { Double.fromBits(values.getOrElse(it) { 0L }) }
            activeStep = if (tag.contains("ActiveStep")) tag.getInt("ActiveStep") else -1
            fault = CombobulatorFault.entries.firstOrNull { it.name == tag.getString("Fault") }
        }
    }

    private inner class ChannelLink(channel: CombobulatorChannel) : IRedstoneLinkable {
        val id = channel.id
        private val listening = channel.input
        private val key = Couple.create(Frequency.of(channel.first), Frequency.of(channel.second))

        override fun isListening(): Boolean = listening
        override fun getTransmittedStrength(): Int = if (listening) 0 else signal(id)
        override fun setReceivedStrength(power: Int) = Unit
        override fun getNetworkKey(): Couple<Frequency> = key
        override fun getLocation(): BlockPos = worldPosition

        override fun isAlive(): Boolean {
            val level = level ?: return false
            return !isRemoved && !isChunkUnloaded && level.isLoaded(worldPosition) &&
                level.getBlockEntity(worldPosition) === this@CombobulatorBlockEntity
        }

        fun sample(level: Level): Int {
            val actors = network.networksIn(level)[key] ?: return 0
            val origin = level.toWorldCoordinates(Vec3.atCenterOf(worldPosition))
            val range = AllConfigs.server().logistics.linkRange.get().toDouble()
            return actors.asSequence().filter { actor ->
                !actor.isListening && actor.isAlive &&
                    origin.closerThan(level.toWorldCoordinates(Vec3.atCenterOf(actor.location)), range)
            }.maxOfOrNull { it.transmittedStrength }?.coerceIn(0, 15) ?: 0
        }
    }
}
