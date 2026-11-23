package org.valkyrienskies.clockwork.content.physicalities.steering

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelHubBlock
import org.valkyrienskies.mod.common.getShipManagingPos

internal class DrivingControlLinks : SavedData() {
    private val controllers = mutableMapOf<BlockPos, BlockPos>()
    private val brakeTargets = mutableMapOf<BlockPos, BlockPos>()
    private val tables = listOf(controllers, brakeTargets)
    private val parked = mutableSetOf<BlockPos>()
    private val removed = mutableSetOf<BlockPos>()

    fun bind(target: BlockPos, control: BlockPos, brake: Boolean = false) {
        for (pos in listOf(target, control)) if (removed.remove(pos)) {
            if (parked.remove(pos)) setDirty()
            for (table in tables) if (table.entries.removeIf { it.key == pos || it.value == pos }) setDirty()
        }
        val table = if (brake) brakeTargets else controllers
        if (table.put(target.immutable(), control.immutable()) != control) setDirty()
    }

    fun throttle(level: ServerLevel, hub: BlockPos): Float? {
        val pos = controllers[hub] ?: return null
        if (!level.hasChunkAt(pos)) return 0f
        return (level.getBlockEntity(pos) as? SteeringWheelBlockEntity)?.driveThrottle ?: 0f
    }

    fun controller(level: ServerLevel, target: BlockPos): SteeringWheelBlockEntity? {
        val pos = controllers[target] ?: return null
        return if (level.hasChunkAt(pos)) level.getBlockEntity(pos) as? SteeringWheelBlockEntity else null
    }

    fun braked(level: ServerLevel, target: BlockPos): Boolean {
        val pos = brakeTargets[target] ?: return false
        if (!level.hasChunkAt(pos)) return pos in parked
        val state = level.getBlockState(pos)
        return state.block is HandbrakeBlock && state.getValue(HandbrakeBlock.PULLED)
    }

    fun setParked(pos: BlockPos, latched: Boolean) {
        val changed = if (latched) parked.add(pos) else parked.remove(pos)
        if (changed) setDirty()
    }

    fun engageTargets(level: ServerLevel, brake: BlockPos) {
        for ((target, controller) in brakeTargets) if (controller == brake && level.hasChunkAt(target)) {
            if (level.getBlockState(target).block is HingeBlock) HingeLinks.get(level).engageBrake(level, target)
            HandbrakeEffectPacket.send(level, target)
        }
    }

    fun endpointRemoved(pos: BlockPos) {
        if (pos in parked || tables.any { it.isNotEmpty() }) removed.add(pos.immutable())
    }

    fun finishRemovals() {
        if (removed.isEmpty()) return
        if (parked.removeAll(removed)) setDirty()
        for (table in tables) if (table.entries.removeIf { it.key in removed || it.value in removed }) setDirty()
        removed.clear()
    }

    fun remap(destination: (BlockPos) -> BlockPos?) {
        val movedBrakes = parked.mapNotNull { from -> destination(from)?.let { from to it } }
        if (movedBrakes.isNotEmpty()) {
            parked.removeAll(movedBrakes.map { it.first }.toSet())
            parked.addAll(movedBrakes.map { it.second })
            setDirty()
        }
        tables.forEach { remap(it, destination) }
    }

    private fun remap(table: MutableMap<BlockPos, BlockPos>, destination: (BlockPos) -> BlockPos?) {
        val moved = table.mapNotNull { (hub, wheel) ->
            val newHub = destination(hub)
            val newWheel = destination(wheel)
            if (newHub == null && newWheel == null) null else Triple(hub, newHub ?: hub, newWheel ?: wheel)
        }
        if (moved.isEmpty()) return
        moved.forEach { (old, _, _) -> table.remove(old) }
        moved.forEach { (_, hub, wheel) -> table[hub] = wheel }
        setDirty()
    }

    fun capture(level: ServerLevel, origins: Map<Long, BlockPos>) = ListTag().also { list ->
        for (table in tables) for ((hub, wheel) in table) {
            val hubShip = level.getShipManagingPos(hub)?.id ?: continue
            val wheelShip = level.getShipManagingPos(wheel)?.id ?: continue
            val hubOrigin = origins[hubShip] ?: continue
            val wheelOrigin = origins[wheelShip] ?: continue
            list.add(CompoundTag().apply {
                if (table === brakeTargets) putBoolean("Brake", true)
                putLong("HubShip", hubShip)
                putLong("WheelShip", wheelShip)
                putLong("Hub", hub.subtract(hubOrigin).asLong())
                putLong("Wheel", wheel.subtract(wheelOrigin).asLong())
            })
        }
    }

    fun paste(level: ServerLevel, links: ListTag, destinations: Map<Long, Pair<Long, BlockPos>>) {
        for (entry in links.filterIsInstance<CompoundTag>()) {
            val (hubShip, hubOrigin) = destinations[entry.getLong("HubShip")] ?: continue
            val (wheelShip, wheelOrigin) = destinations[entry.getLong("WheelShip")] ?: continue
            val hub = hubOrigin.offset(BlockPos.of(entry.getLong("Hub")))
            val wheel = wheelOrigin.offset(BlockPos.of(entry.getLong("Wheel")))
            if (level.getShipManagingPos(hub)?.id != hubShip || level.getShipManagingPos(wheel)?.id != wheelShip ||
                !level.hasChunkAt(hub) || !level.hasChunkAt(wheel)) continue
            val target = level.getBlockState(hub).block
            val source = level.getBlockState(wheel).block
            val brake = entry.getBoolean("Brake")
            val valid = if (brake) source is HandbrakeBlock && DrivingControlItem.brakeTarget(target)
                else source is SteeringWheelBlock && (target is WheelHubBlock || target is HandbrakeBlock)
            if (valid) bind(hub, wheel, brake)
        }
    }

    override fun save(tag: CompoundTag) = tag.apply {
        putLongArray("Parked", parked.map(BlockPos::asLong).toLongArray())
        put("Links", ListTag().also { list -> for (table in tables) table.forEach { (hub, wheel) ->
            list.add(CompoundTag().apply {
                putLong("Hub", hub.asLong()); putLong("Wheel", wheel.asLong())
                if (table === brakeTargets) putBoolean("Brake", true)
            })
        } })
    }

    companion object {
        fun get(level: ServerLevel): DrivingControlLinks = level.dataStorage.computeIfAbsent({ tag ->
            DrivingControlLinks().apply {
                tag.getLongArray("Parked").mapTo(parked, BlockPos::of)
                for (entry in tag.getList("Links", 10).filterIsInstance<CompoundTag>()) {
                    val table = if (entry.getBoolean("Brake")) brakeTargets else controllers
                    table[BlockPos.of(entry.getLong("Hub"))] = BlockPos.of(entry.getLong("Wheel"))
                }
            }
        }, ::DrivingControlLinks, "clockwork_steering_hubs")
    }
}
