package org.valkyrienskies.clockwork.mixin.content.schematics;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableSchematicBehaviour;

import java.util.Map;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class MixinThinkableBehaviours {
    @Shadow @Final private Map<BehaviourType<?>, BlockEntityBehaviour> behaviours;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clockwork$schematicConnections(CallbackInfo ci) {
        if ((Object) this instanceof IThinkable) {
            behaviours.put(ThinkableSchematicBehaviour.TYPE,
                    new ThinkableSchematicBehaviour((SmartBlockEntity) (Object) this));
        }
    }
}
