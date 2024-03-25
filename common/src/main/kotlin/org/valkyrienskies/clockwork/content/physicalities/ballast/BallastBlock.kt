package org.valkyrienskies.clockwork.content.physicalities.ballast

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.item.ItemHelper
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.platform.SharedValues

class BallastBlock(properties: Properties) : Block(properties), IBE<BallastBlockEntity> {

    override fun getBlockEntityClass(): Class<BallastBlockEntity> {
        return BallastBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out BallastBlockEntity> {
        return SharedValues.ballast.get()
    }

    override fun onRemove(state: BlockState, world: Level, pos: BlockPos?, newState: BlockState, pIsMoving: Boolean) {
        if (state.hasBlockEntity() && (state.block !== newState.block || !newState.hasBlockEntity())) {
            val be = world.getBlockEntity(pos) as? BallastBlockEntity ?: return
            val vaultBE = be
            ItemHelper.dropContents(world, pos, vaultBE.inventory as Storage<ItemVariant>?)
            world.removeBlockEntity(pos)
        }
    }
}