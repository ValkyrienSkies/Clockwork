package org.valkyrienskies.clockwork.client.render.scanner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.compat.VSRenderer;
import org.valkyrienskies.mod.mixin.ValkyrienCommonMixinConfigPlugin;

import javax.annotation.Nullable;

public interface ScannerRenderer {
    ScannerRenderer INSTANCE =
            ValkyrienCommonMixinConfigPlugin.getVSRenderer() == VSRenderer.VANILLA ?
                    new ShipScannerRenderer() : new WorldScannerRenderer();

    void doRender(final PoseStack poseStack);

    void ping(@Nullable final ClientShip ship, final Vec3 pos, PhysicsInfuserBlockEntity te);
}
