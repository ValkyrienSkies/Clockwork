package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.schematics.client.tools.ToolType
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toJOMLF

abstract class GravitronToolBase : IGravitronTool {
    protected var gravitronHandler: GravitronHandler? = null
    var clickedPos: BlockPos? = null
    var clickedLocation: Vec3? = null


    fun updateTargetPos() {
        val player = Minecraft.getInstance().player

        val trace = RaycastHelper.rayTraceRange(
            player!!.level, player, 15.0
        )
        if (trace == null || trace.type != HitResult.Type.BLOCK) {
            return
        }

        clickedPos = trace.blockPos.immutable()

        clickedLocation = clickedPos!!.toDoubles().add(0.5,0.5,0.5)
    }

    override fun handleRightClick(): Boolean {
        return false
    }

    override fun handleMouseWheel(delta: Double): Boolean {
        return false;
    }

    override fun init() {
        gravitronHandler = SharedValues.gravitronHandler
    }

    override fun renderTool(ms: PoseStack?, buffer: SuperRenderTypeBuffer?, camera: Vec3?) {
    }

    override fun renderOverlay(poseStack: PoseStack, partialTicks: Float, width: Int, height: Int) {
    }

    companion object {
        @JvmField
        var GRAB: Byte = 1

        @JvmField
        var ASSEMBLE: Byte = 2

        @JvmField
        var GRABSSEMBLE: Byte = 3
    }
}