package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllSpecialTextures
import com.simibubi.create.foundation.utility.RaycastHelper
import net.createmod.catnip.outliner.Outliner
import net.createmod.catnip.render.SuperRenderTypeBuffer
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
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.SelectTool
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import kotlin.math.max
import kotlin.math.min

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

    val clusters = ArrayList<Set<BlockPos>>()

    //clusters and attachments
    fun clientTick(clientLevel: ClientLevel) {
        var count = 0
        if (SharedValues.wanderwandHandler.findWandInHand(client.player) != null) {
            for (cluster in clusters) {
                Outliner.getInstance().showCluster("cluster$count", cluster).colored(0xFF55FF).lineWidth(0.1f).withFaceTextures(AllSpecialTextures.THIN_CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                count++
            }
        }
        count = 0
        for (attachment in attachments) {
            Outliner.getInstance().showLine("attachment$count", attachment.first.toMinecraft(), attachment.second.toMinecraft()).lineWidth(0.1f).colored(0xFF55FF)
            count++
        }
    }

    //binds (when i add them) and weld holograms
    fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, partialTicks: Float) {
        val level = client.level ?: return
        val player = client.player ?: return

        if (isWelding && weldingShip != null) {
            ms.pushPose()
            println("WELDING FUCKHEAD")


            val range = (client.gameMode?.getPickRange()?.toDouble()?.plus(1)) ?: 5.0
            val traceOrigin = player.eyePosition
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
            val targetRotation = Quaternionf()
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
                    ), true, level.random, blockState.getSeed(blockPos), OverlayTexture.NO_OVERLAY
                )
                ms.translate(
                    -(targetRenderPos.x - camera.x),
                    -(targetRenderPos.y - camera.y),
                    -(targetRenderPos.z - camera.z)
                )
            }
            lastTargetPos = targetPos

            ms.popPose()
        } else {
            val tool = SharedValues.wanderwandHandler.currentTool
            if (tool != ToolType.SELECT) return
            val selectionTool = (tool.tool) as SelectTool

            val player = Minecraft.getInstance().player ?: return
            val itemStack = player.mainHandItem

            if (itemStack.item !is WanderwandItem) { Outliner.getInstance().remove("wandSelectionBox"); return }
            val wand = itemStack.item as WanderwandItem

            val trace = RaycastHelper.rayTraceRange(player.level(), player, 15.0) ?: return
            if (trace.type != HitResult.Type.BLOCK) return


            val aabb: AABB
            if (selectionTool.clickedPos == null) aabb = AABB(trace.blockPos)
            else {
                val minX = min(selectionTool.clickedPos!!.x, trace.blockPos.x).toDouble()
                val minY = min(selectionTool.clickedPos!!.y, trace.blockPos.y).toDouble()
                val minZ = min(selectionTool.clickedPos!!.z, trace.blockPos.z).toDouble()
                val maxX = max(selectionTool.clickedPos!!.x, trace.blockPos.x).toDouble() + 1.0
                val maxY = max(selectionTool.clickedPos!!.y, trace.blockPos.y).toDouble() + 1.0
                val maxZ = max(selectionTool.clickedPos!!.z, trace.blockPos.z).toDouble() + 1.0
                aabb = AABB(minX,minY,minZ,maxX,maxY,maxZ)
            }

            println("${selectionTool.clickedPos} ${trace.blockPos}")

            Outliner.getInstance().showAABB("wandSelectionBox", aabb).colored(0xFF5FFF).lineWidth(0.05f)
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
            if (packet.blocks != null) updateClusters(packet.blocks)
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
