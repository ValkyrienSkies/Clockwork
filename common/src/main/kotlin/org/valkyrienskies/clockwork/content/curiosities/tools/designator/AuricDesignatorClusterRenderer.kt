package org.valkyrienskies.clockwork.content.curiosities.tools.designator


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
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.SelectedAreaToolkit
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft

class AuricDesignatorClusterRenderer {
    private val bbOutlineSlotAD = Any()
    private var storedClusters = HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>>()
    private var hoveredCluster: Set<AABBic>? = HashSet()

    fun discard() {
        storedClusters = HashMap()
    }

    fun renderDesignator(
        level: ClientLevel?,
        minecraft: Minecraft,
        poseStack: PoseStack,
    ) {
        if (level != null) {
            for (player in level.players()) {
                if (player.mainHandItem.`is`(ClockworkItems.AURIC_DESIGNATOR.get())) {

                    // other players
                    val adi = player.mainHandItem.item as AuricDesignatorItem
                    val clusters: Set<Set<AABBic>> = adi.selectedArea.selectionClusters
                    for (cluster in clusters) {
                        if (!storedClusters.containsKey(cluster)) {
                            storedClusters[cluster] =
                                Pair.of(SelectedAreaToolkit.blocksFromCluster(cluster), clusterID + clusterIncrement)
                            clusterIncrement++
                        }
                    }
                    while (adi.selectedArea.toStopRendering.isNotEmpty()) {
                        ClockworkMod.AURIC_OUTLINER.remove(adi.selectedArea.toStopRendering[0])
                        storedClusters.remove(adi.selectedArea.toStopRendering.removeAt(0))
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
                        var hoveredFace: BlockPos? = null
                        if (mc.hitResult != null && mc.hitResult!!.type == HitResult.Type.BLOCK) {
                            hovered = (mc.hitResult as BlockHitResult?)!!.blockPos
                            hoveredFace = (mc.hitResult as BlockHitResult?)!!.blockPos.relative(
                                (mc.hitResult as BlockHitResult?)!!.direction
                            )
                        }
                        var hoveredBlockPos: Vector3ic = Vector3i()
                        if (hovered != null) {
                            hoveredBlockPos = hovered.toJOML()
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
                            if (adi.firstPos == null) {
                                val vec = Vector3d(hoveredBlockPos).toMinecraft()
                                if (vec != localPlayer.eyePosition) {
                                    ClockworkMod.AURIC_OUTLINER.chaseAABB(adi, AABB(hoveredBlockPos.toBlockPos()))
                                    ClockworkMod.AURIC_OUTLINER.edit(adi).ifPresent { outline ->
                                        outline.colored(
                                            HOVERPURPLE
                                        ).withFaceTexture(AllSpecialTextures.SELECTION)
                                    }
                                } else {
                                    ClockworkMod.AURIC_OUTLINER.remove(adi)
                                }
                            }
                        }
                        if (adi.firstPos != null) {
                            val vec = Vector3d(hoveredBlockPos).toMinecraft()
                            if (vec != localPlayer.eyePosition) {
                                ClockworkMod.AURIC_OUTLINER.chaseAABB(
                                    bbOutlineSlotAD,
                                    AABB(adi.firstPos!!.toBlockPos(), hoveredBlockPos.toBlockPos()).expandTowards(
                                        1.0,
                                        1.0,
                                        1.0
                                    )
                                )
                                ClockworkMod.AURIC_OUTLINER.edit(bbOutlineSlotAD).ifPresent { outline ->
                                    outline.colored(
                                        HOVERPURPLE
                                    ).withFaceTexture(AllSpecialTextures.SELECTION)
                                }
                            } else {
                                ClockworkMod.AURIC_OUTLINER.chaseAABB(
                                    bbOutlineSlotAD,
                                    AABB(adi.firstPos!!.toBlockPos(), adi.firstPos!!.toBlockPos())
                                )
                            }
                            // render selection box
                        } else {
                            ClockworkMod.AURIC_OUTLINER.remove(bbOutlineSlotAD)
                        }
                    }

                    for (key in storedClusters.keys) {
                        ClockworkMod.AURIC_OUTLINER.showCluster(
                            storedClusters[key]!!.right, storedClusters[key]!!.left
                        )
                        ClockworkMod.AURIC_OUTLINER.edit(storedClusters[key]!!.right).ifPresent { outline ->
                            outline.colored(
                                IDLEPURPLE
                            )
                        }
                        if (key == hoveredCluster) {
                            ClockworkMod.AURIC_OUTLINER.edit(storedClusters[key]!!.right).ifPresent { outline ->
                                outline.colored(
                                    HOVERPURPLE
                                )
                            }
                        }
                    }


                    // initialSelectionBox.tick();
                    // selectionBox.tick();
                    poseStack.popPose()
                }
            }
        }
    }


    companion object {
        var INSTANCE = AuricDesignatorClusterRenderer()
        private val HOVERPURPLE = Color(203, 195, 227)
        private val IDLEPURPLE = Color(221, 160, 221)
        private const val clusterID = "clusterID_"
        private var clusterIncrement = 0
    }
}