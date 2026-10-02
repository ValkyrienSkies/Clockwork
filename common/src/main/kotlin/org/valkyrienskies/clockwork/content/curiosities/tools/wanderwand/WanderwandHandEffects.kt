package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.valkyrienskies.clockwork.mixin.content.gravitron.GameRendererAccessor
import java.util.UUID
import kotlin.math.PI
import kotlin.math.tan

object WanderwandHandEffects {
    class State(val slot: Int, now: Long) {
        val animation = WanderwandAnimation().also { it.accept("draw", now) }
        var viewTip: Vec3? = null
        var worldTip: Vec3? = null
        var viewTime = -1L
        var worldTime = -1L
    }
    private data class Owner(val player: Player?)
    private val owners = ArrayDeque<Owner>()
    private val states = mutableMapOf<UUID, State>()
    private var world: ClientLevel? = null
    fun tick() {
        val level = Minecraft.getInstance().level
        if (world !== level) { states.clear(); owners.clear(); world = level }
        if (level == null) return
        states.keys.retainAll(level.players().map { it.uuid }.toSet())
        for (player in level.players()) {
            if (player.mainHandItem.item !is WanderwandItem) { states.remove(player.uuid); continue }
            if (states[player.uuid]?.slot != player.inventory.selected) states[player.uuid] = State(player.inventory.selected, level.gameTime)
        }
    }
    fun accept(owner: UUID, action: String) {
        val level = Minecraft.getInstance().level ?: return
        val player = level.getPlayerByUUID(owner) ?: return
        states.getOrPut(owner) { State(player.inventory.selected, level.gameTime) }.animation.accept(action, level.gameTime)
    }
    @JvmStatic fun beginItem(entity: LivingEntity, stack: ItemStack, context: ItemDisplayContext) {
        val right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        val held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        owners.addLast(Owner((entity as? Player)?.takeIf { held && stack.item is WanderwandItem &&
            (it.mainArm == HumanoidArm.RIGHT) == right && it.mainHandItem.item == stack.item }))
    }
    @JvmStatic fun endItem() { if (owners.isNotEmpty()) owners.removeLast() }
    fun player() = owners.lastOrNull()?.player
    fun state(player: Player?) = player?.let { states[it.uuid] }
    fun capture(player: Player, context: ItemDisplayContext, ms: PoseStack, point: Vector3f) {
        val state = state(player) ?: return
        val p = ms.last().pose().transformPosition(point)
        val tip = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
        val time = player.level().gameTime
        if (context.firstPerson()) { state.viewTip = tip; state.viewTime = time }
        else { state.worldTip = tip; state.worldTime = time }
    }
    fun tip(player: Player, partial: Float, inverseView: Matrix4f): Vec3 {
        val mc = Minecraft.getInstance()
        val state = state(player)
        val camera = mc.gameRenderer.mainCamera
        val now = player.level().gameTime
        var point: Vec3? = null
        if (player === mc.player && mc.options.cameraType.isFirstPerson) {
            if (state?.viewTip != null && now - state.viewTime <= 1) {
                val accessor = mc.gameRenderer as GameRendererAccessor
                val ratio = tan(accessor.`clockwork$getFov`(camera, partial, true) * PI / 360) /
                    tan(accessor.`clockwork$getFov`(camera, partial, false) * PI / 360)
                point = state.viewTip!!.multiply(ratio, ratio, 1.0)
            }
        } else if (state?.worldTip != null && now == state.worldTime) point = state.worldTip
        if (point != null) {
            val p = inverseView.transformPosition(Vector3f(point.x.toFloat(), point.y.toFloat(), point.z.toFloat()))
            return camera.position.add(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
        }
        val view = player.getViewVector(partial)
        val right = Vec3(view.z, 0.0, -view.x).normalize().scale(if (player.mainArm == HumanoidArm.RIGHT) 0.3 else -0.3)
        return player.getEyePosition(partial).add(view.scale(0.65)).add(right).add(0.0, -0.05, 0.0)
    }
}
