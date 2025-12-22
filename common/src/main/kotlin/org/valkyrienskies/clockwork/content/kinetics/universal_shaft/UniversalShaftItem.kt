package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.util.universal_joint.UniversalJointItem
import org.valkyrienskies.mod.common.toWorldCoordinates

class UniversalShaftItem(properties: Properties) : UniversalJointItem<UniversalShaftBlockEntity>(properties) {

    override fun isJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }
}