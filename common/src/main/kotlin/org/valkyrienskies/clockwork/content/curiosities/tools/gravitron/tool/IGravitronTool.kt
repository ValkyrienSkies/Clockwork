package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import com.mojang.blaze3d.vertex.PoseStack
import net.createmod.catnip.render.SuperRenderTypeBuffer
import net.minecraft.world.phys.Vec3

interface IGravitronTool {
    fun init()

    /**
     * Will run when a player rightClicks, should be overriden for tools which utilises right click
     *
     * @param isRegular is the gravitron tool a regular one, or a creative one
     */
    fun handleRightClick(isRegular: Boolean): Boolean

    /**
     * Will run when a player leftClicks, should be overriden for tools which utilises right click
     *
     * @param isRegular is the gravitron tool a regular one, or a creative one
     */
    fun handleLeftClick(regular: Boolean): Boolean

    /**
     * Will run when a player uses their mouse wheel, should be overriden for tools which utilises mouse wheel
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
