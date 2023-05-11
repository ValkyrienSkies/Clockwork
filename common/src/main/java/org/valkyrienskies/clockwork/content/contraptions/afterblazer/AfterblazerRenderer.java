package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionMatrices;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.valkyrienskies.clockwork.ClockWorkPartials;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import javax.annotation.Nullable;

public class AfterblazerRenderer extends SafeTileEntityRenderer<AfterblazerBlockEntity> {

    public AfterblazerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(AfterblazerBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource bufferSource,
                              int light, int overlay) {
        if (!(te instanceof AfterblazerBlockEntity)) {
            return;
        }

        LiquidFuelType fuelQuality = te.fuelLevel;

        Level level = te.getLevel();
        BlockState blockState = te.getBlockState();
        boolean isAboveYMax = te.isAboveYMax;
        float animation = te.headAnimation.getValue(partialTicks) * .175f;
        float horizontalAngle = AngleHelper.rad(te.headAngle.getValue(partialTicks));
        boolean canDrawFlame = fuelQuality.isAtLeast(LiquidFuelType.STALE);
        boolean drawGoggles = te.goggles;
        boolean drawHat = te.hat;
        int hashCode = te.hashCode();
        double plumeOffset = te.redstoneLevel/15f;
        renderShared(ms, null, bufferSource,
                level, blockState, fuelQuality, animation, horizontalAngle,
                canDrawFlame, drawGoggles, drawHat, hashCode, plumeOffset, isAboveYMax);
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                           ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
        BlockState state = context.state;

        Level level = context.world;
        float horizontalAngle = AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level)));
        boolean drawGoggles = context.tileData.contains("Goggles");
        boolean drawHat = conductor || context.tileData.contains("TrainHat");
        int hashCode = context.hashCode();


        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource,
                level, state, LiquidFuelType.PLAIN, 0, horizontalAngle,
                false, drawGoggles, drawHat, hashCode, 1f, false);
    }

    private static void renderShared(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource,
                                     Level level, BlockState blockState, LiquidFuelType fuelQuality, float animation, float horizontalAngle,
                                     boolean canDrawFlame, boolean drawGoggles, boolean drawHat, int hashCode, double plumeOffset, boolean isAboveYMax) {

        boolean blockAbove = animation > 0.125f;
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + (hashCode % 13) * 16f;
        float offsetMult = fuelQuality.isAtLeast(LiquidFuelType.STALE) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
        float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
        float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
        float headY = offset - (animation * .75f);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
        final Direction ogfacing = blockState
                .getValue(BlockStateProperties.FACING);

        final Direction facing = Direction.NORTH;
        ms.pushPose();

        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Quaternion.fromXYZ(0.0f, (float) Math.toRadians(-180.0), 0.0f));
        switch (ogfacing) {
            case SOUTH -> ms.mulPose(Vector3f.YP.rotationDegrees(180));
            case WEST -> ms.mulPose(Vector3f.YP.rotationDegrees(90));
            case NORTH -> ms.mulPose(Vector3f.YP.rotationDegrees(0));
            case EAST -> ms.mulPose(Vector3f.YP.rotationDegrees(270));
            case UP -> ms.mulPose(Vector3f.XP.rotationDegrees(90));
            case DOWN -> ms.mulPose(Vector3f.XN.rotationDegrees(90));
        }
        ms.translate(-0.5, -0.5, -0.5);

        PartialModel blazeModel;
        if (fuelQuality.equals(LiquidFuelType.GOURMET) || isAboveYMax) {
            blazeModel = ClockWorkPartials.BLAZE_INFURIATED;
        } else if (fuelQuality.isAtLeast(LiquidFuelType.SWEET)) {
            blazeModel = AllBlockPartials.BLAZE_SUPER_ACTIVE;
        } else if (fuelQuality.isAtLeast(LiquidFuelType.PLAIN)) {
            blazeModel = AllBlockPartials.BLAZE_ACTIVE;
        } else {
            blazeModel = AllBlockPartials.BLAZE_INERT;
        }

        SuperByteBuffer blazeBuffer = CachedBufferer.partial(blazeModel, blockState);
        if (modelTransform != null)
            blazeBuffer.transform(modelTransform);
        blazeBuffer.translate(0, headY, 0);
        draw(blazeBuffer, horizontalAngle, ms, solid);

        if (drawGoggles) {
            PartialModel gogglesModel = blazeModel == AllBlockPartials.BLAZE_INERT
                    ? AllBlockPartials.BLAZE_GOGGLES_SMALL : AllBlockPartials.BLAZE_GOGGLES;

            SuperByteBuffer gogglesBuffer = CachedBufferer.partial(gogglesModel, blockState);
            if (modelTransform != null)
                gogglesBuffer.transform(modelTransform);
            gogglesBuffer.translate(0, headY + 8 / 16f, 0);
            draw(gogglesBuffer, horizontalAngle, ms, solid);
        }

        if (drawHat) {
            SuperByteBuffer hatBuffer = CachedBufferer.partial(AllBlockPartials.TRAIN_HAT, blockState);
            if (modelTransform != null)
                hatBuffer.transform(modelTransform);
            hatBuffer.translate(0, headY, 0);
            if (blazeModel == AllBlockPartials.BLAZE_INERT) {
                hatBuffer.translateY(0.5f)
                        .centre()
                        .scale(0.75f)
                        .unCentre();
            } else {
                hatBuffer.translateY(0.75f);
            }
            hatBuffer
                    .rotateCentered(Direction.UP, horizontalAngle + Mth.PI)
                    .translate(0.5f, 0, 0.5f)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, solid);
        }

        if (fuelQuality.isAtLeast(LiquidFuelType.STALE)) {
            PartialModel plumeModel;
            if (fuelQuality.equals(LiquidFuelType.GOURMET) || isAboveYMax) {
                plumeModel = ClockWorkPartials.PLUME_INFURIATED;
            } else if (fuelQuality.equals(LiquidFuelType.SWEET)) {
                plumeModel = ClockWorkPartials.PLUME_FUMING;
            } else {
                plumeModel = ClockWorkPartials.PLUME_ANGRY;
            }

            SuperByteBuffer plumeBuffer = CachedBufferer.partial(plumeModel, blockState);
            if (modelTransform != null)
                plumeBuffer.transform(modelTransform);
//            if (blockState.getValue(BlockStateProperties.FACING) == Direction.UP) {
//                plumeBuffer.rotateCentered(Direction.NORTH, 90);
//                plumeBuffer.translate(0, -plumeOffset, 0);
//            } else if (blockState.getValue(BlockStateProperties.FACING) == Direction.DOWN) {
//                plumeBuffer.rotateCentered(Direction.NORTH, 270);
//                plumeBuffer.translate(0, plumeOffset, 0);
//            } else if (blockState.getValue(BlockStateProperties.FACING) == Direction.NORTH) {
//                plumeBuffer.translate(0, 0, plumeOffset);
//            } else if (blockState.getValue(BlockStateProperties.FACING) == Direction.SOUTH) {
//                plumeBuffer.rotateCentered(Direction.UP, 180);
//                plumeBuffer.translate(0, 0, -plumeOffset);
//            } else if (blockState.getValue(BlockStateProperties.FACING) == Direction.EAST) {
//                plumeBuffer.rotateCentered(Direction.UP, 90);
//                plumeBuffer.translate(plumeOffset, 0, 0);
//            } else if (blockState.getValue(BlockStateProperties.FACING) == Direction.WEST) {
//                plumeBuffer.rotateCentered(Direction.UP, 270);
//                plumeBuffer.translate(-plumeOffset, 0, 0);
//            }
            plumeBuffer.translate(0, 0, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, solid);
        }

        ms.popPose();
    }

    private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, VertexConsumer vc) {
        buffer.rotateCentered(Direction.UP, horizontalAngle)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, vc);
    }

}
