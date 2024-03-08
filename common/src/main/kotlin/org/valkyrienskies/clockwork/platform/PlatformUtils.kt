package org.valkyrienskies.clockwork.platform

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.tterrag.registrate.fabric.EnvExecutor
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.InputKey
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour
import java.util.function.Supplier

object PlatformUtils {
    @ExpectPlatform
    @JvmStatic
    fun getEnvExecutor(toRun: Supplier<Runnable>) {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun getReachDistance(player: Player): Double {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun createExtraDataSpawnPacket(entity: Entity): Packet<*> {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun setAboveGroundTicks(player: ServerPlayer, ticks: Int) {
        throw AssertionError()
    }

    //    @ExpectPlatform
    //    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
    //                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {throw new AssertionError();}
    //    @ExpectPlatform
    //    public static boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate, BalloonerBlockEntity blockEntity) {throw new AssertionError();}
    @ExpectPlatform
    @JvmStatic
    fun maxBalloonRange(): Int {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun useBallooner(
        state: BlockState?,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
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
    @JvmStatic
    fun isCannon(stack: ItemStack): Boolean {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun cwFluidTank(
        type: BehaviourType<CWFluidTankBehaviour>,
        te: SmartBlockEntity,
        tanks: Int,
        tankCapacity: Long,
        enforceVariety: Boolean
    ): CWFluidTankBehaviour {
        throw AssertionError()
    }

//    @ExpectPlatform
//    @JvmStatic
//    fun getBakedModel(itemModel: CustomRenderedItemModel): BakedModel {
//        throw AssertionError()
//    }

    @ExpectPlatform
    @JvmStatic
    fun isModLoaded(modId: String): Boolean {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun sequencedSeatKeysUpdated(level: ServerLevel, pos: BlockPos, keys: Set<InputKey>) {
        throw AssertionError()
    }


    @ExpectPlatform
    @JvmStatic
    fun getExtraData(be: SmartBlockEntity): CompoundTag {
        throw AssertionError()
    }
}
