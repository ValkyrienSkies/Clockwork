package org.valkyrienskies.clockwork.compat.jei.categories

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import kotlin.math.ceil
import kotlin.math.floor

internal class SiphoningPreview {
    private class Target(val entity: Entity, val scale: Float, val hit: Vec3) {
        val baseDistance = 2.5 + maxOf(entity.bbWidth, entity.bbHeight).coerceIn(1f, 6f)
    }

    private val targets = mutableMapOf<EntityType<*>, Target?>()
    private var level: ClientLevel? = null
    private val launcher = ClockworkBlocks.SIPHON_HARPOON.asStack()
    private val view = Axis.XP.rotationDegrees(-18f).mul(Axis.YP.rotationDegrees(-39f))
    private val cameraView = Quaternionf(view).conjugate()
    private val downrange = Axis.XP.rotationDegrees(90f)
    private val muzzle = Vec3(0.0, 0.0, 5.0 / 16)

    fun draw(graphics: GuiGraphics, type: EntityType<*>, x: Int, y: Int) {
        val minecraft = Minecraft.getInstance()
        val world = minecraft.level ?: return
        if (world !== level) {
            targets.clear()
            level = world
        }
        if (type !in targets) targets[type] = createTarget(world, type)
        val target = targets[type] ?: return
        val impactZ = target.baseDistance + 1.25 * Mth.sin(AnimationTickHolder.getRenderTime() / 28f)
        val dispatcher = minecraft.entityRenderDispatcher
        val pose = graphics.pose()
        val matrix = pose.last().pose()
        val top = matrix.transformPosition(x.toFloat(), y.toFloat(), 0f, Vector3f())
        val bottom = matrix.transformPosition(x + 120f, y + 64f, 0f, Vector3f())
        val camera = dispatcher.cameraOrientation()
        val guiProjection = RenderSystem.getProjectionMatrix()
        val sorting = RenderSystem.getVertexSorting()
        graphics.flush()
        graphics.enableScissor(floor(top.x).toInt(), floor(top.y).toInt(), ceil(bottom.x).toInt(), ceil(bottom.y).toInt())
        pose.pushPose()
        try {
            pose.translate(x + 20f, y + 46f, 150f)
            pose.mulPoseMatrix(Matrix4f().scaling(24f, 24f, -24f))
            val previewToView = Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose())
            // Give the recipe a perspective camera seven blocks behind the launcher.
            val projection = Matrix4f(guiProjection).mul(previewToView)
                .mul(Matrix4f().m23(1f / 7f)).mul(previewToView.invert())
            RenderSystem.setProjectionMatrix(projection, sorting)
            pose.mulPose(Axis.ZP.rotationDegrees(180f))
            pose.mulPose(view)
            dispatcher.overrideCameraOrientation(cameraView)
            Lighting.setupForEntityInInventory()
            RenderSystem.runAsFancy {
                drawTarget(graphics, target, impactZ)
                RopeKnotRenderer.strand(world, hoseTo(impactZ), Vec3.ZERO, pose, graphics.bufferSource(), RopeKnotRenderer.Material.HOSE,
                    packedLight = LightTexture.FULL_BRIGHT)
                pose.mulPose(downrange)
                minecraft.itemRenderer.renderStatic(launcher, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, graphics.bufferSource(), world, 0)
                pose.translate(0.0, impactZ, 0.0)
                RopeKnotRenderer.harpoonModel(pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT)
                graphics.flush()
            }
        } finally {
            RenderSystem.setProjectionMatrix(guiProjection, sorting)
            dispatcher.overrideCameraOrientation(camera)
            pose.popPose()
            graphics.disableScissor()
            Lighting.setupFor3DItems()
        }
    }

    private fun hoseTo(impactZ: Double): List<Vec3> {
        val length = RopeKnotRenderer.harpoonRopeLength(impactZ - muzzle.z)
        val tail = muzzle.add(0.0, 0.0, length)
        val curve = WandRopeCurve(muzzle, tail, length * 1.03)
        val segments = ceil(length * 2).toInt().coerceAtLeast(8)
        return (0..segments).map { curve.point(it.toDouble() / segments) }
    }

    private fun createTarget(world: ClientLevel, type: EntityType<*>): Target? {
        val entity = if (type == EntityType.PLAYER) RemotePlayer(world, Minecraft.getInstance().user.gameProfile)
            else type.create(world) ?: return null
        if (entity is LivingEntity) {
            entity.yRot = 180f
            entity.yRotO = 180f
            entity.yBodyRot = 180f
            entity.yBodyRotO = 180f
            entity.yHeadRot = 180f
            entity.yHeadRotO = 180f
        }
        return when (entity) {
            is Ghast -> Target(entity, .30f, Vec3(-.75, 1.8, -2.15))
            is EnderDragon -> {
                entity.flapTime = .6f
                entity.oFlapTime = entity.flapTime
                Target(entity, .24f, Vec3(-.35, 1.05, -5.7))
            }
            else -> Target(entity, minOf(1.7f / entity.bbHeight.coerceAtLeast(.25f), 2f / entity.bbWidth.coerceAtLeast(.25f)),
                Vec3(-entity.bbWidth * .16, entity.bbHeight * .45, -entity.bbWidth * .18))
        }
    }

    private fun drawTarget(graphics: GuiGraphics, target: Target, impactZ: Double) {
        val renderer = Minecraft.getInstance().entityRenderDispatcher.getRenderer(target.entity) ?: return
        val pose = graphics.pose()
        pose.pushPose()
        try {
            pose.translate(0.0, 0.0, impactZ)
            pose.scale(target.scale, target.scale, target.scale)
            pose.translate(-target.hit.x, -target.hit.y, -target.hit.z)
            renderer.render(target.entity, 180f, 1f, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT)
        } finally {
            pose.popPose()
        }
    }
}
