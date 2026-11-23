package org.valkyrienskies.clockwork.compat.jei.categories

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotRenderer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.sin

internal class SiphoningPreview {
    private val entities = mutableMapOf<EntityType<*>, Entity?>()
    private var level: ClientLevel? = null

    fun draw(graphics: GuiGraphics, type: EntityType<*>) {
        val minecraft = Minecraft.getInstance()
        val world = minecraft.level ?: return
        if (world !== level) {
            entities.clear()
            level = world
        }
        if (type !in entities) {
            val entity = if (type == EntityType.PLAYER) RemotePlayer(world, minecraft.user.gameProfile) else type.create(world)
            if (entity is LivingEntity) {
                entity.yRot = 180f
                entity.yRotO = 180f
                entity.yBodyRot = 180f
                entity.yBodyRotO = 180f
                entity.yHeadRot = 180f
                entity.yHeadRotO = 180f
            }
            entities[type] = entity
        }
        val entity = entities[type] ?: return
        val dispatcher = minecraft.entityRenderDispatcher
        val renderer = dispatcher.getRenderer(entity) ?: return
        val pose = graphics.pose()
        val matrix = pose.last().pose()
        val top = matrix.transformPosition(0f, 0f, 0f, Vector3f())
        val bottom = matrix.transformPosition(99f, 61f, 0f, Vector3f())
        val scale = when (entity) {
            is Ghast -> 6.4f
            is EnderDragon -> 5f
            else -> minOf(45f / entity.bbHeight.coerceAtLeast(.25f), 47f / entity.bbWidth.coerceAtLeast(.25f))
        }
        val baseY = when (entity) {
            is Ghast -> 32f
            is EnderDragon -> 42f
            else -> 54f
        }
        val view = Axis.XP.rotationDegrees(if (entity is EnderDragon) 18f else 10f)
            .mul(Axis.YP.rotationDegrees(if (entity is EnderDragon) 50f else 27f))
        val camera = dispatcher.cameraOrientation()
        graphics.flush()
        graphics.enableScissor(floor(top.x).toInt(), floor(top.y).toInt(), ceil(bottom.x).toInt(), ceil(bottom.y).toInt())
        pose.pushPose()
        try {
            pose.translate(if (entity is EnderDragon) 46f else 55f, baseY, 50f)
            pose.mulPoseMatrix(Matrix4f().scaling(scale, scale, -scale))
            pose.mulPose(Axis.ZP.rotationDegrees(180f))
            pose.mulPose(view)
            dispatcher.overrideCameraOrientation(Quaternionf(view).conjugate())
            Lighting.setupForEntityInInventory()
            RenderSystem.runAsFancy {
                renderer.render(entity, 180f, 1f, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT)
                drawHarpoon(graphics, world, entity, scale)
                graphics.flush()
            }
        } finally {
            dispatcher.overrideCameraOrientation(camera)
            pose.popPose()
            graphics.disableScissor()
            Lighting.setupFor3DItems()
        }
    }

    private fun drawHarpoon(graphics: GuiGraphics, world: ClientLevel, entity: Entity, scale: Float) {
        val tip = when (entity) {
            is EnderDragon -> Vec3(.35, 1.05, -5.7)
            is Ghast -> Vec3(2.05, 1.8, -.9)
            else -> Vec3(entity.bbWidth * .34, entity.bbHeight * .56, -entity.bbWidth * .19)
        }
        val pose = graphics.pose()
        pose.translate(tip.x, tip.y, tip.z)
        val harpoonScale = 22f / scale
        pose.scale(harpoonScale, harpoonScale, harpoonScale)
        val direction = (if (entity is EnderDragon) Vec3(.35, .8, .62) else Vec3(-.9, .36, .24)).normalize()
        val tail = direction.scale(-13.0 / 16.0)
        val hose = (0..4).map { i ->
            val t = i / 4.0
            tail.add(direction.scale(-.55 * t)).add(0.0, -.07 * sin(PI * t), 0.0)
        }
        RopeKnotRenderer.strand(world, hose, Vec3.ZERO, pose, graphics.bufferSource(), RopeKnotRenderer.Material.HOSE,
            packedLight = LightTexture.FULL_BRIGHT)
        pose.mulPose(Quaternionf().rotationTo(0f, 1f, 0f, direction.x.toFloat(), direction.y.toFloat(), direction.z.toFloat()))
        RopeKnotRenderer.harpoonModel(pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT)
    }
}
