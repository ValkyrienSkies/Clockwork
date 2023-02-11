package org.valkyrienskies.clockwork.forge.mixin.compat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerBlock;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.valkyrienskies.clockwork.forge.content.contraptions.sticker.StickerMovementBehaviour;

import static com.simibubi.create.AllMovementBehaviours.movementBehaviour;

@Mixin(AllBlocks.class)
public class MixinAllBlocks {
    private static Logger LOGGER = LogManager.getLogger("Clockwork.MixinAllBlocks");
    @Mutable
    @Shadow
    @Final
    public static BlockEntry<StickerBlock> STICKER;

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;item()Lcom/tterrag/registrate/builders/ItemBuilder;"),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lcom/simibubi/create/AllBlocks;RADIAL_CHASSIS:Lcom/tterrag/registrate/util/entry/BlockEntry;"),
                    to = @At(value = "FIELD", target = "Lcom/simibubi/create/AllBlocks;STICKER:Lcom/tterrag/registrate/util/entry/BlockEntry;")
            )
    )
    private static ItemBuilder setSTICKER(BlockBuilder instance) {
        breakpointHelper(instance);
        instance.onRegister(movementBehaviour(new StickerMovementBehaviour()));
        return instance.item();
    }

    @Unique
    private static void breakpointHelper(BlockBuilder instance) {
        String name = instance.getName();
        if (LOGGER == null)
            LOGGER = LogManager.getLogger("Clockwork.MixinAllBlocks");

        if (LOGGER != null) {
            LOGGER.warn(name);
            //LOGGER.warn(STICKER.getId().toString());
        }
    }
}
