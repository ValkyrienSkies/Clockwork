package org.valkyrienskies.clockwork.content.contraptions.phys.infuser;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem;

public class PhysicsInfuserBlock extends Block implements IBE<PhysicsInfuserBlockEntity> {
    private static final VoxelShape SHAPE = makeShape();

    public PhysicsInfuserBlock(Properties properties) {
        super(properties);

    }

    public static boolean isInfuser(BlockState state) {
        return state.getBlock() instanceof PhysicsInfuserBlock;
    }

    public static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();

        shape = Shapes.join(shape, Block.box(0, 11, 0, 5, 16, 5), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(0.5, 0.5, 0.5, 15.5, 15.5, 15.5), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(0, 0, 0, 5, 5, 5), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(11, 0, 0, 16, 5, 5), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(11, 11, 0, 16, 16, 5), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(11, 11, 11, 16, 16, 16), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(11, 0, 11, 16, 5, 16), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(0, 0, 11, 5, 5, 16), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(0, 11, 11, 5, 16, 16), BooleanOp.OR);

        return shape.optimize();
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock()) {
            return;
        }
        if (moved) {
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (worldIn.isClientSide) {
            return InteractionResult.PASS;
        }

        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(worldIn, pos, te -> {
                    if (te.isAssembled && !te.assembling && !te.disassembling) te.startDisassembly();
                });
                withBlockEntityDo(worldIn, pos, te -> {
                    if (!te.isAssembled && !te.assembling && !te.disassembling) te.startAssembly();
                });
                return InteractionResult.SUCCESS;
            }

            withBlockEntityDo(worldIn, pos, te -> {
                if (te.isAssembled && !te.assembling && !te.disassembling && !te.onCooldown) {
                    te.startDisassembly();
                } else if (!te.isAssembled && te.assembling && !te.disassembling && !te.onCooldown) {
                    te.skipAssembly();
                } else if (!te.isAssembled && !te.assembling && !te.disassembling && !te.onCooldown) {
                    te.startAssembly();
                }
            });
            return InteractionResult.SUCCESS;
        } else if (player.getItemInHand(handIn).getItem() instanceof AreaDesignatorItem) {
            if (worldIn.getBlockEntity(pos) != null) {
                if (worldIn.getBlockEntity(pos) instanceof PhysicsInfuserBlockEntity) {
                    PhysicsInfuserBlockEntity te = (PhysicsInfuserBlockEntity) worldIn.getBlockEntity(pos);
                    if (te.inventory.get(0).isEmpty()) {
                        te.inventory.set(0, player.getItemInHand(handIn).copy());
                        player.getItemInHand(handIn).shrink(1);
                        return InteractionResult.SUCCESS;
                    } else {
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    //Voxelshape Hell
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public Class<PhysicsInfuserBlockEntity> getBlockEntityClass() {
        return PhysicsInfuserBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhysicsInfuserBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.PHYSICS_INFUSER.get();
    }

}
