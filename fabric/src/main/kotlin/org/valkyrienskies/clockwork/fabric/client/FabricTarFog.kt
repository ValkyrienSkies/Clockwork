package org.valkyrienskies.clockwork.fabric.client

import io.github.fabricators_of_create.porting_lib.event.client.FogEvents
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.fabric.FabricClockworkFluids

object FabricTarFog {
    @JvmStatic
    fun register() {
        FogEvents.SET_COLOR.register { event, _ ->
            if (isSubmerged(event.camera)) {
                event.red = 0f
                event.green = 0f
                event.blue = 0f
            }
        }
        FogEvents.RENDER_FOG.register { _, _, camera, _, _, _, _, _, fog ->
            if (isSubmerged(camera)) {
                fog.scaleFarPlaneDistance(1f / 8f)
                true
            } else {
                false
            }
        }
    }

    private fun isSubmerged(camera: Camera): Boolean {
        val level = Minecraft.getInstance().level ?: return false
        val pos = camera.blockPosition
        val fluid = level.getFluidState(pos)
        return FabricClockworkFluids.TAR.get().isSame(fluid.type) &&
            camera.position.y < pos.y + fluid.getHeight(level, pos)
    }
}
