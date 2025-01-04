package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.client.particle.LargeSmokeParticle
import net.minecraft.client.particle.Particle
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.forces.SugarRocketController
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.GameTickForceApplier
import org.valkyrienskies.mod.common.util.toJOMLD

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

    fun ignite(time: Int = burnTime, power: Int = burnPower) {
        isBurning = true
        burnTime = time
        burnPower = power
        val serverLevel = level as ServerLevel
        val ship = serverLevel.getShipObjectManagingPos(worldPosition)
        if (ship != null) {
            SugarRocketController.getOrCreate(ship).addRocket(worldPosition, burnPower.toDouble() * 10000.0, blockState.getValue(DirectionalBlock.FACING))
        }
        sendData()
    }

    fun burn() {
        burnProgress++
        val serverLevel = level as ServerLevel
        val ship = serverLevel.getShipObjectManagingPos(worldPosition)
        val realWorldPosition = serverLevel.toWorldCoordinates(worldPosition)
        var realDirection = blockState.getValue(DirectionalBlock.FACING).opposite.normal.toJOMLD()
        if (ship != null) {
            realDirection = ship.transform.rotation.transform(realDirection, Vector3d()).normalize()
        }
        val fireVelocity = realDirection.mul(burnPower.toDouble() * 0.5, Vector3d())
        val smokeVelocity = realDirection.mul(burnPower.toDouble(), Vector3d())
        for (i in 0..4) {
            val fireRandomX = serverLevel.random.nextDouble(1.0, 1.5)
            val fireRandomY = serverLevel.random.nextDouble(1.0, 1.5)
            val fireRandomZ = serverLevel.random.nextDouble(1.0, 1.5)
            val thisFireVelocity = Vector3d(fireVelocity.x * fireRandomX, fireVelocity.y * fireRandomY, fireVelocity.z * fireRandomZ)
            serverLevel.addParticle(ParticleTypes.FLAME, realWorldPosition.x + (realDirection.x * 0.5), realWorldPosition.y + (realDirection.y * 0.5), realWorldPosition.z + (realDirection.z * 0.5), thisFireVelocity.x, thisFireVelocity.y, thisFireVelocity.z)
        }

        for (j in 0..12) {
            val sparkRandomX = serverLevel.random.nextDouble(-1.0, 1.0)
            val sparkRandomY = serverLevel.random.nextDouble(-1.0, 1.0)
            val sparkRandomZ = serverLevel.random.nextDouble(-1.0, 1.0)
            val thisSparkVelocity = Vector3d(fireVelocity.x * sparkRandomX, fireVelocity.y * sparkRandomY, fireVelocity.z * sparkRandomZ)
            serverLevel.addParticle(ParticleTypes.SMALL_FLAME, realWorldPosition.x + (realDirection.x * 0.5), realWorldPosition.y + (realDirection.y * 0.5), realWorldPosition.z + (realDirection.z * 0.5), thisSparkVelocity.x, thisSparkVelocity.y, thisSparkVelocity.z)
        }

        serverLevel.addParticle(ParticleTypes.LARGE_SMOKE, realWorldPosition.x + (realDirection.x * 0.5), realWorldPosition.y + (realDirection.y * 0.5), realWorldPosition.z + (realDirection.z * 0.5), smokeVelocity.x, smokeVelocity.y, smokeVelocity.z)

        if (burnProgress >= burnTime) {
            isBurning = false
            if (hasNextBlock) {
                val nextBlock = level!!.getBlockEntity(worldPosition.relative(blockState.getValue(DirectionalBlock.FACING)))
                if (nextBlock is SugarRocketBlockEntity && !nextBlock.isBurning) {
                    nextBlock.ignite(burnTime, burnPower)
                }
            }
            if (ship != null) {
                SugarRocketController.getOrCreate(ship).removeRocket(worldPosition)
            }
            level!!.destroyBlock(worldPosition, false)
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