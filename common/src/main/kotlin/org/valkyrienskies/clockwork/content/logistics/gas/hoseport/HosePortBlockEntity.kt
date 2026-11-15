package org.valkyrienskies.clockwork.content.logistics.gas.hoseport

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.universal_joint.BlockAnchorLinks
import org.valkyrienskies.clockwork.util.universal_joint.GasLinkBlockEntity

class HosePortBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : GasLinkBlockEntity(type, pos, state) {
    override val linkKind = BlockAnchorLinks.Kind.HOSE
}
