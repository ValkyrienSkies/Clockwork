package org.valkyrienskies.clockwork.client.render.scanner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.core.api.ships.ClientShip;

import javax.annotation.Nullable;

public interface ScannerRenderer {
    ScannerRenderer INSTANCE = new ShipScannerRenderer();
//            ValkyrienCommonMixinConfigPlugin.getVSRenderer() == VSRenderer.VANILLA ?
//                    new ShipScannerRenderer() : new WorldScannerRenderer();

    void doRender(final PoseStack poseStack);

    void ping(@Nullable final ClientShip ship, final Vec3 pos, PhysicsInfuserBlockEntity te);
}
