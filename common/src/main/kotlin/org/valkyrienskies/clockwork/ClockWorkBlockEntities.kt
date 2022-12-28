package org.valkyrienskies.clockwork

import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.blockentity.EngineBlockEntity
import org.valkyrienskies.clockwork.blockentity.ShipHelmBlockEntity
import org.valkyrienskies.clockwork.registry.DeferredRegister
import org.valkyrienskies.clockwork.registry.RegistrySupplier

@Suppress("unused")
object ClockWorkBlockEntities {
    private val BLOCKENTITIES = DeferredRegister.create(ClockWorkMod.MOD_ID, Registry.BLOCK_ENTITY_TYPE_REGISTRY)

    val SHIP_HELM = setOf(
        ClockWorkBlocks.OAK_SHIP_HELM,
        ClockWorkBlocks.SPRUCE_SHIP_HELM,
        ClockWorkBlocks.BIRCH_SHIP_HELM,
        ClockWorkBlocks.JUNGLE_SHIP_HELM,
        ClockWorkBlocks.ACACIA_SHIP_HELM,
        ClockWorkBlocks.DARK_OAK_SHIP_HELM,
        ClockWorkBlocks.CRIMSON_SHIP_HELM,
        ClockWorkBlocks.WARPED_SHIP_HELM
    ) withBE ::ShipHelmBlockEntity byName "ship_helm"

    val ENGINE = ClockWorkBlocks.ENGINE withBE ::EngineBlockEntity byName "engine"

    fun register() {
        ClockWorkBlockEntities.BLOCKENTITIES.applyAll()
    }

    private infix fun <T : BlockEntity> Set<RegistrySupplier<out Block>>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        Pair(this, blockEntity)

    private infix fun <T : BlockEntity> RegistrySupplier<out Block>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        Pair(setOf(this), blockEntity)

    private infix fun <T : BlockEntity> Block.withBE(blockEntity: (BlockPos, BlockState) -> T) = Pair(this, blockEntity)
    private infix fun <T : BlockEntity> Pair<Set<RegistrySupplier<out Block>>, (BlockPos, BlockState) -> T>.byName(name: String): RegistrySupplier<BlockEntityType<T>> =
        ClockWorkBlockEntities.BLOCKENTITIES.register(name) {
            val type = Util.fetchChoiceType(References.BLOCK_ENTITY, name)

            BlockEntityType.Builder.of(
                this.second,
                *this.first.map { it.get() }.toTypedArray()
            ).build(type)
        }
}
