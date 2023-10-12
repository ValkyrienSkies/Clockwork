package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSounds

class GravitronRenderHandler : ShootableGadgetRenderHandler() {
    override fun transformTool(ms: PoseStack, flip: Float, equipProgress: Float, recoil: Float, pt: Float) {
        ms.translate((flip * -0.1f).toDouble(), 0.1, -0.4)
        ms.mulPose(Vector3f.YP.rotationDegrees(flip * 5.0f))
    }

    override fun playSound(hand: InteractionHand, position: Vec3) {
        val pitch = if (hand == InteractionHand.MAIN_HAND) 0.1f else 0.9f
        val mc = Minecraft.getInstance()
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.play(mc.level, mc.player, position, 0.1f, pitch)
    }

    override fun appliesTo(stack: ItemStack): Boolean {
        return false
    }

    override fun transformHand(ms: PoseStack, flip: Float, equipProgress: Float, recoil: Float, pt: Float) {}
}
