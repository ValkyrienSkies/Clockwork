package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllKeys
import com.simibubi.create.AllSpecialTextures
import com.simibubi.create.content.schematics.client.ToolSelectionScreen
import com.simibubi.create.content.schematics.client.tools.ToolType
import com.simibubi.create.foundation.utility.RaycastHelper
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.outliner.AABBOutline
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import kotlin.math.sign

object VdexPlacementClient {
    private var id = ""
    private var slot = -1
    private var level: Any? = null
    private var active = false
    private var deployed = false
    private var placement = VdexPlacement(Vec3.ZERO, 0f)
    private var target: VdexPlacement? = null
    private var selection: ToolSelectionScreen? = null
    private var tool = ToolType.DEPLOY
    private var range = 10
    private var face: Direction? = null
    private var data: VdexBlueprintData? = null
    private var outline: AABBOutline? = null
    private var pendingTicks = 0
    private val animation = VdexPlacementAnimation()

    fun tick() {
        val mc = Minecraft.getInstance()
        val player = mc.player
        val stack = player?.mainHandItem
        val file = stack?.tag?.getString("VdexFile").orEmpty()
        if (mc.screen != null || player == null || player.isSpectator || !player.mayBuild() || stack?.item !is VdexBlueprintItem || !VdexBlueprintData.validId(file)) {
            active = false; id = ""; selection = null; target = null
            animation.reset()
            return
        }
        val blueprint = VdexBlueprintClient.get(file) ?: run { active = false; target = null; animation.reset(); return }
        if (id != file || slot != player.inventory.selected || level !== mc.level) {
            id = file; slot = player.inventory.selected; level = mc.level; data = blueprint
            val tag = stack.tag!!
            deployed = tag.getBoolean("Deployed")
            placement = VdexPlacement(Vec3(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ")), tag.getFloat("VdexYaw"))
            outline = AABBOutline(blueprint.bounds)
            range = (blueprint.bounds.size / 2).toInt().coerceIn(1, 100)
            menu(if (deployed) ToolType.MOVE else ToolType.DEPLOY)
            pendingTicks = 0
            animation.reset()
        }
        if (pendingTicks > 0) pendingTicks-- else {
            val tag = stack.tag!!
            if (deployed != tag.getBoolean("Deployed")) {
                deployed = tag.getBoolean("Deployed")
                menu(if (deployed) ToolType.MOVE else ToolType.DEPLOY)
            }
            if (deployed) placement = VdexPlacement(Vec3(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ")), tag.getFloat("VdexYaw"))
        }
        active = true
        selection?.update()
        target = null
        if (tool == ToolType.DEPLOY) {
            val point = if (AllKeys.ACTIVATE_TOOL.isPressed) {
                Vec3.atBottomCenterOf(BlockPos.containing(player.eyePosition.add(player.lookAngle.scale(range.toDouble()))))
            } else {
                val hit = RaycastHelper.rayTraceRange(player.level(), player, 75.0)
                if (hit == null || hit.type != HitResult.Type.BLOCK) null else {
                    var block = hit.blockPos
                    if (hit.direction.axis.isVertical && !player.level().getBlockState(block).canBeReplaced()) block = block.relative(hit.direction)
                    Vec3.atBottomCenterOf(block)
                }
            }
            target = point?.let { VdexPlacement.at(blueprint.bounds, it, placement.yaw) }
        }
        val focused = AllKeys.TOOL_MENU.isPressed
        selection?.let {
            if (it.focused && !focused) it.onClose()
            it.focused = focused
        }
        animation.tick(if (tool == ToolType.DEPLOY) target else placement.takeIf { deployed }, blueprint.bounds.center)
        face = if (deployed) view()?.face(blueprint.bounds, player.eyePosition, player.eyePosition.add(player.lookAngle.scale(70.0))) else null
    }

    private fun menu(selected: ToolType) {
        val tools = if (!deployed) listOf(ToolType.DEPLOY) else buildList {
            add(ToolType.MOVE); add(ToolType.MOVE_Y); add(ToolType.DEPLOY); add(ToolType.ROTATE)
            if (Minecraft.getInstance().player?.isCreative == true) add(ToolType.PRINT)
        }
        selection = ToolSelectionScreen(tools) { tool = it }
        tool = selected
        selection!!.setSelectedElement(selected)
    }

    private fun available(): Boolean {
        val mc = Minecraft.getInstance()
        return active && mc.screen == null && mc.player?.inventory?.selected == slot &&
            mc.player?.mainHandItem?.item is VdexBlueprintItem && mc.player?.mainHandItem?.tag?.getString("VdexFile") == id
    }

    fun view(): VdexPlacement? = if (!available()) null else data?.let {
        animation.value(it.bounds.center, AnimationTickHolder.getPartialTicks())
    }

    private fun sync(next: VdexPlacement): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        if (!next.position.x.isFinite() || !next.position.y.isFinite() || !next.position.z.isFinite() ||
            player.position().distanceToSqr(next.position) > 256.0 * 256.0 ||
            !player.level().worldBorder.isWithinBounds(next.position.x, next.position.z)) return false
        placement = next
        pendingTicks = 3
        deployed = true
        player.mainHandItem.orCreateTag.apply {
            putBoolean("Deployed", true)
            putDouble("VdexX", next.position.x); putDouble("VdexY", next.position.y); putDouble("VdexZ", next.position.z)
            putFloat("VdexYaw", next.yaw)
        }
        ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.DEPLOY, id, next.position, next.yaw))
        return true
    }

    @JvmStatic
    fun mouseScrolled(delta: Double): Boolean {
        if (!available() || delta == 0.0) return false
        if (selection?.focused == true || AllKeys.TOOL_MENU.isPressed) {
            selection?.cycle(sign(delta).toInt()); return true
        }
        if (!AllKeys.ctrlDown()) return false
        val step = sign(delta)
        when (tool) {
            ToolType.DEPLOY -> if (AllKeys.ACTIVATE_TOOL.isPressed) range = (range + step.toInt()).coerceIn(1, 100)
            ToolType.MOVE -> face?.takeIf { it.axis.isHorizontal }?.let {
                val movement = Vec3.atLowerCornerOf(it.normal).scale(-step).yRot(Math.toRadians(placement.yaw.toDouble()).toFloat())
                sync(placement.copy(position = placement.position.add(movement)))
            }
            ToolType.MOVE_Y -> sync(placement.copy(position = placement.position.add(0.0, step, 0.0)))
            ToolType.ROTATE -> data?.let { sync(placement.rotate(it.bounds, if (delta > 0) -90f else 90f)) }
            else -> return false
        }
        return true
    }

    @JvmStatic
    fun renderHud(graphics: GuiGraphics, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        if (available() && !mc.options.hideGui) selection?.renderPassive(graphics, partialTicks)
    }

    fun rightClick(): Boolean {
        if (!available() || Minecraft.getInstance().player?.isShiftKeyDown == true) return false
        when (tool) {
            ToolType.DEPLOY -> target?.let { if (sync(it)) menu(ToolType.MOVE) }
            ToolType.PRINT -> if (deployed) ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.PASTE, id))
            else -> Unit
        }
        return true
    }

    @JvmStatic
    fun onMouseInput(button: Int, pressed: Boolean): Boolean {
        val mc = Minecraft.getInstance()
        if (button != 1 || !pressed || mc.screen != null || !active) return false
        val hit = mc.hitResult as? BlockHitResult
        if (hit != null) {
            val state = mc.level?.getBlockState(hit.blockPos)
            if (state != null && (AllBlocks.SCHEMATICANNON.has(state) || AllBlocks.DEPLOYER.has(state))) return false
        }
        return rightClick()
    }

    fun renderOutline(pose: PoseStack, camera: Vec3) {
        val view = view() ?: return
        val box = outline ?: return
        if (Minecraft.getInstance().screen != null) return
        pose.pushPose()
        pose.translate(view.position.x - camera.x, view.position.y - camera.y, view.position.z - camera.z)
        pose.mulPose(Axis.YP.rotationDegrees(view.yaw))
        val params = box.params.colored(0x8EAEC9).lineWidth(1 / 32f)
        if (tool == ToolType.MOVE) params.highlightFace(face).withFaceTextures(AllSpecialTextures.CHECKERED,
            if (AllKeys.ctrlDown()) AllSpecialTextures.HIGHLIGHT_CHECKERED else AllSpecialTextures.CHECKERED)
        val buffers = DefaultSuperRenderTypeBuffer.getInstance()
        box.render(pose, buffers, Vec3.ZERO, AnimationTickHolder.getPartialTicks())
        params.clearTextures()
        buffers.draw()
        pose.popPose()
    }
}

private class VdexPlacementAnimation {
    private var center: Vec3? = null
    private var previous = Vec3.ZERO
    private val rotation = LerpedFloat.angular()

    fun reset() { center = null }

    fun tick(target: VdexPlacement?, pivot: Vec3) {
        if (target == null) { reset(); return }
        val destination = target.world(pivot)
        val current = center
        if (current == null) {
            center = destination
            previous = destination
            rotation.chase(target.yaw.toDouble(), .45, LerpedFloat.Chaser.EXP).startWithValue(target.yaw.toDouble())
            return
        }
        previous = current
        center = if (current.distanceToSqr(destination) < 1e-10) destination else current.lerp(destination, .45)
        rotation.updateChaseTarget(target.yaw)
        rotation.tickChaser()
    }

    fun value(pivot: Vec3, partialTick: Float): VdexPlacement? {
        val current = center ?: return null
        val partial = partialTick.coerceIn(0f, 1f)
        val yaw = rotation.getValue(partial)
        val position = previous.lerp(current, partial.toDouble())
            .subtract(pivot.yRot(Math.toRadians(yaw.toDouble()).toFloat()))
        return VdexPlacement(position, yaw)
    }
}
