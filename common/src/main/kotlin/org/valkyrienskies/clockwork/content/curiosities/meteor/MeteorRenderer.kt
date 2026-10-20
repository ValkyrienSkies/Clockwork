package org.valkyrienskies.clockwork.content.curiosities.meteor

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.BlockPos
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.core.api.bodies.properties.BodyTransform
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.util.GameTickOnly
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

object MeteorRenderer {

    private val heatShieldModelLocation = ModelResourceLocation(
        ResourceLocation.tryBuild(ClockworkMod.MOD_ID, "misc/heat_shield")!!,
        ""
    )
    private val meteorTrailModelLocation = ModelResourceLocation(
        ResourceLocation.tryBuild(ClockworkMod.MOD_ID, "misc/meteor_trail")!!,
        ""
    )
    val meteorList = hashMapOf<Long, MeteorVfxState>()
    private val activeMeteorIds = hashSetOf<Long>()
    private val spentMeteorIds = hashSetOf<Long>()
    private var trackedLevel: ClientLevel? = null

    fun setMeteorState(shipId: Long, active: Boolean) {
        val level = Minecraft.getInstance().level ?: return
        resetForLevel(level)
        if (active) {
            if (shipId !in spentMeteorIds) activeMeteorIds.add(shipId)
        } else {
            activeMeteorIds.remove(shipId)
            spentMeteorIds.add(shipId)
            meteorList.remove(shipId)
        }
    }

    private fun resetForLevel(level: ClientLevel) {
        if (trackedLevel === level) return
        trackedLevel = level
        activeMeteorIds.clear()
        spentMeteorIds.clear()
        meteorList.clear()
    }

    @OptIn(GameTickOnly::class)
    fun onShipRender(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource.BufferSource, pt: Float) {
        if (ship.id !in activeMeteorIds) return
        renderMeteor(ship, ms, camera, bufferSource, pt)
    }

    @OptIn(GameTickOnly::class)
    fun clientTick(level: ClientLevel, ships: Iterable<ClientShip>) {
        resetForLevel(level)
        val loadedMeteorIds = HashSet<Long>()
        for (ship in ships) {
            if (ship.id !in activeMeteorIds) continue
            loadedMeteorIds.add(ship.id)
            val state = meteorList.getOrPut(ship.id) { MeteorVfxState() }
            state.blocks = createState(level, ship).blocks
            updateMeteorStateWorld(ship, state)
        }
        meteorList.keys.retainAll(loadedMeteorIds)
    }

    @OptIn(GameTickOnly::class)
    private fun createState(level: ClientLevel, ship: ClientShip): MeteorVfxState {
        val blocks = mutableListOf<MeteorShipBlock>()
        val bounds = ship.shipAABB ?: return MeteorVfxState(blocks)
        val pos = BlockPos.MutableBlockPos()
        for (x in bounds.minX()..bounds.maxX()) {
            for (y in bounds.minY()..bounds.maxY()) {
                for (z in bounds.minZ()..bounds.maxZ()) {
                    pos.set(x, y, z)
                    val blockState = level.getBlockState(pos)
                    if (isMeteorMaterial(blockState) && blockState.renderShape == RenderShape.MODEL) {
                        blocks.add(MeteorShipBlock(pos.immutable(), blockState))
                    }
                }
            }
        }
        return MeteorVfxState(blocks)
    }

    private fun isMeteorMaterial(state: BlockState): Boolean =
        state.block == ClockworkBlocks.NYX.defaultState.block ||
            state.block == ClockworkBlocks.CHARGED_NYX.defaultState.block

    @OptIn(GameTickOnly::class)
    fun renderMeteor(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource.BufferSource, pt: Float) {
        if (ship.id !in activeMeteorIds) {
            meteorList.remove(ship.id)
            return
        }

        // LOD gate
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val state = meteorList[ship.id] ?: return
        val transform = interpolateTransform(ship, pt)
        val shipBounds = ship.shipAABB ?: return
        val localCenter = shipBounds.center(Vector3d())
        val center = transform.toWorld.transformPosition(localCenter, Vector3d())
        val animationTime = level.gameTime.toDouble() + pt
        val pulse = 1.1 + abs(sin(animationTime * 0.25)) * 0.5
        val renderBounds = ship.renderAABB
        val camPos = camera.position
        val dx = center.x - camPos.x
        val dy = center.y - camPos.y
        val dz = center.z - camPos.z
        val dist2 = dx*dx + dy*dy + dz*dz
        if (dist2 > 256.0.pow(2.0)) return

        ms.pushPose()
        ms.translate(-camPos.x, -camPos.y, -camPos.z)

        val yaw = transform.rotation.getEulerAnglesZXY(Vector3d()).y.toFloat()
        val halfExtents = renderBounds.extent(Vector3d())
        val halfExtentsShip = shipBounds.extent(Vector3d())

        renderMeteorOverlay(ms, bufferSource, state, transform, center, localCenter, animationTime)
        renderMeteorTrail(ms, bufferSource, ship, state, center, pt)
        renderHeatShield(
            ms = ms,
            bufferSource = bufferSource,
            centerX = center.x, centerY = center.y, centerZ = center.z,
            halfWidth = ((2*halfExtents.x()+halfExtentsShip.x())/3.0).coerceAtLeast(0.5),
            halfHeight = ((2*halfExtents.y()+halfExtentsShip.y())/3.0).coerceAtLeast(0.5),
            halfDepth = ((2*halfExtents.z()+halfExtentsShip.z())/3.0).coerceAtLeast(0.5),
            Rot = yaw
        )

        ms.popPose()
    }

