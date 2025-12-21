package org.valkyrienskies.clockwork.content.physicalities.extendon

import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.hoseport.HosePortBlockEntity
import org.valkyrienskies.clockwork.util.universal_joint.UniversalJointItem

class ExtendonHoseItem(properties: Properties) : UniversalJointItem<ExtendonBlockEntity>(properties) {

    override fun isJoint(be: BlockEntity): Boolean {
        return be is ExtendonBlockEntity || be is HosePortBlockEntity
    }
}