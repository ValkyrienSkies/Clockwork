package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import com.simibubi.create.foundation.utility.RaycastHelper
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*

@Environment(EnvType.CLIENT)
class WanderwandEffectRenderer {

    val client: Minecraft = Minecraft.getInstance()

    val attachments: HashSet<Pair<Vector3d, Vector3d>> = HashSet()

    var isWelding = false
    var weldingShip: ShipId? = null
    val weld: HashSet<BlockPos> = HashSet()
    var selectionPos: BlockPos? = null
    var selectionDir: Direction? = null
    var lastTargetPos: Vec3 = Vec3.ZERO

    val clusters = HashSet<Set<BlockPos>>()

    val shouldUpdateClusters = true

    //clusters and attachments
    fun clientTick(clientLevel: ClientLevel) {
        var count = 0
        for (cluster in clusters) {
            ClockworkModClient.WANDER_OUTLINER.showCluster("cluster$count", cluster)
            count++
        }
        count = 0
        for (attachment in attachments) {
            ClockworkModClient.WANDER_OUTLINER.showLine("attachment$count", attachment.first.toMinecraft(), attachment.second.toMinecraft())
            count++
        }
    }

    //binds (when i add them) and weld holograms
    fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, partialTicks: Float) {
        val level = client.level ?: return
        val player = client.player ?: return
        if (isWelding && weldingShip != null) {
            ms.pushPose()


            val range = (client.gameMode?.getPickRange()?.toDouble()?.plus(1)) ?: 5.0
            val traceOrigin = RaycastHelper.getTraceOrigin(player)
            val traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin)

            val clipContext: ClipContext = ClipContext(
                traceOrigin,
                traceTarget,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            )

            val hit = level.clipIncludeShips(clipContext, true, weldingShip!!)

            val rawTargetPos = hit.blockPos.toJOMLD().add(0.5, 0.5, 0.5).add(hit.direction.normal.toJOMLD().mul(0.5)).toMinecraft()
            val targetPos = lastTargetPos.lerp(rawTargetPos, partialTicks.toDouble() * 2.0)
            val targetRotation = Quaternion.ONE
            targetRotation.mul(hit.direction.rotation)
            targetRotation.mul(selectionDir!!.rotation)
            targetRotation.normalize()
            ms.mulPose(targetRotation)
            for (blockPos in weld) {
                val blockState = level.getBlockState(blockPos)
                if (blockState.isAir) {
                    continue
                }
                val offset = blockPos.toJOMLD().sub(selectionPos!!.toJOMLD()).toMinecraft()
                val targetRenderPos = targetPos.add(offset)
                ms.translate(
                    targetRenderPos.x - camera.x,
                    targetRenderPos.y - camera.y,
                    targetRenderPos.z - camera.z
                )
                val blockRenderDispatcher = client.blockRenderer
                blockRenderDispatcher.modelRenderer.tesselateBlock(
                    level, blockRenderDispatcher.getBlockModel(blockState), blockState, blockPos, ms,
                    buffer.getBuffer(
                        ItemBlockRenderTypes.getMovingBlockRenderType(blockState)
                    ), true, Random(), blockState.getSeed(blockPos), OverlayTexture.NO_OVERLAY
                )
                ms.translate(
                    -(targetRenderPos.x - camera.x),
                    -(targetRenderPos.y - camera.y),
                    -(targetRenderPos.z - camera.z)
                )
            }
            lastTargetPos = targetPos

            ms.popPose()
        }
    }

    private fun updateClusters(tag: CompoundTag) {
        clusters.clear()
        val deserialized = WanderwandItem.readBlockPosSetFromNBT(tag)
        clusters.addAll(WanderwandItem.findIsolatedComponents(deserialized))
    }

    private fun startWelding(selPos: BlockPos, selDir: Direction, blocks: CompoundTag, shipId: ShipId) {
        isWelding = true
        selectionPos = selPos
        selectionDir = selDir
        weld.clear()
        val deserialized = WanderwandItem.readBlockPosSetFromNBT(blocks)
        weld.addAll(deserialized)
        weldingShip = shipId
    }

    private fun stopWelding() {
        isWelding = false
        selectionPos = null
        selectionDir = null
        weld.clear()
        weldingShip = null
    }

    fun handlePacket(packet: WanderwandRenderUpdatePacket) {
        if (packet.tool == ToolType.SELECT || packet.tool == ToolType.DESELECT) {
            updateClusters(packet.blocks!!)
        } else if (packet.tool == ToolType.WELD) {
            if (packet.blocks != null && packet.onOff) {
                startWelding(packet.selectionPos, packet.selectionDir!!, packet.blocks, packet.shipId!!)
            } else {
                stopWelding()
            }
        } else if (packet.tool == ToolType.ATTACH) {
            if (packet.onOff) {
                attachments.add(Pair(packet.selectionPos.toJOMLD(), packet.secondPos!!.toJOMLD()))
            } else {
                attachments.removeIf { it.first == packet.selectionPos.toJOMLD() }
            }
        }
    }
}