    @OptIn(GameTickOnly::class)
    private fun interpolateTransform(ship: ClientShip, partialTick: Float): BodyTransform =
        ship.transformProvider?.provideNextRenderTransform(
            ship.prevTickTransform,
            ship.transform,
            partialTick.toDouble()
        ) ?: ship.renderTransform

    @OptIn(GameTickOnly::class)
    private fun renderMeteorOverlay(
        ms: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        state: MeteorVfxState,
        transform: BodyTransform,
        center: Vector3d,
        localCenter: Vector3d,
        animationTime: Double
    ) {
        if (ClockworkShaders.meteorGlow() == null || state.blocks.isEmpty()) return
        val mc = Minecraft.getInstance()
        val vertexConsumer = bufferSource.getBuffer(ClockworkRenderTypes.METEOR_GLOW)
        val blockPositions = state.blocks.mapTo(HashSet()) { it.pos }
        val shipToWorld = transform.toWorld
        val pose = ms.last()
        ms.pushPose()
        for (block in state.blocks) {
            val model = mc.blockRenderer.getBlockModel(block.state)
            if (!model.isCustomRenderer) {
                val random = RandomSource.create(42L)
                for (direction in Direction.values()) {
                    if (block.pos.relative(direction) in blockPositions) continue
                    random.setSeed(42L)
                    for (quad in model.getQuads(block.state, direction, random)) {
                        renderMeteorOverlayQuad(
                            vertexConsumer,
                            pose,
                            shipToWorld,
                            block.pos,
                            quad,
                            quad.direction,
                            center,
                            localCenter,
                            animationTime
                        )
                    }
                }
                random.setSeed(42L)
                for (quad in model.getQuads(block.state, null, random)) {
                    renderMeteorOverlayQuad(
                        vertexConsumer,
                        pose,
                        shipToWorld,
                        block.pos,
                        quad,
                        quad.direction,
                        center,
                        localCenter,
                        animationTime
                    )
                }
            }
        }
        ms.popPose()
        bufferSource.endBatch(ClockworkRenderTypes.METEOR_GLOW)
    }

    private fun renderMeteorOverlayQuad(
        vertexConsumer: com.mojang.blaze3d.vertex.VertexConsumer,
        pose: PoseStack.Pose,
        shipToWorld: org.joml.Matrix4dc,
        blockPos: BlockPos,
        quad: net.minecraft.client.renderer.block.model.BakedQuad,
        face: Direction,
        center: Vector3d,
        localCenter: Vector3d,
        animationTime: Double
    ) {
        val vertices = quad.vertices
        val quadCenter = Vector3d()
        val vertexCount = vertices.size / 8
        for (vertexIndex in 0 until vertexCount) {
            val offset = vertexIndex * 8
            quadCenter.add(
                blockPos.x + java.lang.Float.intBitsToFloat(vertices[offset]).toDouble(),
                blockPos.y + java.lang.Float.intBitsToFloat(vertices[offset + 1]).toDouble(),
                blockPos.z + java.lang.Float.intBitsToFloat(vertices[offset + 2]).toDouble()
            )
        }
        quadCenter.div(vertexCount.toDouble())
        val relativeQuadPosition = quadCenter.sub(localCenter)
        val spatialPhase = relativeQuadPosition.x * 0.73 +
            relativeQuadPosition.y * 1.17 +
            relativeQuadPosition.z * 1.91
        val pulse = 1.0 + abs(sin(animationTime * 0.12 + spatialPhase) + cos(animationTime * 0.12 + spatialPhase)) * 0.022
        val normal = shipToWorld.transformDirection(
            Vector3d(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble()),
            Vector3d()
        ).normalize()
        for (vertexIndex in 0 until vertices.size / 8) {
            val offset = vertexIndex * 8
            val worldPos = shipToWorld.transformPosition(
                Vector3d(
                    blockPos.x + java.lang.Float.intBitsToFloat(vertices[offset]).toDouble(),
                    blockPos.y + java.lang.Float.intBitsToFloat(vertices[offset + 1]).toDouble(),
                    blockPos.z + java.lang.Float.intBitsToFloat(vertices[offset + 2]).toDouble()
                ),
                Vector3d()
            ).add(Vector3d(normal).mul(0.005))
            .sub(center)
            .mul(pulse)
            .add(center)
            vertexConsumer
                .vertex(pose.pose(), worldPos.x.toFloat(), worldPos.y.toFloat(), worldPos.z.toFloat())
                .color(1f, 1f, 1f, 1f)
                .uv(
                    java.lang.Float.intBitsToFloat(vertices[offset + 4]),
                    java.lang.Float.intBitsToFloat(vertices[offset + 5])
                )
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(
                    pose.normal(),
                    normal.x.toFloat(),
                    normal.y.toFloat(),
                    normal.z.toFloat()
                )
                .endVertex()
        }
    }

