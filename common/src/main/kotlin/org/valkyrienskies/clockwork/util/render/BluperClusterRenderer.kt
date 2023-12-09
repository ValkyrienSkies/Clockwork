package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllSpecialTextures
import com.simibubi.create.foundation.utility.Color
import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
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
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class BluperClusterRenderer {

    private val bbOutlineSlotAD = Any()
    private val storedClusters = HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>>()
    private var hoveredCluster: Set<AABBic>? = HashSet()

    fun renderDesignator(
        level: ClientLevel?,
        minecraft: Minecraft,
        poseStack: PoseStack,
    ) {
        if (level != null) {
            for (player in level.players()) {
                if (player.mainHandItem.`is`(ClockworkItems.BLUPERGLUE.get())) {
                    // other players
                    val area = AreaData.of(player).get()
                    val clusters: Set<Set<AABBic>> = area.area.selectionClusters
                    for (cluster in clusters) {
                        if (!storedClusters.containsKey(cluster)) {
                            storedClusters[cluster] =
                                Pair.of(SelectedAreaToolkit.blocksFromCluster(cluster), clusterID + clusterIncrement)
                            clusterIncrement++
                        }
                    }
                    while (area.area.toStopRendering.isNotEmpty()) {
                        ClockworkMod.OUTLINER.remove(area.area.toStopRendering[0])
                        storedClusters.remove(area.area.toStopRendering.removeAt(0))
                    }
                    if (minecraft.getCameraEntity() == null) {
                        return
                    }
                    poseStack.pushPose()
                    // local player
                    if (player.isLocalPlayer) {
                        val mc = Minecraft.getInstance()
                        val localPlayer = mc.player ?: return
                        var hovered: BlockPos? = null
                        if (mc.hitResult != null && mc.hitResult!!.type == HitResult.Type.BLOCK) {
                            hovered = (mc.hitResult as BlockHitResult?)!!.blockPos
                            (mc.hitResult as BlockHitResult?)!!.blockPos.relative((mc.hitResult as BlockHitResult?)!!.direction)
                        }
                        var hoveredBlockPos: Optional<Vector3ic> = Optional.empty()
                        if (hovered != null) {
                            hoveredBlockPos = Optional.of(hovered.toJOML())
                        }

                        //  find existing hovered cluster if existing
                        var foundCluster = false
                        for (cluster in clusters) {
                            val range = 10.0
                            val tempOrigin: Vector3dc = RaycastHelper.getTraceOrigin(localPlayer).toJOML()
                            val tempTarget: Vector3dc = RaycastHelper.getTraceTarget(
                                localPlayer,
                                range,
                                RaycastHelper.getTraceOrigin(localPlayer)
                            ).toJOML()
                            val traceOrigin: Vector3fc =
                                Vector3f(tempOrigin.x().toFloat(), tempOrigin.y().toFloat(), tempOrigin.z().toFloat())
                            val traceTarget: Vector3fc =
                                Vector3f(tempTarget.x().toFloat(), tempTarget.y().toFloat(), tempTarget.z().toFloat())
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
                            if (area.firstPos.isEmpty && hoveredBlockPos.isPresent) {
                                val vec = Vector3d(hoveredBlockPos.get()).toMinecraft()
                                if (vec != localPlayer.eyePosition) {
                                    ClockworkMod.OUTLINER.chaseAABB(area, AABB(hoveredBlockPos.get().toBlockPos()))
                                    ClockworkMod.OUTLINER.edit(area).ifPresent { outline ->
                                        outline.colored(HOVERPURPLE).withFaceTexture(AllSpecialTextures.SELECTION)
                                    }
                                } else {
                                    ClockworkMod.OUTLINER.remove(area)
                                }
                            }
                        }
                        if (area.firstPos.isPresent && hoveredBlockPos.isPresent) {
                            val vec = Vector3d(hoveredBlockPos.get()).toMinecraft()
                            if (vec != localPlayer.eyePosition) {

                                ClockworkMod.OUTLINER.chaseAABB(
                                    bbOutlineSlotAD,
                                    AABB(area.firstPos.get().toBlockPos(), hoveredBlockPos.get().toBlockPos()).expandTowards(
                                        1.0,
                                        1.0,
                                        1.0
                                    )
                                )
                                ClockworkMod.OUTLINER.edit(bbOutlineSlotAD).ifPresent { outline ->
                                    outline.colored(HOVERPURPLE).withFaceTexture(AllSpecialTextures.SELECTION)
                                }
                            } else if (area.firstPos.isPresent) {
                                ClockworkMod.OUTLINER.chaseAABB(
                                    bbOutlineSlotAD,
                                    AABB(area.firstPos.get().toBlockPos(), area.firstPos.get().toBlockPos())
                                )
                            } else {
                                ClockworkMod.OUTLINER.remove(bbOutlineSlotAD)
                            }
                            // render selection box
                        } else {
                            ClockworkMod.OUTLINER.remove(bbOutlineSlotAD)
                        }
                    }
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

                    poseStack.popPose()
                }
            }
        }
    }

    companion object {
        var INSTANCE = BluperClusterRenderer()
        private val HOVERPURPLE = Color(203, 195, 227)
        private val IDLEPURPLE = Color(221, 160, 221)
        private const val clusterID = "clusterID_"
        private var clusterIncrement = 0
    }
}
