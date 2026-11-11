package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.STATE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.util.GlueAssembler
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShipFull
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import kotlin.math.roundToInt

class PhysicalPistonBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    PhysicalLinearBlockEntity(type, pos, state) {

    private var shaftOrigin: BlockPos? = null
    private var shaftShipId = -1L
    private var head = 0
    private var tail = 0
    override val isAssembled: Boolean get() = shaftOrigin != null

    private val travelSpeed: Double
        get() {
            val sign = facing.axisDirection.step * if (facing.axis == Direction.Axis.Z) 1 else -1
            return convertToLinear(getSpeed()).coerceIn(-0.49f, 0.49f).toDouble() * sign
        }

    override fun tick() {
        super.tick()
        val level = level as? ServerLevel ?: return
        val origin = shaftOrigin
        val speed = travelSpeed
        if (origin == null) {
            if (speed == 0.0) {
                if (waitForStop) {
                    waitForStop = false
                    setChanged()
                }
            } else if (!waitForStop && (speed > 0 || blockState.getValue(STATE) == PistonState.EXTENDED)) {
                waitForStop = true
                tryAssemble(level)
            }
            return
        }
        val ship = level.shipObjectWorld.loadedShips.getById(shaftShipId)
        if (ship == null) {
            pauseDrive()
            if (level.shipObjectWorld.allShips.getById(shaftShipId) == null) disconnect(level)
            return
        }
        val parent = level.getLoadedShipManagingPos(worldPosition)
        ship.getAttachment<SplittingDisablerAttachment>()?.disableSplitting()
        val offset = displacement(parent, ship, worldPosition, origin, facing)
        if (!updateShaft(level, ship, origin, offset)) {
            pauseDrive()
            return
        }
        val max = maxOf(-head, minOf(-tail, MechanicalPistonBlock.maxAllowedPistonPoles() - head))
        drive(parent, ship, worldPosition, origin, facing, -head, max, speed, !level.hasNeighborSignal(worldPosition))
    }

    override fun assemble(level: ServerLevel) {
        val direction = facing
        val limit = MechanicalPistonBlock.maxAllowedPistonPoles()
        val blocks = mutableSetOf<BlockPos>()
        var front = 0
        if (blockState.getValue(STATE) == PistonState.EXTENDED) {
            while (front < limit && isPole(level.getBlockState(worldPosition.relative(direction, front + 1)))) front++
            val headPos = worldPosition.relative(direction, front + 1)
            if (!isHead(level.getBlockState(headPos))) return
            front++
            for (index in 1..front) blocks.add(worldPosition.relative(direction, index))
        }
        var back = 0
        while (isPole(level.getBlockState(worldPosition.relative(direction.opposite, back + 1)))) {
            back++
            if (front + back > limit) throw AssemblyException.tooManyPistonPoles()
            blocks.add(worldPosition.relative(direction.opposite, back))
        }
        if (front + back == 0) throw AssemblyException.noPistonPoles()
        if (front + back > limit) throw AssemblyException.tooManyPistonPoles()

        val payload = worldPosition.relative(direction, front + 1)
        GlueAssembler.collectGlued(level, payload) { pos, state ->
            pos != worldPosition && canAssembleLinearBlock(level, pos, state)
        }?.mapTo(blocks) { it.toMinecraft() }
        if (blocks.any { !level.isLoaded(it) || level.getBlockState(it).inAssemblyBlacklist() }) return

        val assembly = assembleLinearShip(level, blocks)
        val origin = assembly.migrate(worldPosition)
        shaftOrigin = origin
        shaftShipId = assembly.ship.id
        head = front
        tail = -back
        (assembly.ship as? LoadedServerShip)?.getAttachment<SplittingDisablerAttachment>()?.disableSplitting()
        val internal = if (front == 0) ClockworkBlocks.PHYS_PISTON_HEAD.defaultState else AllBlocks.PISTON_EXTENSION_POLE.defaultState
        level.setBlockAndUpdate(worldPosition, blockState.setValue(STATE, PistonState.MOVING))
        level.setBlockAndUpdate(origin, internal.setValue(BlockStateProperties.FACING, direction))
        playAssemblySound(level, worldPosition, true)
        sendData()
    }

    fun addPole(player: Player, hand: InteractionHand): InteractionResult {
        val origin = shaftOrigin ?: return InteractionResult.PASS
        if (head - tail >= MechanicalPistonBlock.maxAllowedPistonPoles()) return InteractionResult.FAIL
        val lastPole = origin.relative(facing, tail)
        val stack = player.getItemInHand(hand)
        val item = stack.item as? BlockItem ?: return InteractionResult.PASS
        val hit = BlockHitResult(lastPole.center, facing.opposite, lastPole, false)
        return item.place(BlockPlaceContext(player, hand, stack, hit))
    }

