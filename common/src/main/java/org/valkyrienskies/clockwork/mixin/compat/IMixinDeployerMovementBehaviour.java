package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.contraptions.components.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DeployerMovementBehaviour.class)
public interface IMixinDeployerMovementBehaviour {
    @Invoker("tryGrabbingItem")
    void invokeTryGrabbingItem(MovementContext movementContext);
}
