package org.valkyrienskies.clockwork.client.render.scanner

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.core.api.ships.ClientShip

interface ScannerRenderer {
    //            ValkyrienCommonMixinConfigPlugin.getVSRenderer() == VSRenderer.VANILLA ?
    //                    new ShipScannerRenderer() : new WorldScannerRenderer();
    fun doRender(poseStack: PoseStack?)
    fun ping(ship: ClientShip?, pos: Vec3?, te: PhysicsInfuserBlockEntity)

    companion object {
        val INSTANCE: ScannerRenderer = ShipScannerRenderer()
    }
}