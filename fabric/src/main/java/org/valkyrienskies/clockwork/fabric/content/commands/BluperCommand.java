package org.valkyrienskies.clockwork.fabric.content.commands;

import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueEntity;

public class BluperCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("bluper")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
                                    BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");

                                    ServerLevel world = ctx.getSource()
                                            .getLevel();

                                    BluperGlueEntity entity = new BluperGlueEntity(world, BluperGlueEntity.span(from, to));
                                    entity.playPlaceSound();
                                    world.addFreshEntity(entity);
                                    return 1;
                                })));

    }
}
