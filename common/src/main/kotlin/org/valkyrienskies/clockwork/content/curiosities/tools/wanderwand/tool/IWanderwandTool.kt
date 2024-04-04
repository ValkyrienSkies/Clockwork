package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.world.phys.Vec3

interface IWanderwandTool {
    fun init()

    /**
     * Will run when a player leftClicks, should be overridden for tools which utilise left click
     */
    fun handleLeftClick(): Boolean

    /**
     * Will run when a player rightClicks, should be overridden for tools which utilise right click
     */
    fun handleRightClick(crouching: Boolean): Boolean

    fun handleRightClick(): Boolean {
        return handleRightClick(false)
    }

    /**
     * Will run when a player uses their mouse wheel, should be overridden for tools which utilise mouse wheel
     */
    fun handleMouseWheel(delta: Double): Boolean

    /**
     * Override to render extra stuff on the specific tool selected
     */
    fun renderTool(ms: PoseStack?, buffer: SuperRenderTypeBuffer?, camera: Vec3?)

    /**
     * Override to render extra stuff at hotbar slot when specific tool selected
     */
    fun renderOverlay(poseStack: PoseStack, partialTicks: Float, width: Int, height: Int)
}