    @OptIn(GameTickOnly::class)
    private fun updateMeteorStateWorld(ship: ClientShip, state: MeteorVfxState) {
        val center = ship.worldAABB.center(Vector3d())
        val velocity = ship.velocity
        val speed = velocity.length()
        val direction = if (speed > 1e-6) {
            velocity.normalize(Vector3d())
        } else if (state.hasLastPosition) {
            Vector3d(center).sub(state.lastPosition).normalize()
        } else {
            Vector3d()
        }
        if (direction.lengthSquared() > 1e-6) {
            if (state.hasDirection) {
                state.previousDirection.set(state.direction)
            } else {
                state.previousDirection.set(direction)
                state.hasDirection = true
            }
            state.direction.set(direction)
        }
        state.lastPosition.set(center)
        state.hasLastPosition = true
    }

    @OptIn(GameTickOnly::class)
    private fun  renderMeteorTrail(
        ms: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        ship: ClientShip,
        state: MeteorVfxState,
        center: Vector3d,
        partialTick: Float
    ) {
        if (ClockworkShaders.meteorTrail() == null || state.direction.lengthSquared() < 1e-6) return
        val shipExtents = ship.shipAABB?.extent(Vector3d()) ?: return
        val extents = ship.renderAABB.extent(Vector3d())
        val radius = ((extents.length()+shipExtents.length())/2.0).coerceAtLeast(0.5)
        val width = (radius * 1.75).toFloat()
        val length = (radius * 6.0).toFloat()
        val model = Minecraft.getInstance().modelManager.getModel(meteorTrailModelLocation)
        val vertexConsumer = bufferSource.getBuffer(ClockworkRenderTypes.METEOR_PLUME)
        val direction = if (state.hasDirection) {
            Vector3d(state.previousDirection).lerp(state.direction, partialTick.toDouble())
                .let { if (it.lengthSquared() > 1e-6) it.normalize() else Vector3d(state.direction) }
        } else {
            Vector3d(state.direction)
        }
        val rotation = Quaternionf().rotationTo(
            0f, 1f, 0f,
            -direction.x.toFloat(), -direction.y.toFloat(), -direction.z.toFloat()
        )

        ms.pushPose()
        ms.translate(center.x, center.y, center.z)
        ms.mulPose(rotation)
        ms.scale(width, length, width)
        ms.translate(-0.5, 0.0, -0.5)
        Minecraft.getInstance().blockRenderer.modelRenderer.renderModel(
            ms.last(),
            vertexConsumer,
            null,
            model,
            1f,
            1f,
            1f,
            0xF000F0,
            OverlayTexture.NO_OVERLAY
        )
        ms.popPose()
    }

    private fun renderHeatShield(
        ms: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        centerX: Double, centerY: Double, centerZ: Double,
        halfWidth: Double, halfHeight: Double, halfDepth: Double,
        Rot: Float
    ) {
        val model = Minecraft.getInstance().modelManager.getModel(heatShieldModelLocation)
        val modelWidth = 44.43892 / 16.0
        val scaleX = (halfWidth * 2.0 / modelWidth).toFloat()
        val scaleZ = (halfDepth * 2.0 / modelWidth).toFloat()
        val scaleY = (halfHeight * 2.0 / modelWidth).toFloat()

        ms.pushPose()
        ms.translate(centerX, centerY - halfHeight, centerZ)
        ms.scale(scaleX, scaleY, scaleZ)
        ms.rotateAround(Quaternionf().rotationY(Rot),0f,0f,0f)
        ms.translate(-0.5, 0.0, -0.5)
        val depthBuffer = bufferSource.getBuffer(ClockworkRenderTypes.METEOR_SHIELD_DEPTH)
        Minecraft.getInstance().blockRenderer.modelRenderer.renderModel(
            ms.last(),
            depthBuffer,
            null,
            model,
            1f,
            1f,
            1f,
            0xF000F0,
            OverlayTexture.NO_OVERLAY
        )
        bufferSource.endBatch(ClockworkRenderTypes.METEOR_SHIELD_DEPTH)
        val heatShieldRenderType = ClockworkRenderTypes.METEOR_HEAT_SHIELD
        Minecraft.getInstance().blockRenderer.modelRenderer.renderModel(
            ms.last(),
            bufferSource.getBuffer(heatShieldRenderType),
            null,
            model,
            1f,
            1f,
            1f,
            0xF000F0,
            OverlayTexture.NO_OVERLAY
        )
        bufferSource.endBatch(heatShieldRenderType)
        ms.popPose()
    }
}

class MeteorVfxState(var blocks: List<MeteorShipBlock> = emptyList()) {
    val lastPosition = Vector3d()
    val direction = Vector3d()
    val previousDirection = Vector3d()
    var hasLastPosition = false
    var hasDirection = false
}

data class MeteorShipBlock(val pos: BlockPos, val state: BlockState)
