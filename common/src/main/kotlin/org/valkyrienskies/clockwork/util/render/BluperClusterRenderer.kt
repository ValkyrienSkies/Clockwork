package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllSpecialTextures
import com.simibubi.create.foundation.utility.Color
import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.apache.commons.lang3.tuple.Pair
import org.joml.*
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.joml.primitives.Intersectionf
import org.joml.primitives.LineSegmentf
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.mod.common.assembly.SeamlessChunksManager
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.mixin.client.MixinMinecraft
import org.valkyrienskies.mod.mixin.feature.seamless_copy.MixinClientPacketListener
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class BluperClusterRenderer {

    private val bbOutlineSlotAD = Any()
    private val outlineSlot = Any()
    private val storedClusters = HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>>()
    private var hoveredCluster: Set<AABBic>? = HashSet()
    private var selectedPos = Optional.empty<Vector3ic>()


    fun renderDesignator(level: ClientLevel?, minecraft: Minecraft, poseStack: PoseStack) {
        if (level == null) return

        for (player in level.players()) {
            if (player.mainHandItem.`is`(ClockworkItems.BLUPERGLUE.get())) {
                renderForPlayer(player, minecraft, poseStack)
            }
        }
    }

    private fun renderForPlayer(player: Player, minecraft: Minecraft, poseStack: PoseStack) {
        val area = AreaData.of(player).get()
        val clusters: Set<Set<AABBic>> = area.area.selectionClusters

        for (cluster in clusters) {
            if (!storedClusters.containsKey(cluster)) {
                storedClusters[cluster] =
                    Pair.of(SelectedAreaToolkit.blocksFromCluster(cluster), clusterID + clusterIncrement)
                clusterIncrement++
            }
        }

        removeStoppedRendering(area)

        if (minecraft.getCameraEntity() == null) return

        poseStack.pushPose()

        if (player.isLocalPlayer) {
            handleLocalPlayer(minecraft, area, clusters)
        }

        renderStoredClusters()

        poseStack.popPose()
    }

    private fun handleLocalPlayer(mc: Minecraft, area: AreaData, clusters: Set<Set<AABBic>>) {
        val localPlayer = mc.player ?: return
        var hovered: BlockPos? = null

        if (mc.hitResult != null && mc.hitResult!!.type == HitResult.Type.BLOCK) {
            hovered = (mc.hitResult as BlockHitResult?)!!.blockPos
            (mc.hitResult as BlockHitResult?)!!.blockPos.relative((mc.hitResult as BlockHitResult?)!!.direction)
        }

        if (hovered != null) {
            selectedPos = Optional.of(hovered.toJOML())
        }

        var foundCluster = false
        for (cluster in clusters) {
            val range = 10.0
            val tempOrigin: Vector3dc = RaycastHelper.getTraceOrigin(localPlayer).toJOML()
            val tempTarget: Vector3dc = RaycastHelper.getTraceTarget(localPlayer, range, RaycastHelper.getTraceOrigin(localPlayer)).toJOML()
            val traceOrigin: Vector3fc = Vector3f(tempOrigin.x().toFloat(), tempOrigin.y().toFloat(), tempOrigin.z().toFloat())
            val traceTarget: Vector3fc = Vector3f(tempTarget.x().toFloat(), tempTarget.y().toFloat(), tempTarget.z().toFloat())
            val cast = LineSegmentf(traceOrigin, traceTarget)

            for (box in cluster) {
                val intersection = Intersectionf.intersectLineSegmentAab(cast, AABBi(box), Vector2f())
                if (intersection != Intersectionf.OUTSIDE) {
                    hoveredCluster = cluster
                    foundCluster = true
                    break
                }
            }

            if (foundCluster) break
        }

        if (!foundCluster) {
            hoveredCluster = null
        }

        if (hoveredCluster == null) {
            // render initial selection box
            if (area.firstPos.isEmpty && selectedPos.isPresent) {
                val vec = Vector3d(selectedPos.get()).toMinecraft()
                if (vec != localPlayer.eyePosition) {
                    ClockworkMod.OUTLINER.chaseAABB(area, getCurrentSelectionBox(area.firstPos, area.secondPos))
                        .colored(HOVERPURPLE)
                        .withFaceTexture(AllSpecialTextures.SELECTION)
                } else {
                    ClockworkMod.OUTLINER.remove(area)
                }
            }
        }

        if (area.firstPos.isPresent && selectedPos.isPresent) {
            val vec = Vector3d(selectedPos.get()).toMinecraft()
            if (vec != localPlayer.eyePosition) {
                ClockworkMod.OUTLINER.chaseAABB(outlineSlot, getCurrentSelectionBox(area.firstPos, area.secondPos))
                    .colored(HOVERPURPLE)
                    .withFaceTexture(AllSpecialTextures.SELECTION)
            } else if (area.firstPos.isPresent) {
                ClockworkMod.OUTLINER.chaseAABB(
                    bbOutlineSlotAD,
                    getCurrentSelectionBox(area.firstPos, area.secondPos)
                )
            } else {
                ClockworkMod.OUTLINER.remove(bbOutlineSlotAD)
            }
        } else {
            ClockworkMod.OUTLINER.remove(bbOutlineSlotAD)
        }
    }

    private fun removeStoppedRendering(area: AreaData) {
        while (area.area.toStopRendering.isNotEmpty()) {
            val removed = area.area.toStopRendering.removeAt(0)
            ClockworkMod.OUTLINER.remove(removed)
            storedClusters.remove(removed)
        }
    }

    private fun renderStoredClusters() {
        for (key in storedClusters.keys) {
            ClockworkMod.OUTLINER.showCluster(
                storedClusters[key]!!.right, storedClusters[key]!!.left
            )
            ClockworkMod.OUTLINER.edit(storedClusters[key]!!.right).ifPresent { outline ->
                outline.colored(IDLEPURPLE)
            }
            if (key == hoveredCluster) {
                ClockworkMod.OUTLINER.edit(storedClusters[key]!!.right).ifPresent { outline ->
                    outline.colored(HOVERPURPLE)
                }
            }
        }
    }

    private fun getCurrentSelectionBox(firstPos : Optional<Vector3ic>, secondPos : Optional<Vector3ic>): AABB? {
        if (secondPos.isEmpty) {
            if (firstPos.isEmpty) return if (selectedPos.isEmpty) null else AABB(selectedPos.get().toBlockPos())
            return if (selectedPos.isEmpty) AABB(firstPos.get().toBlockPos()) else AABB(firstPos.get().toBlockPos(), selectedPos.get().toBlockPos()).expandTowards(1.0, 1.0, 1.0)
        }
        return AABB(firstPos.get().toBlockPos(), secondPos.get().toBlockPos()).expandTowards(1.0, 1.0, 1.0)
    }

    companion object {
        var INSTANCE = BluperClusterRenderer()
        private val HOVERPURPLE = Color(203, 195, 227)
        private val IDLEPURPLE = Color(221, 160, 221)
        private const val clusterID = "clusterID_"
        private var clusterIncrement = 0
    }
}
