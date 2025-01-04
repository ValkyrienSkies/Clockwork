package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class SugarRocketBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?,
                             state: BlockState?
) : SmartBlockEntity(type, pos, state) {
    var burnProgress = 0
    var burnTime = 20
    var burnPower = 1
    var hasNextBlock = false
    var isBurning = false

    var sugarCooldown = 0

    val clientBurnProgress = LerpedFloat.linear().chase(0.0, 0.05, LerpedFloat.Chaser.LINEAR)

    init {
        clientBurnProgress.setValue(0.0)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
    }

    fun addSugar(amount: Int) {
        burnTime += (20 * amount)
        burnPower += amount
        sugarCooldown = 4
        sendData()
    }

    override fun tick() {
        super.tick()
        if (sugarCooldown > 0) {
            sugarCooldown--
        }
        if (level == null) return
        if (!level!!.isClientSide) {
            if (isBurning) {
                burn()
            }
        } else {
            clientBurnProgress.tickChaser()
        }
    }

    fun burn() {
        burnProgress++
        if (burnProgress >= burnTime) {
            isBurning = false
            if (hasNextBlock) {
                val nextBlock = level!!.getBlockEntity(worldPosition.relative(blockState.getValue(DirectionalBlock.FACING)))
                if (nextBlock is SugarRocketBlockEntity) {
                    nextBlock.burnTime = burnTime
                    nextBlock.burnPower = burnPower
                    nextBlock.isBurning = true
                    level!!.destroyBlock(worldPosition, false)
                }
            }
        }
        sendData()
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean("burning", isBurning)
        tag.putInt("burnProgress", burnProgress)
        tag.putInt("burnTime", burnTime)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        isBurning = tag.getBoolean("burning")
        burnProgress = tag.getInt("burnProgress")
        burnTime = tag.getInt("burnTime")

        if (clientPacket && isBurning) {
            clientBurnProgress.updateChaseTarget((burnProgress.toDouble() / burnTime.toDouble()).toFloat())
        }
    }
}