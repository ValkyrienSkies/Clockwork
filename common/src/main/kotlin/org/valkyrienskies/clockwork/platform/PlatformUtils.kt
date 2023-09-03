package org.valkyrienskies.clockwork.platform

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.InputKey
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour

object PlatformUtils {
    @ExpectPlatform
    fun getReachDistance(player: Player): Double {
        throw AssertionError()
    }

    @ExpectPlatform
    fun createExtraDataSpawnPacket(entity: Entity): Packet<*> {
        throw AssertionError()
    }

    @ExpectPlatform
    fun setAboveGroundTicks(player: ServerPlayer, ticks: Int) {
        throw AssertionError()
    }

    //    @ExpectPlatform
    //    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
    //                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {throw new AssertionError();}
    //    @ExpectPlatform
    //    public static boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate, BalloonerBlockEntity blockEntity) {throw new AssertionError();}
    @ExpectPlatform
    fun maxBalloonRange(): Int {
        throw AssertionError()
    }

    @ExpectPlatform
    fun useBallooner(
        state: BlockState?, world: Level, pos: BlockPos, player: Player, hand: InteractionHand,
        blockRayTraceResult: BlockHitResult
    ) {
        throw AssertionError()
    }

    //    @ExpectPlatform
    //    public static Class<?> getCombustionEngineTileEntityClass() {throw new AssertionError();}
    //
    //    @ExpectPlatform
    //    public static BlockEntityType<? extends CombustionEngineBlockEntity> getCombustionEngineTileEntityType() {throw new AssertionError();}
    @ExpectPlatform
    fun isCannon(stack: ItemStack): Boolean {
        throw AssertionError()
    }

    @ExpectPlatform
    fun cwFluidTank(
        type: BehaviourType<CWFluidTankBehaviour>,
        te: SmartBlockEntity,
        tanks: Int,
        tankCapacity: Long,
        enforceVariety: Boolean
    ): CWFluidTankBehaviour {
        throw AssertionError()
    }

    @ExpectPlatform
    fun getBakedModel(itemModel: CustomRenderedItemModel): BakedModel {
        throw AssertionError()
    }

    @ExpectPlatform
    fun isModLoaded(modId: String): Boolean {
        throw AssertionError()
    }

    @ExpectPlatform
    fun sequencedSeatKeysUpdated(level: ServerLevel, pos: BlockPos, keys: Set<InputKey>) {
        throw AssertionError()
    }

    @ExpectPlatform
    fun getLiquidFuelTypeFromItemStack(stack: ItemStack): LiquidFuelType {
        throw AssertionError()
    }
}