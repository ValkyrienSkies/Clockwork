package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import java.util.WeakHashMap
import java.util.function.BooleanSupplier
import net.minecraft.world.inventory.AbstractContainerMenu

object GatemakyrBlockInteraction {
    private data class Target(val hit: BlockHitResult, val path: GatemakyrRayPath)
    private data class Menu(val id: Int, val target: Target)
    private val mining = WeakHashMap<ServerPlayer, MutableMap<BlockPos, Target>>()
    private val menus = WeakHashMap<ServerPlayer, Menu>()
    private val active = ThreadLocal<Double>()
    private data class MenuReach(val player: ServerPlayer, val pos: BlockPos, val distanceSquared: Double)
    private val checkingMenu = ThreadLocal<MenuReach>()

    private fun pick(player: ServerPlayer, direction: Vec3, reach: Double): Target? {
        val eye = player.eyePosition
        val ray = ClipContext(eye, eye.add(direction.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
        val hit = GatemakyrRaycast.clip(player.serverLevel(), ray, false, null, false) ?: return null
        val path = (hit as GatemakyrHit).gatePath ?: return null
        return if (hit.type == HitResult.Type.BLOCK && path.distance <= reach + 1e-6) Target(hit, path) else null
    }

    private fun via(player: ServerPlayer, hit: BlockHitResult, gates: List<GatemakyrPlayerGate>, reach: Double): Target? {
        val level = player.serverLevel()
        val live = GatemakyrServer.playerGates(level)
        val reverse = gates.asReversed().map { prior ->
            val gate = live[prior.packet.owner to prior.packet.side]?.takeIf(prior::matches) ?: return null
            GatemakyrPassage(gate.passage.exit, gate.passage.entrance)
        }
        fun check(world: Vec3): Target? {
            val image = reverse.fold(world) { point, passage -> passage.point(point) }
            val delta = image.subtract(player.eyePosition)
            if (delta.lengthSqr() > (reach + 1e-5) * (reach + 1e-5)) return null
            val result = pick(player, delta.normalize(), reach) ?: return null
            return result.takeIf { it.hit.blockPos == hit.blockPos && it.path.gates.size == gates.size &&
                gates.indices.all { index -> gates[index].matches(it.path.gates[index]) } }
        }
        val world = level.toWorldCoordinates(hit.location)
        check(world)?.let { return it }
        if (level.getShipManagingPos(hit.blockPos) != null) {
            for (gate in live.values) check(gate.passage.point(world))?.let { return it }
        }
        return null
    }

    private fun target(player: ServerPlayer, pos: BlockPos, claimed: BlockHitResult?, face: Direction): Target? {
        val gates = GatemakyrServer.playerGates(player.serverLevel())
        if (gates.isEmpty()) return null
        val reach = PlatformUtils.getBlockReach(player)
        fun matches(found: Target?) = found != null && found.hit.blockPos == pos && (claimed == null ||
            found.hit.direction == claimed.direction && found.hit.location.distanceToSqr(claimed.location) < 0.01)
        val aimed = pick(player, player.lookAngle, reach)
        if (matches(aimed)) return aimed
        val point = claimed ?: BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.normal).scale(0.5)), face, pos, false)
        for (gate in gates.values) {
            val found = via(player, point, listOf(gate), reach)
            if (matches(found)) return found
        }
        return null
    }

    private inline fun scoped(path: Target?, run: () -> Unit) {
        val previous = active.get()
        if (path == null) active.remove() else active.set(path.path.distance)
        try { run() } finally { if (previous == null) active.remove() else active.set(previous) }
    }

    @JvmStatic fun reached() = active.get() != null
    @JvmStatic fun distanceSquared(original: Double): Double = active.get()?.let { it * it } ?: original

    @JvmStatic fun use(player: ServerPlayer, hit: BlockHitResult, run: Runnable) {
        if (!player.serverLevel().server.isSameThread) { run.run(); return }
        val path = target(player, hit.blockPos, hit, hit.direction)
        val before = player.containerMenu
        scoped(path) { run.run() }
        if (player.containerMenu !== before) {
            if (path != null) menus[player] = Menu(player.containerMenu.containerId, path) else menus.remove(player)
        }
    }

    @JvmStatic fun mine(player: ServerPlayer, pos: BlockPos, face: Direction, action: Action, run: Runnable) {
        val prior = mining[player]?.get(pos)
        val path = if (action == Action.START_DESTROY_BLOCK || prior == null) target(player, pos, null, face)
            else via(player, prior.hit, prior.path.gates, PlatformUtils.getBlockReach(player))
        if (action == Action.START_DESTROY_BLOCK) {
            if (path == null) mining[player]?.remove(pos) else mining.getOrPut(player) { mutableMapOf() }[pos.immutable()] = path
        }
        scoped(path) { run.run() }
    }

    @JvmStatic fun retainMining(player: ServerPlayer, current: BlockPos?, delayed: BlockPos?) {
        val targets = mining[player] ?: return
        targets.keys.removeIf { it != current && it != delayed }
        if (targets.isEmpty()) mining.remove(player)
    }

    @JvmStatic fun canFinishMining(player: ServerPlayer, pos: BlockPos): Boolean {
        val prior = mining[player]?.get(pos) ?: return true
        return via(player, prior.hit, prior.path.gates, PlatformUtils.getBlockReach(player)) != null
    }

    @JvmStatic fun checkMenu(menu: AbstractContainerMenu, player: Player, check: BooleanSupplier): Boolean {
        if (player !is ServerPlayer) return check.asBoolean
        val record = menus[player] ?: return check.asBoolean
        if (player.containerMenu.containerId != record.id) {
            menus.remove(player)
            return check.asBoolean
        }
        if (menu !== player.containerMenu) return check.asBoolean
        val target = record.target
        val distance = via(player, target.hit, target.path.gates, maxOf(8.0, PlatformUtils.getBlockReach(player)))?.path?.distance
            ?: return check.asBoolean
        val previous = checkingMenu.get()
        checkingMenu.set(MenuReach(player, target.hit.blockPos, distance * distance))
        return try { check.asBoolean } finally { if (previous == null) checkingMenu.remove() else checkingMenu.set(previous) }
    }

    @JvmStatic fun menuPlayer(): ServerPlayer? = checkingMenu.get()?.player

    @JvmStatic fun menuDistance(original: Double, player: Player, x: Double, y: Double, z: Double): Double {
        val menu = checkingMenu.get() ?: return original
        if (menu.player !== player) return original
        val pos = BlockPos.containing(x, y, z)
        if (pos != menu.pos) {
            val state = player.level().getBlockState(menu.pos)
            if (state.block !is ChestBlock || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE ||
                menu.pos.relative(ChestBlock.getConnectedDirection(state)) != pos) return original
        }
        return menu.distanceSquared
    }
}
