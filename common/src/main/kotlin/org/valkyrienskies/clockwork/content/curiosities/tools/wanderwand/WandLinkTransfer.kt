package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Rotation
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotItem
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableItem
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.getShipManagingPos

object WandLinkTransfer {
    internal data class Destination(val anchor: WandAnchor, val rotation: Quaterniondc = Quaterniond())

    internal fun pose(pose: VSJointPose, from: WandAnchor, to: Destination): VSJointPose {
        val offset = Vector3d(pose.pos).sub(from.pos.x + 0.5, from.pos.y + 0.5, from.pos.z + 0.5)
        to.rotation.transform(offset)
        offset.add(to.anchor.pos.x + 0.5, to.anchor.pos.y + 0.5, to.anchor.pos.z + 0.5)
        return VSJointPose(offset, Quaterniond(to.rotation).mul(pose.rot).normalize())
    }

    @JvmStatic
    fun moveBlock(level: Level, from: BlockPos, to: BlockPos, rotation: Rotation) {
        if (level !is ServerLevel || from == to) return
        val turn = Quaterniond().rotationY(-rotation.ordinal * Math.PI / 2)
        remap(level) { anchor ->
            if (anchor.pos != from) null else Destination(anchor.copy(pos = to.immutable(),
                face = rotation.rotate(anchor.face), shipId = level.getShipManagingPos(to)?.id ?: -1), turn)
        }
    }

    @JvmStatic
    fun moveRegion(level: ServerLevel, blocks: Set<BlockPos>, origin: BlockPos, destination: BlockPos) {
        remap(level) { anchor ->
            if (anchor.pos !in blocks) null else {
                val pos = destination.offset(anchor.pos.subtract(origin))
                Destination(anchor.copy(pos = pos, shipId = level.getShipManagingPos(pos)?.id ?: -1))
            }
        }
    }

    fun moveBlocks(level: ServerLevel, moves: Map<BlockPos, BlockPos>, rotation: WandGridRotation) {
        fun axis(face: Direction) = rotation.face(face).normal.let { Vector3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
        val turn = Quaterniond().setFromNormalized(Matrix3d(axis(Direction.EAST), axis(Direction.UP), axis(Direction.SOUTH)))
        moveRotated(level, moves, turn)
    }

    @JvmStatic
    fun moveRotated(level: ServerLevel, moves: Map<BlockPos, BlockPos>, turn: Quaterniondc) {
        remap(level) { anchor ->
            moves[anchor.pos]?.let { pos ->
                val face = turn.transform(Vector3d(anchor.face.stepX.toDouble(), anchor.face.stepY.toDouble(), anchor.face.stepZ.toDouble()))
                Destination(anchor.copy(pos = pos.immutable(), face = Direction.getNearest(face.x, face.y, face.z),
                    shipId = level.getShipManagingPos(pos)?.id ?: -1), turn)
            }
        }
    }

    private fun remap(level: ServerLevel, destination: (WandAnchor) -> Destination?) {
        HingeLinks.get(level).remap(level, destination)
        val data = WandLinks.get(level)
        var changed = false
        for (link in data.links.values) {
            val a = destination(link.a)
            val b = destination(link.b)
            if (a == null && b == null) continue
            data.resetJoints(level, link)
            if (a != null) { link.pose0 = pose(link.pose0, link.a, a); link.a = a.anchor }
            if (b != null) { link.pose1 = pose(link.pose1, link.b, b); link.b = b.anchor }
            changed = true
        }
        for ((id, session) in data.sessions) {
            val first = session.first?.let(destination)
            val rope = session.rope?.let(destination)
            if (first != null) session.first = first.anchor
            if (rope != null) { session.rope = rope.anchor; session.previousAnchor = null }
            if (first != null || rope != null) level.server.playerList.getPlayer(id)?.let { WanderwandServer.sync(it, session) }
        }
        for (player in level.players()) for (stack in player.inventory.items + player.inventory.offhand) {
            if (stack.item !is RopeKnotItem && stack.item !is ThinkableItem) continue
            val start = RopeKnotItem.start(stack, level) ?: continue
            destination(start)?.let { RopeKnotItem.setStart(stack, level, it.anchor) }
        }
        if (changed) WanderwandServer.syncLinks(level)
    }
}