    private fun updateShaft(level: ServerLevel, ship: ServerShip, origin: BlockPos, offset: Double): Boolean {
        val inHousing = (-offset.roundToInt()).coerceIn(tail, head)
        val housingPos = origin.relative(facing, inHousing)
        if (!level.isLoaded(housingPos)) return false
        val housingState = level.getBlockState(housingPos)
        if (!isPole(housingState) && !(inHousing == head && isHead(housingState))) {
            val oldHead = origin.relative(facing, head)
            if (!level.isLoaded(oldHead)) return false
            if (isHead(level.getBlockState(oldHead))) level.removeBlock(oldHead, false)
            disconnect(level)
            return false
        }

        var newTail = inHousing
        val maxPoles = maxOf(head - tail, MechanicalPistonBlock.maxAllowedPistonPoles())
        while (newTail > head - maxPoles) {
            val next = origin.relative(facing, newTail - 1)
            if (!level.isLoaded(next)) return false
            if (!isPole(level.getBlockState(next))) break
            newTail--
        }

        var newHead = inHousing
        while (newHead < head) {
            val next = origin.relative(facing, newHead + 1)
            if (!level.isLoaded(next)) return false
            val state = level.getBlockState(next)
            if (!isPole(state) && !(newHead + 1 == head && isHead(state))) break
            newHead++
        }
        if (newHead < head || !isHead(level.getBlockState(origin.relative(facing, newHead)))) {
            if (!snapHead(level, ship, origin, newTail, newHead)) return false
        } else if (newTail > tail) {
            val rear = (tail until newTail).map { origin.relative(facing, it) }.filterTo(mutableSetOf()) {
                isPole(level.getBlockState(it))
            }
            if (rear.isNotEmpty()) assembleToShipFull(level, rear)
        }
        if (newHead != head || newTail != tail) {
            head = newHead
            tail = newTail
            sendData()
        }
        return true
    }

    private fun snapHead(level: ServerLevel, ship: ServerShip, origin: BlockPos, newTail: Int, newHead: Int): Boolean {
        val attached = (newTail..newHead).mapTo(mutableSetOf()) { origin.relative(facing, it) }
        val blocks = shipBlocks(level, ship)
        if (blocks.isEmpty()) return false
        val detached = blocks - attached
        val oldHead = origin.relative(facing, head)
        if (detached.isNotEmpty()) {
            if (detached.any { level.getBlockState(it).inAssemblyBlacklist() }) return false
            val assembly = assembleLinearShip(level, detached)
            val movedHead = assembly.migrate(oldHead)
            if (oldHead in detached && isHead(level.getBlockState(movedHead))) level.removeBlock(movedHead, false)
        }
        val headPos = origin.relative(facing, newHead)
        val replacedPole = isPole(level.getBlockState(headPos))
        level.setBlockAndUpdate(headPos, ClockworkBlocks.PHYS_PISTON_HEAD.defaultState.setValue(BlockStateProperties.FACING, facing))
        if (replacedPole) {
            val pole = AllBlocks.PISTON_EXTENSION_POLE.defaultState.setValue(BlockStateProperties.FACING, facing)
            Block.dropResources(pole, level, headPos)
        }
        playAssemblySound(level, worldPosition, false)
        return true
    }

    override fun disassemble(level: ServerLevel) {
        val origin = shaftOrigin ?: return
        val ship = level.shipObjectWorld.loadedShips.getById(shaftShipId) ?: return
        val parent = level.getLoadedShipManagingPos(worldPosition)
        val offset = displacement(parent, ship, worldPosition, origin, facing).roundToInt().coerceIn(-head, -tail)
        val internal = origin.relative(facing, -offset)
        val blocks = shipBlocks(level, ship)
        if (internal !in blocks || !moveOffShip(level, blocks - internal, internal, worldPosition, ship)) return
        level.setBlockAndUpdate(internal, Blocks.AIR.defaultBlockState())
        disconnect(level, if (offset == -head) PistonState.RETRACTED else PistonState.EXTENDED)
        playAssemblySound(level, worldPosition, false)
    }

    private fun disconnect(level: ServerLevel, state: PistonState = PistonState.RETRACTED) {
        releaseShaft(level)
        shaftOrigin = null
        shaftShipId = -1L
        waitForStop = true
        head = 0
        tail = 0
        level.setBlockAndUpdate(worldPosition, blockState.setValue(STATE, state))
        sendData()
    }

    private fun releaseShaft(level: ServerLevel) {
        stopDrive()
        level.shipObjectWorld.loadedShips.getById(shaftShipId)?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting()
    }

    override fun destroy() {
        (level as? ServerLevel)?.let(::releaseShaft)
        super.destroy()
    }

    private fun isPole(state: BlockState) =
        AllBlocks.PISTON_EXTENSION_POLE.has(state) && state.getValue(BlockStateProperties.FACING).axis == facing.axis

    private fun isHead(state: BlockState) =
        ClockworkBlocks.PHYS_PISTON_HEAD.has(state) && state.getValue(BlockStateProperties.FACING) == facing

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        shaftOrigin?.let { tag.putLong("ShaftOrigin", it.asLong()) }
        tag.putLong("ShaftShip", shaftShipId)
        tag.putInt("Head", head)
        tag.putInt("Tail", tail)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        shaftOrigin = BlockPos.of(tag.getLong("ShaftOrigin")).takeIf { tag.contains("ShaftOrigin") }
        shaftShipId = tag.getLong("ShaftShip")
        head = tag.getInt("Head")
        tail = tag.getInt("Tail")
    }
}
