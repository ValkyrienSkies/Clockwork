package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

interface IUniversalJoint {
    var connectedJoint: IUniversalJoint?
    var pos: BlockPos

    fun connectTo(other: IUniversalJoint) {
        if (connectedJoint != null) return
        connectedJoint = other

        other.connectTo(this)

    }

    fun tryConnect(level: Level, blockPos: BlockPos): Boolean {
        val other = level.getBlockEntity(blockPos) ?: return false
        if (!isThisJoint(other)) return false

        val oJ = other as IUniversalJoint
        if (oJ.connectedJoint != null) return false


        connectTo(other)
        return true
    }

    fun disconnect() {
        if (connectedJoint == null) return
        val temp = connectedJoint
        connectedJoint = null
        temp!!.disconnect()
    }


    fun isThisJoint(be: BlockEntity): Boolean {
        return (be is IUniversalJoint)
    }

}