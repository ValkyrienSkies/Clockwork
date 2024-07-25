package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import com.simibubi.create.foundation.utility.RaycastHelper
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*

@Environment(EnvType.CLIENT)
class WanderwandEffectRenderer {

    val client: Minecraft

    init {
        client = Minecraft.getInstance()
    }

    val attachments: HashSet<Pair<Vector3d, Vector3d>> = HashSet()

    var isWelding = false
    val weld: HashSet<BlockPos> = HashSet()

    val clusters = HashSet<Set<BlockPos>>()

    val shouldUpdateClusters = true

    //clusters and attachments
    fun clientTick(clientLevel: ClientLevel) {
        var count = 0
        for (cluster in clusters) {
            ClockworkModClient.WANDER_OUTLINER.showCluster("cluster$count", cluster)
            count++
        }
    }

    //binds (when i add them) and weld holograms
    fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, partialTicks: Float) {
        val level = client.level ?: return
        val player = client.player ?: return
        if (isWelding) {
            ms.pushPose()


            val range = (client.gameMode?.getPickRange()!!.toDouble() + 1) ?: 5.0
            val traceOrigin = RaycastHelper.getTraceOrigin(player)
            val traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin)


            //level.clipIncludeShips()

            for (blockPos in weld) {
                val x = blockPos.x - player.x
                val y = blockPos.y - player.y
                val z = blockPos.z - player.z
                ms.translate(x, y, z)
                val blockState = level.getBlockState(blockPos)
                if (blockState.isAir) {
                    ms.translate(-x, -y, -z)
                    continue
                }
                val blockRenderDispatcher = client.blockRenderer
                blockRenderDispatcher.modelRenderer.tesselateBlock(
                    level, blockRenderDispatcher.getBlockModel(blockState), blockState, blockPos, ms,
                    buffer.getBuffer(
                        ItemBlockRenderTypes.getMovingBlockRenderType(blockState)
                    ), true, Random(), blockState.getSeed(blockPos), OverlayTexture.NO_OVERLAY
                )
                ms.translate(-x, -y, -z)
            }

            ms.popPose()
        }
    }

    fun updateClusters(tag: CompoundTag) {
        clusters.clear()
        val deserialized = WanderwandItem.readBlockPosSetFromNBT(tag)
        clusters.addAll(WanderwandItem.findIsolatedComponents(deserialized))
    }

    fun startWelding(blocks: Set<BlockPos>) {
        isWelding = true
        weld.clear()
        weld.addAll(blocks)
    }

    fun stopWelding() {
        isWelding = false
        weld.clear()
    }

}