package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.mod.common.util.toJOMLD

object ActiveChutes {
    val actives: HashMap<BlockPos, DeliveryChuteBlockEntity> = HashMap()
    val unloaded: HashMap<BlockPos, DeliveryChuteBlockEntity> = HashMap()

    fun addChute(pos: BlockPos, chute: DeliveryChuteBlockEntity) {
        actives[pos] = chute
    }

    fun removeChute(pos: BlockPos) {
        actives.remove(pos)
        unloaded.remove(pos)
    }

    private fun unloadChute(pos: BlockPos) {
        unloaded[pos] = actives[pos]!!
        actives.remove(pos)
    }

    private fun loadChute(pos: BlockPos) {
        actives[pos] = unloaded[pos]!!
        unloaded.remove(pos)
    }

    fun getChutes(): HashMap<BlockPos, DeliveryChuteBlockEntity> {
        return actives
    }

    fun getNearestChute(pos: BlockPos, maxDistance: Double): BlockPos? {
        var closest: BlockPos? = null
        var closestDistance: Double = Double.MAX_VALUE
        for (chute in actives.keys) {
            val realPos = actives[chute]!!.getRealPos()
            val realBlockPos = BlockPos(realPos.x().toInt(), realPos.y().toInt(), realPos.z().toInt())
            if (realBlockPos.closerThan(pos, maxDistance)) {
                if (realPos.distance(pos.toJOMLD()) < closestDistance) {
                    closest = chute
                    closestDistance = chute.toJOMLD().distance(pos.toJOMLD())
                }
            }
        }
        return closest
    }

    fun getNearestChuteWithId(pos: BlockPos, maxDistance: Double, id: Int): BlockPos? {
        var closest: BlockPos? = null
        var closestDistance: Double = Double.MAX_VALUE
        for (chute in actives.keys) {
            val realPos = actives[chute]!!.getRealPos()
            val realBlockPos = BlockPos(realPos.x().toInt(), realPos.y().toInt(), realPos.z().toInt())
            if (realBlockPos.closerThan(pos, maxDistance)) {
                if (realPos.distance(pos.toJOMLD()) < closestDistance) {
                    if (actives[chute]!!.id == id) {
                        closest = chute
                        closestDistance = chute.toJOMLD().distance(pos.toJOMLD())
                    }
                }
            }
        }
        return closest
    }

    fun hasChute(pos: BlockPos): Boolean {
        return actives.containsKey(pos) || unloaded.containsKey(pos)
    }

    fun getChuteRealPos(pos: BlockPos): Vector3dc? {
        if (actives.containsKey(pos)) {
            return actives[pos]!!.getRealPos()
        } else if (unloaded.containsKey(pos)) {
            return unloaded[pos]!!.getRealPos()
        }
        return null
    }

    fun tick(level: ServerLevel) {
        val toUnload: ArrayList<BlockPos> = ArrayList()
        for (pos in actives.keys) {
            if (!level.isLoaded(pos)) {
                toUnload.add(pos)
            }
        }
        for (pos in toUnload) {
            unloadChute(pos)
        }

        val toLoad: ArrayList<BlockPos> = ArrayList()
        for (pos in unloaded.keys) {
            if (level.isLoaded(pos)) {
                toLoad.add(pos)
            }
        }
        for (pos in toLoad) {
            loadChute(pos)
        }
    }
}