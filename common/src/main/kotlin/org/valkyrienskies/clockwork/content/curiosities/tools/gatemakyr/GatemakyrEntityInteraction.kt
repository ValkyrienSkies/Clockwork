package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.PlatformUtils

object GatemakyrEntityInteraction {
    @JvmStatic fun pick(player: Player, pt: Float): EntityHitResult? {
        val eye = player.getEyePosition(pt)
        val range = PlatformUtils.getEntityReach(player)
        val end = eye.add(player.getViewVector(pt).scale(range))
        val ray = ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
        val block = GatemakyrRaycast.clip(player.level(), ray, true, null, false) ?: return null
        val path = (block as? GatemakyrHit)?.gatePath ?: return null
        var start = eye
        fun nearest(to: Vec3): EntityHitResult? {
            val bounds = AABB(start, to).inflate(1.0)
            return ProjectileUtil.getEntityHitResult(player, start, to, bounds,
                { !it.isSpectator && it.isPickable }, start.distanceToSqr(to) + 1e-8)
        }
        for ((index, crossing) in path.crossings.withIndex()) {
            val hit = nearest(crossing.first)
            if (hit != null) return hit.takeIf { index > 0 }
            start = crossing.second
        }
        if (!player.level().hasChunkAt(BlockPos.containing(start))) return null
        return nearest(block.location)
    }

    @JvmStatic fun reached(player: ServerPlayer, packet: ServerboundInteractPacket): Boolean {
        val target = packet.getTarget(player.serverLevel()) ?: return false
        return pick(player, 1f)?.entity === target
    }
}
