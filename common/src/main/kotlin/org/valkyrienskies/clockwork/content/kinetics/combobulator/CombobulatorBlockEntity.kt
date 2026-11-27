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
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
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
    internal val isSequentialNode: Boolean
        get() = program.operation?.function?.sequential ?: true
    internal val hasWirelessInput: Boolean
        get() = program.channels.any { it.input && it.wireless && it.constant == null && it.bound }
    internal val needsPreparation: Boolean
        get() = !linksReady || cables.isNotEmpty() ||
            blockState.getValue(CombobulatorBlock.FUNCTION_GROUP) != (program.operation?.function?.group ?: CombobulatorGroup.DEFAULT)
    private val faceSignals = IntArray(Direction.entries.size)
    private val links = mutableMapOf<Int, ChannelLink>()
    private val cables = mutableMapOf<CombobulatorFace, WandAnchor>()
    private var linksReady = false
    private var syncPending = false
    var availableReadings: Map<Direction, List<ThinkableReading>> = emptyMap()
        private set
    var cabledReadingFaces: Set<Direction> = emptySet()
        private set
    private val network get() = Create.REDSTONE_LINK_NETWORK_HANDLER

    init {
        setLazyTickRate(4)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit

    // Runs whenever this block entity is placed or loaded into a level, before its first tick.
    override fun setLevel(level: Level) {
        super.setLevel(level)
        if (level is ServerLevel) CombobulatorNetwork.register(level, this)
    }

    override fun lazyTick() {
        super.lazyTick()
        val level = level as? ServerLevel ?: return
        refreshReadings(level)
        if (syncPending) {
            syncPending = false
            sendData()
        }
    }

    internal fun ensurePrepared(level: ServerLevel) {
        refreshFunctionGroup(level)
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
    }

    internal fun wirelessUpstreamCombobulators(level: ServerLevel): List<CombobulatorBlockEntity> {
        val result = mutableListOf<CombobulatorBlockEntity>()
        for (channel in program.channels) {
            if (channel.input && channel.wireless && channel.constant == null) {
                links[channel.id]?.collectUpstreamCombobulators(level, result)
            }
        }
        return result
    }

    internal fun upstreamCombobulators(level: ServerLevel): List<CombobulatorBlockEntity> {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val result = mutableListOf<CombobulatorBlockEntity>()
        for (channel in program.channels) {
            if (!channel.input || channel.constant != null) continue
            if (channel.wireless) {
                links[channel.id]?.collectUpstreamCombobulators(level, result)
                continue
            }
            val face = channel.face?.direction(front) ?: continue
            if (channel.thinkable || channel.device) {
                val source = ThinkableConnections.sourceAt(level, worldPosition, face, channel.thinkable)
                val resolved = when (val device = source?.device) {
                    is ThinkableRelayBlockEntity -> device.resolveSource(source.side)?.device
                    else -> device
                }
                if (resolved is CombobulatorBlockEntity) result += resolved
            } else {
                val neighborPos = worldPosition.relative(face)
                if (level.hasChunkAt(neighborPos)) {
                    val neighbor = level.getBlockEntity(neighborPos) as? CombobulatorBlockEntity
                    if (neighbor != null && !neighbor.isRemoved && neighbor.emitsRedstoneOn(face.opposite)) {
                        result += neighbor
                    }
                }
            }
        }
        return result
    }

    internal fun sampleInputs(level: ServerLevel): DoubleArray {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val next = DoubleArray(CombobulatorProgram.MAX_CHANNELS)
        for (channel in program.channels) {
            if (!channel.input) continue
            val face = channel.face?.direction(front)
            next[channel.id] = when {
                channel.constant != null -> channel.constant
                channel.wireless -> links[channel.id]?.sample(level)?.toDouble() ?: 0.0
                (channel.thinkable || channel.device) && face != null -> {
                    val source = ThinkableConnections.sourceAt(level, worldPosition, face, channel.thinkable)
                    source?.device?.thinkableValue(source.side, channel.reading)?.takeIf { it.isFinite() } ?: Double.NaN
                }
                face != null -> level.getSignal(worldPosition.relative(face), face).toDouble()
                else -> if (program.operation != null) Double.NaN else 0.0
            }
        }
        return next
    }

    internal fun commitEvaluation(level: ServerLevel, next: DoubleArray) {
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
        val nextStep = if (operation != null) -1 else runtime.activeStep
        val nextFault = if (operation != null) operationRuntime.fault else runtime.fault
        syncPending = syncPending || activeStep != nextStep || fault != nextFault ||
            !previous.contentEquals(next)
        activeStep = nextStep
        fault = nextFault
    }

    internal fun publishOutputs(level: ServerLevel) {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        for (channel in program.channels.filter { !it.input && it.device }) {
            val side = channel.face?.direction(front) ?: continue
            ThinkableConnections.sendAdjacent(level, worldPosition, side, signals[channel.id].takeIf { it.isFinite() })
        }
        for (link in links.values) {
            if (!link.isListening && signals[link.id] != previousSignals[link.id]) network.updateNetworkOf(level, link)
        }
        refreshFaces(level, front)
    }

    private fun emitsRedstoneOn(side: Direction): Boolean {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return program.channels.any {
            !it.input && !it.wireless && !it.thinkable && !it.device && it.face?.direction(front) == side
        }
    }

    private fun readChannelOutput(channel: CombobulatorChannel): Double {
        val serverLevel = level as? ServerLevel
        if (serverLevel != null && CombobulatorNetwork.usesPreTickRead(serverLevel, this)) {
            val operation = program.operation
            if (operation?.function == CombobulatorFunction.DELAY && channel.id == 3) {
                val value = operationRuntime.peekDelayOutput(operation)
                return if (channel.thinkable || channel.device) value else redstone(value).toDouble()
            }
            val snapshot = if (evaluatedAt == serverLevel.gameTime) previousSignals else signals
            return snapshot.getOrElse(channel.id) { Double.NaN }
        }
        return signals.getOrElse(channel.id) { Double.NaN }
    }

    fun signal(id: Int): Int = redstone(value(id))

    fun value(id: Int): Double = signals.getOrElse(id) { Double.NaN }

    private fun redstone(value: Double): Int =
        if (value.isFinite()) value.coerceIn(0.0, 15.0).roundToInt() else 0

    fun facePower(side: Direction, front: Direction): Int {
        val channel = program.channels.firstOrNull {
            !it.input && !it.wireless && !it.thinkable && !it.device && it.face?.direction(front) == side
        }
        return channel?.let { redstone(readChannelOutput(it)) } ?: 0
    }

    override fun thinkableValue(side: Direction): Double? {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val channel = program.channels.firstOrNull { !it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }
            ?: return null
        return readChannelOutput(channel).takeIf { it.isFinite() }
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

    override fun thinkableInputReading(side: Direction): String {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return program.channels.firstOrNull { it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }?.reading ?: ""
    }

    private fun refreshReadings(world: ServerLevel) {
        val cables = Direction.entries.filter { thinkableLink(it) != null }.toSet()
        val readings = Direction.entries.associateWith { side ->
            val source = ThinkableConnections.sourceAt(world, worldPosition, side)
            source?.device?.thinkableReadings(source.side)?.filter { it.id().length <= 128 }?.distinctBy { it.id() }?.take(32) ?: emptyList()
        }
        if (readings != availableReadings || cables != cabledReadingFaces) {
            availableReadings = readings
            cabledReadingFaces = cables
            syncPending = true
        }
    }

    private fun thinkableLink(side: Direction): WandLinks.Link? {
        val level = level as? ServerLevel ?: return null
        val sink = WandAnchor.at(level, worldPosition, side)
        return WandLinks.get(level).links.values.firstOrNull { it.thinkable && it.b == sink }
    }

    private fun inputFace(side: Direction): CombobulatorFace? {
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return program.channels.firstOrNull { it.input && (it.thinkable || it.device) && it.face?.direction(front) == side }?.face
    }

    fun updateProgram(newProgram: CombobulatorProgram, expectedRevision: Int) {
        val level = level ?: return
        if (level.isClientSide || revision != expectedRevision) return
        if (newProgram.write() == program.write()) return
        clearDeviceOutputs()
        unregisterLinks(level)
        program = newProgram
        refreshFunctionGroup(level)
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
        (level as? ServerLevel)?.let { CombobulatorNetwork.markDirty(it) }
        notifyUpdate()
    }

    private fun refreshFunctionGroup(world: Level) {
        val group = program.operation?.function?.group ?: CombobulatorGroup.DEFAULT
        val state = blockState
        if (state.getValue(CombobulatorBlock.FUNCTION_GROUP) != group)
            world.setBlock(worldPosition, state.setValue(CombobulatorBlock.FUNCTION_GROUP, group), net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
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
        (level as? ServerLevel)?.let {
            CombobulatorNetwork.unregister(it, this)
            unregisterLinks(it)
        }
        super.invalidate()
    }

    private fun clearDeviceOutputs() {
        val level = level as? ServerLevel ?: return
        val front = blockState.getValue(HorizontalDirectionalBlock.FACING)
        for (channel in program.channels.filter { !it.input && it.device }) {
            val side = channel.face?.direction(front) ?: continue
            ThinkableConnections.sendAdjacent(level, worldPosition, side, null, clear = true)
        }
    }

    override fun writeSafe(tag: CompoundTag) {
        super.writeSafe(tag)
        tag.put("Program", program.write())
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
            tag.put("Readings", CompoundTag().apply {
                availableReadings.forEach { (side, readings) ->
                    put(side.name, ListTag().apply {
                        readings.forEach { reading -> add(CompoundTag().apply {
                            putString("Id", reading.id())
                            putString("Name", Component.Serializer.toJson(reading.name()))
                        }) }
                    })
                    putBoolean("Cable_${side.name}", side in cabledReadingFaces)
                }
            })
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
        if (!clientPacket) {
            (level as? ServerLevel)?.let { CombobulatorNetwork.markDirty(it) }
        }
        cables.clear()
        val savedCables = tag.getCompound("Thinkables")
        for (face in CombobulatorFace.entries) {
            if (savedCables.contains(face.name)) cables[face] = WandAnchor.load(savedCables.getCompound(face.name))
        }
        if (clientPacket) {
            val readings = tag.getCompound("Readings")
            availableReadings = Direction.entries.associateWith { side ->
                val entries = readings.getList(side.name, Tag.TAG_COMPOUND.toInt())
                (0 until minOf(entries.size, 32)).map { index ->
                    val entry = entries.getCompound(index)
                    ThinkableReading(entry.getString("Id"), Component.Serializer.fromJson(entry.getString("Name")) ?: Component.empty())
                }
            }
            cabledReadingFaces = Direction.entries.filter { readings.getBoolean("Cable_${it.name}") }.toSet()
            val values = tag.getLongArray("Values")
            signals = DoubleArray(CombobulatorProgram.MAX_CHANNELS) { Double.fromBits(values.getOrElse(it) { 0L }) }
            activeStep = if (tag.contains("ActiveStep")) tag.getInt("ActiveStep") else -1
            fault = CombobulatorFault.entries.firstOrNull { it.name == tag.getString("Fault") }
        }
    }

    private inner class ChannelLink(channel: CombobulatorChannel) : IRedstoneLinkable {
        val id = channel.id
        val owner get() = this@CombobulatorBlockEntity
        private val listening = channel.input
        private val key = Couple.create(Frequency.of(channel.first), Frequency.of(channel.second))

        override fun isListening(): Boolean = listening
        override fun getTransmittedStrength(): Int {
            if (listening) return 0
            val channel = program.channels.firstOrNull { it.id == id && !it.input } ?: return 0
            return redstone(readChannelOutput(channel))
        }
        override fun setReceivedStrength(power: Int) = Unit
        override fun getNetworkKey(): Couple<Frequency> = key
        override fun getLocation(): BlockPos = worldPosition

        override fun isAlive(): Boolean {
            val level = level ?: return false
            return !isRemoved && !isChunkUnloaded && level.isLoaded(worldPosition) &&
                level.getBlockEntity(worldPosition) === this@CombobulatorBlockEntity
        }

        fun collectUpstreamCombobulators(level: ServerLevel, out: MutableList<CombobulatorBlockEntity>) {
            val actors = network.networksIn(level)[key] ?: return
            val origin = level.toWorldCoordinates(Vec3.atCenterOf(worldPosition))
            val range = AllConfigs.server().logistics.linkRange.get().toDouble()
            for (actor in actors) {
                if (actor is ChannelLink && !actor.isListening && actor.isAlive &&
                    origin.closerThan(level.toWorldCoordinates(Vec3.atCenterOf(actor.location)), range)) {
                    out += actor.owner
                }
            }
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
