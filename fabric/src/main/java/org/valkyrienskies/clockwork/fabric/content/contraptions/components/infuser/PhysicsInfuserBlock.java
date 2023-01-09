package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import java.util.function.Consumer;

import com.simibubi.create.foundation.block.ITE;
import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.fabric.AllClockworkTileEntities;

public class PhysicsInfuserBlock extends Block implements ITE<PhysicsInfuserBlockEntity> {

    private static final VoxelShape SHAPE = makeShape();

    public PhysicsInfuserBlock(Properties properties) {
        super(properties);
    }

    public static boolean isInfuser(BlockState state) {
        return state.getBlock() instanceof PhysicsInfuserBlock;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock()) {
            return;
        }
        if (moved) {
            return;
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide) {
                withTileEntityDo(worldIn, pos, te -> {if (te.isAssembled) te.startDisassembly();});
                return InteractionResult.SUCCESS;
            }

            withTileEntityDo(worldIn, pos, te -> {
                if (te.isAssembled && !te.disassembling && !te.assembling) {
                    te.startDisassembly();
                    return;
                } else if (!te.isAssembled && !te.disassembling && !te.assembling) {
                    te.startAssembly();
                    return;
                }

            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }



    //Voxelshape Hell
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.1859375, 0.25, 0.25, 0.1859375, 0.75, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.1859375, 0.75, 0.75, 0.1859375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.8140625, 0.75, 0.75, 0.8140625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8140625, 0.25, 0.25, 0.8140625, 0.75, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.140625, 0.25, 0.25, 0.140625, 0.75, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.140625, 0.75, 0.75, 0.140625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.859375, 0.25, 0.25, 0.859375, 0.75, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.859375, 0.75, 0.75, 0.859375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.03125, 0.71875, 0.3125, 0.09375, 0.96875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.734375, 0.3125, 0.25, 0.953125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.21875, 0.71875, 0.3125, 0.28125, 0.96875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.734375, 0.75, 0.6875, 0.953125, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.71875, 0.3125, 0.78125, 0.96875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.90625, 0.71875, 0.3125, 0.96875, 0.96875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.71875, 0.21875, 0.6875, 0.96875, 0.28125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.71875, 0.03125, 0.6875, 0.96875, 0.09375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.71875, 0.71875, 0.6875, 0.96875, 0.78125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.71875, 0.90625, 0.6875, 0.96875, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.75, 0.734375, 0.3125, 0.9375, 0.953125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.734375, 0.0625, 0.6875, 0.953125, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.03125, 0.90625, 0.6875, 0.28125, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.03125, 0.71875, 0.6875, 0.28125, 0.78125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.03125, 0.3125, 0.78125, 0.28125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.90625, 0.03125, 0.3125, 0.96875, 0.28125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.03125, 0.21875, 0.6875, 0.28125, 0.28125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.03125, 0.03125, 0.6875, 0.28125, 0.09375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.21875, 0.03125, 0.3125, 0.28125, 0.28125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.03125, 0.03125, 0.3125, 0.09375, 0.28125, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.046875, 0.3125, 0.25, 0.265625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.046875, 0.0625, 0.6875, 0.265625, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.75, 0.046875, 0.3125, 0.9375, 0.265625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.046875, 0.75, 0.6875, 0.265625, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0, 0, 1, 0.3125, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.3125, 0.3125, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.6875, 0, 0.3125, 1, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0.6875, 0, 1, 1, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0.6875, 0.6875, 1, 1, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0, 0.6875, 1, 0.3125, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.6875, 0.6875, 0.3125, 1, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.6875, 0.3125, 0.3125, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.703125, 0.65625, 0.296875, 0.984375, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.703125, 0.3125, 0.296875, 0.984375, 0.34375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.3125, 0.015625, 0.296875, 0.34375, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.3125, 0.703125, 0.296875, 0.34375, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.3125, 0.015625, 0.984375, 0.34375, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.703125, 0.65625, 0.984375, 0.984375, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.703125, 0.3125, 0.984375, 0.984375, 0.34375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.65625, 0.703125, 0.703125, 0.6875, 0.984375, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.703125, 0.703125, 0.34375, 0.984375, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.703125, 0.015625, 0.34375, 0.984375, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.65625, 0.703125, 0.015625, 0.6875, 0.984375, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.015625, 0.65625, 0.984375, 0.296875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.015625, 0.3125, 0.984375, 0.296875, 0.34375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.015625, 0.015625, 0.34375, 0.296875, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.65625, 0.015625, 0.015625, 0.6875, 0.296875, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.015625, 0.3125, 0.296875, 0.296875, 0.34375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.015625, 0.65625, 0.296875, 0.296875, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.015625, 0.703125, 0.34375, 0.296875, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.65625, 0.015625, 0.703125, 0.6875, 0.296875, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.3125, 0.703125, 0.984375, 0.34375, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.65625, 0.703125, 0.984375, 0.6875, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.65625, 0.015625, 0.984375, 0.6875, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.65625, 0.015625, 0.296875, 0.6875, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.015625, 0.65625, 0.703125, 0.296875, 0.6875, 0.984375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.765625, 0.28125, 0.03125, 0.96875, 0.65625, 0.234375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.03125, 0.28125, 0.03125, 0.234375, 0.65625, 0.234375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.03125, 0.28125, 0.765625, 0.234375, 0.65625, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.765625, 0.28125, 0.765625, 0.96875, 0.65625, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.09375, 0.25, 0.71875, 0.21875, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.46875, 0.8125, 0.28125, 0.53125, 0.875, 0.71875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.8125, 0.46875, 0.71875, 0.875, 0.53125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.984375, 0.0625, 0.1875, 1.046875, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.09375, 0.96875, 0.203125, 0.1875, 1.03125, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.203125, 0.96875, 0.09375, 0.296875, 1.03125, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.984375, 0.0625, 0.9375, 1.046875, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.96875, 0.09375, 0.796875, 1.03125, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.96875, 0.203125, 0.90625, 1.03125, 0.296875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.984375, 0.8125, 0.9375, 1.046875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.703125, 0.96875, 0.8125, 0.796875, 1.03125, 0.90625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.96875, 0.703125, 0.90625, 1.03125, 0.796875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.203125, 0.96875, 0.8125, 0.296875, 1.03125, 0.90625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.984375, 0.8125, 0.1875, 1.046875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.09375, 0.96875, 0.703125, 0.1875, 1.03125, 0.796875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.125, 0.3125, 0.125, 0.25, 0.6875, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.75, 0.3125, 0.125, 0.875, 0.6875, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.75, 0.3125, 0.75, 0.875, 0.6875, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.125, 0.3125, 0.75, 0.25, 0.6875, 0.875), BooleanOp.OR);

        return shape;
    }

    @Override
    public Class<PhysicsInfuserBlockEntity> getTileEntityClass() {
        return PhysicsInfuserBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhysicsInfuserBlockEntity> getTileEntityType() {
        return AllClockworkTileEntities.PHYSICS_INFUSER.get();
    }

}
