package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import com.simibubi.create.content.contraptions.AssemblyException
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandGridRotation
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.core.internal.ships.VsiServerShip
import kotlin.math.abs
import kotlin.math.cos

object HingeAssembly {
    private const val SELECTION = "ClockworkHingeStart"

    fun use(level: ServerLevel, pos: BlockPos): String? {
        val state = level.getBlockState(pos)
        val block = state.block as? HingeBlock ?: return "missing"
        val link = HingeLinks.get(level).at(pos)
        if (link?.connected == true) return disassemble(level, link)
        if (block.attachment || state.getValue(HingeBlock.ASSEMBLED)) return "reconnect"
        return assemble(level, pos)
    }

    fun assemble(level: ServerLevel, pos: BlockPos): String? {
        val state = level.getBlockState(pos)
        if (state.block != ClockworkBlocks.HINGE.get() || state.getValue(HingeBlock.ASSEMBLED)) return "missing"
        val be = level.getBlockEntity(pos) as? HingeBlockEntity ?: return "missing"
        val facing = state.getValue(HingeBlock.FACING)
        val front = pos.relative(facing)
        val worldFront = level.getShipManagingPos(pos)?.shipToWorld?.transformPosition(HingeLinks.center(front))
            ?.let { BlockPos.containing(it.x, it.y, it.z) } ?: front
        if (!level.hasChunkAt(front) || level.isOutsideBuildHeight(front) || !level.worldBorder.isWithinBounds(worldFront)) return "blocked"
        val empty = level.getBlockState(front).isAir
        val head = ClockworkBlocks.HINGE_ATTACHMENT.defaultState.setValue(HingeBlock.FACING, facing)
            .setValue(HingeBlock.ROLL, state.getValue(HingeBlock.ROLL)).setValue(HingeBlock.ASSEMBLED, true)
        val positions = if (empty) setOf(front) else try {
            collectGlued(level, front) { p, _ -> p != pos }?.map { BlockPos(it.x(), it.y(), it.z()) }?.toSet() ?: return "missing"
        } catch (_: AssemblyException) { return "assembly_failed" }
        if (positions.any { !level.hasChunkAt(it) }) return "blocked"
        if (empty) level.setBlockAndUpdate(front, head)
        val oldMin = BlockPos(positions.minOf { it.x }, positions.minOf { it.y }, positions.minOf { it.z })
        val ship = try { ShipAssembler.assembleToShip(level, positions.map { it.mutable() }.toSet(), 1.0) }
        catch (e: Exception) {
            if (empty && level.getBlockState(front).block == head.block) level.removeBlock(front, false)
            throw e
        }
        val bounds = ship.shipAABB ?: return "assembly_failed"
        val delta = BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()).subtract(oldMin)
        val headPos = pos.offset(delta)
        level.setBlockAndUpdate(headPos, head)
        if (empty) level.removeBlock(front.offset(delta), false)
        val transform = ship.transform
        val center = Vector3d(ship.inertiaData.centerOfMass)
        val worldCenter = transform.shipToWorld.transformPosition(center, Vector3d())
        val velocity = Vector3d(ship.velocity).add(Vector3d(ship.angularVelocity)
            .cross(Vector3d(worldCenter).sub(transform.positionInWorld)))
        (ship as VsiServerShip).unsafeSetKinematics(vsCore.newBodyKinematics(velocity, ship.angularVelocity,
            vsCore.newBodyTransform(worldCenter, transform.shipToWorldRotation, transform.scaling, center)))
        val a = WandAnchor.at(level, pos, facing)
        val b = WandAnchor(headPos, facing, ship.id)
        HingeLinks.get(level).add(level, a, b, -be.lower.value, be.upper.value) ?: return "occupied"
        HingeLinks.worldPoint(level, a)?.let { ClockworkSounds.HINGE_ASSEMBLE.play(level, null, it, 1f, 1f) }
        return null
    }

    fun disassemble(level: ServerLevel, link: HingeLinks.Link): String? {
        if (!link.connected) return "reconnect"
        val rotationA = HingeLinks.worldRotation(level, link.a)
        val rotationB = HingeLinks.worldRotation(level, link.b)
        if (abs(rotationA.dot(rotationB)) < cos(Math.toRadians(2.5))) return "neutral"
        val source = level.shipObjectWorld.loadedShips.getById(link.b.shipId) ?: return "unloaded"
        if (link.a.shipId == source.id) return "same_body"
        val blocks = linkedSetOf<BlockPos>()
        var unloaded = false
        source.activeChunksSet.forEach { cx, cz ->
            if (!level.hasChunk(cx, cz)) { unloaded = true; return@forEach }
            for ((index, section) in level.getChunk(cx, cz).sections.withIndex()) {
                if (section.hasOnlyAir()) continue
                for (x in 0..15) for (y in 0..15) for (z in 0..15) {
                    if (!section.getBlockState(x, y, z).isAir && blocks.size <= 100000)
                        blocks.add(BlockPos(cx * 16 + x, level.minBuildHeight + index * 16 + y, cz * 16 + z))
                }
            }
        }
        if (unloaded || blocks.size > 100000) return "unloaded"
        val stateA = level.getBlockState(link.a.pos)
        val stateB = level.getBlockState(link.b.pos)
        val axisA = HingeGeometry.axis(stateA)
        val axisB = HingeGeometry.axis(stateB)
        val grid = WandGridRotation.all.firstOrNull {
            it.face(stateB.getValue(HingeBlock.FACING)) == stateA.getValue(HingeBlock.FACING) && it.face(axisB) == axisA
        } ?: return "align"
        val moves = blocks.filter { it != link.b.pos }.associateWith { link.a.pos.offset(grid.offset(it.subtract(link.b.pos))) }
        if (moves.values.any { !level.hasChunkAt(it) || level.isOutsideBuildHeight(it) ||
                !level.worldBorder.isWithinBounds(if (link.a.shipId < 0) it else HingeLinks.worldPoint(level, link.a)!!.let(BlockPos::containing)) || !level.getBlockState(it).isAir }) return "blocked"
        if (moves.isNotEmpty()) {
            if (!PhysBearingAssembler.moveBlocksWithTransform(level, moves, source, level.getShipManagingPos(link.a.pos),
                    link.b.pos, link.a.pos, grid.steps)) return "blocked"
            WandLinkTransfer.moveBlocks(level, moves, grid)
        }
        HingeLinks.get(level).remove(level, link)
        level.removeBlock(link.b.pos, false)
        level.setBlockAndUpdate(link.a.pos, stateA.setValue(HingeBlock.ASSEMBLED, false))
        HingeLinks.worldPoint(level, link.a)?.let { ClockworkSounds.HINGE_DISASSEMBLE.play(level, null, it, 1f, 1f) }
        return null
    }

    fun reconnect(player: ServerPlayer, pos: BlockPos, stack: ItemStack) {
        val level = player.serverLevel()
        if (stack.isEmpty) return
        val tag = stack.orCreateTag
        val previous = tag.getCompound(SELECTION)
        fun message(key: String) = player.displayClientMessage(Component.translatable("message.vs_clockwork.hinge.$key"), true)
        if (previous.getString("dimension") != level.dimension().location().toString() || !previous.contains("pos")) {
            tag.put(SELECTION, CompoundTag().also {
                it.putString("dimension", level.dimension().location().toString()); it.putLong("pos", pos.asLong())
            })
            message("selected")
            return
        }
        val first = BlockPos.of(previous.getLong("pos"))
        tag.remove(SELECTION)
        if (first == pos) { message("cancelled"); return }
        val error = connect(level, first, pos)
        if (error != null) message(error)
    }

    fun connect(level: ServerLevel, first: BlockPos, second: BlockPos): String? {
        if (!level.hasChunkAt(first) || !level.hasChunkAt(second)) return "unloaded"
        val firstBlock = level.getBlockState(first).block as? HingeBlock ?: return "missing"
        val secondBlock = level.getBlockState(second).block as? HingeBlock ?: return "missing"
        if (firstBlock.attachment == secondBlock.attachment) return "different_ends"
        val aPos = if (firstBlock.attachment) second else first
        val bPos = if (firstBlock.attachment) first else second
        val a = WandAnchor.at(level, aPos, level.getBlockState(aPos).getValue(HingeBlock.FACING))
        val b = WandAnchor.at(level, bPos, level.getBlockState(bPos).getValue(HingeBlock.FACING))
        val pointA = HingeLinks.worldPoint(level, a) ?: return "unloaded"
        val pointB = HingeLinks.worldPoint(level, b) ?: return "unloaded"
        if (pointA.distanceTo(pointB) > HingeGeometry.RECONNECT_DISTANCE) return "far"
        val qa = HingeLinks.worldRotation(level, a)
        val qb = HingeLinks.worldRotation(level, b)
        if (qa.transform(Vector3d(1.0, 0.0, 0.0)).dot(qb.transform(Vector3d(1.0, 0.0, 0.0))) < 0.966) return "align"
        val be = level.getBlockEntity(aPos) as? HingeBlockEntity ?: return "missing"
        val angle = HingeGeometry.angle(qa, qb)
        if (angle < -be.lower.value - 5 || angle > be.upper.value + 5) return "align"
        HingeLinks.get(level).add(level, a, b, -be.lower.value, be.upper.value) ?: return "occupied"
        ClockworkSounds.HINGE_ASSEMBLE.play(level, null, pointA, 1f, 1f)
        return null
    }
}
