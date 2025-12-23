package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.utility.RaycastHelper
import net.createmod.catnip.render.SuperRenderTypeBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.IGravitronTool
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandler
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.mod.common.util.toDoubles

abstract class  WanderwandToolBase : IWanderwandTool {
    protected var wanderwandHandler: WanderwandHandler? = null
    var clickedPos: BlockPos? = null
    var clickedLocation: Vec3? = null
    var clickedDirection: Direction? = null
    var lastClickedPos: BlockPos? = null
    var lastClickedLocation: Vec3? = null
    var lastClickedDirection: Direction? = null


    /**
     * This function will store the block the player looks
     * at within 15 blocks to be accessed by the Wanderwand's other functions
     */
    open fun updateTargetPos() {
        lastClickedPos = clickedPos
        lastClickedLocation = clickedLocation
        lastClickedDirection = clickedDirection

        val player = Minecraft.getInstance().player

        val trace = RaycastHelper.rayTraceRange(
            player!!.level(), player, 15.0
        ) ?: return
        if (trace == null || trace.type != HitResult.Type.BLOCK) {
            return
        }

        clickedPos = trace.blockPos.immutable()

        clickedLocation = trace.location
        clickedDirection = trace.direction
    }

    override fun handleLeftClick(): Boolean {
        return false
    }

    override fun handleRightClick(crouching: Boolean): Boolean {
        return false
    }

    override fun handleMouseWheel(delta: Double): Boolean {
        return false;
    }

    override fun init() {
        wanderwandHandler = SharedValues.wanderwandHandler
    }

    override fun renderTool(ms: GuiGraphics?, buffer: SuperRenderTypeBuffer?, camera: Vec3?) {
    }

    override fun renderOverlay(poseStack: GuiGraphics, partialTicks: Float, width: Int, height: Int) {
    }

    companion object {
        @JvmField
        var SELECT: Byte = 1

        @JvmField
        var DESELECT: Byte = 2

        @JvmField
        var ATTACH: Byte = 3

        @JvmField
        var ROPE: Byte = 4

        @JvmField
        var WELD: Byte = 5
    }
}
