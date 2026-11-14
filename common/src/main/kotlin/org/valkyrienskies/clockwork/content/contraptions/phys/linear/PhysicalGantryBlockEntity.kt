package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.util.GlueAssembler
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.Locale
import kotlin.math.roundToInt

class PhysicalGantryBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    PhysicalLinearBlockEntity(type, pos, state) {

    private lateinit var movementMode: ScrollOptionBehaviour<MovementMode>
    private var railOrigin: BlockPos? = null
    private var railDirection = Direction.NORTH
    private var railShipId: Long? = null
    private var railRange: IntRange? = null
    private var railScanTick = 0L
    override val isAssembled: Boolean get() = railOrigin != null

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            MovementMode::class.java, Component.translatable("$MOD_ID.phys_gantry.movement_mode"), this,
            DirectionalExtenderScrollOptionSlot { state, side ->
                val block = state.block as PhysicalGantryBlock
                side.axis != state.getValue(BlockStateProperties.FACING).axis && side.axis != block.getRotationAxis(state)
            }
        )
        movementMode.requiresWrench()
        behaviours.add(movementMode)
    }

    override fun tick() {
        super.tick()
        val level = level as? ServerLevel ?: return
        val origin = railOrigin
        if (origin == null) {
            val shaft = level.getBlockEntity(worldPosition.relative(facing.opposite)) as? GantryShaftBlockEntity
            if (shaft == null || shaft.pinionMovementSpeed == 0f) {
                if (waitForStop) {
                    waitForStop = false
                    setChanged()
                }
            } else if (!waitForStop && shaft.canAssembleOn()) {
                tryAssemble(level)
            }
            return
        }
        val parent = level.getLoadedShipManagingPos(origin)
        val child = level.getLoadedShipManagingPos(worldPosition) ?: return
        if (railShipId != null && parent?.id != railShipId || parent?.id == child.id) {
            pauseDrive()
            return
        }
        val anchor = origin.relative(facing)
        val offset = displacement(parent, child, anchor, worldPosition, railDirection).roundToInt()
        val nearest = railRange?.let { offset.coerceIn(it) } ?: offset
        val railPos = origin.relative(railDirection, nearest)
        if (!level.isLoaded(railPos)) {
            pauseDrive()
            return
        }
        if (railAt(level, railPos) == null) {
            disconnect(level)
            return
        }
        if (railRange == null || level.gameTime >= railScanTick) {
            railRange = scanRail(level, origin, nearest)
            railScanTick = level.gameTime + 5
        }
        val range = railRange ?: return
        val shaft = level.getBlockEntity(railPos) as? GantryShaftBlockEntity
        drive(
            parent, child, anchor, worldPosition, railDirection, range.first, range.last,
            shaft?.pinionMovementSpeed?.toDouble() ?: 0.0, movementMode.get() == MovementMode.LOCKED
        )
    }

    override fun assemble(level: ServerLevel) {
        val origin = worldPosition.relative(facing.opposite)
        val shaft = level.getBlockState(origin)
        if (!AllBlocks.GANTRY_SHAFT.has(shaft)) return
        val direction = shaft.getValue(GantryShaftBlock.FACING)
        val carriageBlock = blockState.block as PhysicalGantryBlock
        if (direction.axis == facing.axis || carriageBlock.getRotationAxis(blockState) == direction.axis) return
        val parentId = level.getShipManagingPos(origin)?.id
        val blocks = GlueAssembler.collectGlued(level, worldPosition) { pos, state ->
            !AllBlocks.GANTRY_SHAFT.has(state) && canAssembleLinearBlock(level, pos, state)
        }?.mapTo(mutableSetOf()) { it.toMinecraft() } ?: return
        val assembly = assembleLinearShip(level, blocks)
        val carriage = assembly.migrate(worldPosition)
        val moved = level.getBlockEntity(carriage) as? PhysicalGantryBlockEntity ?: return
        moved.railOrigin = origin
        moved.railDirection = direction
        moved.railShipId = parentId
        level.setBlockAndUpdate(carriage, moved.blockState.setValue(PhysicalGantryBlock.ASSEMBLED, true))
        moved.sendData()
        playAssemblySound(level, origin, true)
    }

    override fun disassemble(level: ServerLevel) {
        val origin = railOrigin ?: return
        val child = level.getLoadedShipManagingPos(worldPosition) ?: return
        val parent = level.getLoadedShipManagingPos(origin)
        if (parent?.id != railShipId) return
        val offset = displacement(parent, child, origin.relative(facing), worldPosition, railDirection).roundToInt()
        val trackOffset = railRange?.let { offset.coerceIn(it) } ?: offset
        val rail = origin.relative(railDirection, trackOffset)
        if (railAt(level, rail) == null) return
        val destination = rail.relative(facing)
        val blocks = shipBlocks(level, child)
        if (worldPosition !in blocks || !moveOffShip(level, blocks, worldPosition, destination, child)) return
        (level.getBlockEntity(destination) as? PhysicalGantryBlockEntity)?.let {
            it.waitForStop = true
            it.disconnect(level)
        }
        stopDrive()
        playAssemblySound(level, destination, false)
    }

    private fun disconnect(level: ServerLevel) {
        stopDrive()
        railOrigin = null
        railShipId = null
        railRange = null
        level.setBlockAndUpdate(worldPosition, blockState.setValue(PhysicalGantryBlock.ASSEMBLED, false))
        sendData()
    }

    private fun railAt(level: ServerLevel, pos: BlockPos): BlockState? {
        if (!level.isLoaded(pos) || level.getShipManagingPos(pos)?.id != railShipId) return null
        val state = level.getBlockState(pos)
        return state.takeIf { AllBlocks.GANTRY_SHAFT.has(it) && it.getValue(GantryShaftBlock.FACING) == railDirection }
    }

    private fun scanRail(level: ServerLevel, origin: BlockPos, from: Int): IntRange {
        var min = from
        var max = from
        while (railAt(level, origin.relative(railDirection, min - 1)) != null) min--
        while (railAt(level, origin.relative(railDirection, max + 1)) != null) max++
        return min..max
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        railOrigin?.let { tag.putLong("RailOrigin", it.asLong()) }
        tag.putInt("RailDirection", railDirection.get3DDataValue())
        railShipId?.let { tag.putLong("RailShip", it) }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        railOrigin = BlockPos.of(tag.getLong("RailOrigin")).takeIf { tag.contains("RailOrigin") }
        railDirection = Direction.from3DDataValue(tag.getInt("RailDirection"))
        railShipId = tag.getLong("RailShip").takeIf { tag.contains("RailShip") }
    }

    private enum class MovementMode(private val icon: AllIcons) : INamedIconOptions {
        LOCKED(AllIcons.I_MOVE_NEVER_PLACE),
        FREE(AllIcons.I_MOVE_PLACE);

        override fun getIcon() = icon
        override fun getTranslationKey() = "$MOD_ID.phys_gantry.movement_mode.${name.lowercase(Locale.ROOT)}"
    }
}
