package org.valkyrienskies.clockwork.content.curiosities.tools.bluper

import com.simibubi.create.AllKeys
import com.simibubi.create.AllSpecialTextures
import com.simibubi.create.foundation.outliner.Outliner
import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.Pair
import com.simibubi.create.foundation.utility.RaycastHelper
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod.OUTLINER
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.SelectedAreaToolkit.Companion.blocksFromCluster
import org.valkyrienskies.clockwork.util.AreaData
import org.valkyrienskies.clockwork.util.AreaData.Companion.of
import java.util.*

class BluperGlueSelectionHandler {
    private val outlineSlot = Any()

    private var selectedPos: BlockPos? = null
    private var selectedFace: Direction? = null
    private var storedClusters = HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>>()
    private val range = 10

    private val clusterID = "clusterID_"
    private var clusterIncrement = 0
    private val BLUE = 0x6886c5

    fun discard() {
        val player = Minecraft.getInstance().player
        val data = of(player).get()
        data.setFirstPos(Optional.empty())
        data.setSecondPos(Optional.empty())
        storedClusters = HashMap()
    }

    fun tick() {
        if (!isActive()) return

        val player = Minecraft.getInstance().player ?: return

        renderStoredClusters()
        if (ClockworkItems.GRAVITRON.isIn(Minecraft.getInstance().player!!.mainHandItem)) {
            return
        }

        val data = of(player).get()
        if (AllKeys.ACTIVATE_TOOL.isPressed) {
            val pt = AnimationTickHolder.getPartialTicks()
            val targetVec = player.getEyePosition(pt).add(player.lookAngle.scale(range.toDouble()))
            selectedPos = BlockPos.containing(targetVec)
        } else {
            val trace = RaycastHelper.rayTraceRange(player.level(), player, 25.0)
            selectedPos = if (trace != null && trace.type == HitResult.Type.BLOCK) {
                trace.blockPos
            } else null
        }

        val clusters: Set<Set<AABBic>> = HashSet(data.getArea().selectionClusters)

        for (cluster in clusters) {
            if (!storedClusters.containsKey(cluster)) {
                storedClusters[cluster] = Pair.of(blocksFromCluster(cluster), clusterID + clusterIncrement++)
            }
        }

        selectedFace = null
        if (data.getSecondPos().isPresent) {
            val bb = AABB(data.getFirstPos().get(), data.getSecondPos().get()).expandTowards(1.0, 1.0, 1.0).inflate(.45)
            val projectedView = Minecraft.getInstance().gameRenderer.mainCamera.position
            val inside = bb.contains(projectedView)
            val result = RaycastHelper.rayTraceUntil(
                player, 70.0
            ) { pos: BlockPos? ->
                inside xor bb.contains(
                    VecHelper.getCenterOf(pos)
                )
            }
            selectedFace = if (result.missed()) null else if (inside) result.facing.opposite else result.facing
        }

        val currentSelectionBox = getCurrentSelectionBox(data)
        if (currentSelectionBox != null) outliner().chaseAABB(outlineSlot, currentSelectionBox)
            .colored(BLUE)
            .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
            .lineWidth(1 / 16f)
            .highlightFace(selectedFace)
    }

    private fun getCurrentSelectionBox(data: AreaData): AABB? {
        if (data.getSecondPos().isEmpty) {
            if (data.getFirstPos().isEmpty) return if (selectedPos == null) null else AABB(selectedPos)
            return if (selectedPos == null) AABB(data.getFirstPos().get()) else AABB(
                data.getFirstPos().get(),
                selectedPos
            ).expandTowards(1.0, 1.0, 1.0)
        }
        return AABB(data.getFirstPos().get(), data.getSecondPos().get()).expandTowards(1.0, 1.0, 1.0)
    }

    private fun renderStoredClusters() {
        for (key in storedClusters.keys) {
            val storedCluster =
                storedClusters[key]!!
            outliner().showCluster(storedCluster.second, storedCluster.first).colored(BLUE)
        }
    }

    private fun isActive(): Boolean {
        return isPresent() && (ClockworkItems.BLUPERGLUE.isIn(Minecraft.getInstance().player!!.mainHandItem) || ClockworkItems.GRAVITRON.isIn(
            Minecraft.getInstance().player!!.mainHandItem
        ))
    }

    private fun isPresent(): Boolean {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().screen == null
    }

    private fun outliner(): Outliner {
        return OUTLINER
    }
}