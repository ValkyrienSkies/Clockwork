package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.SugarRocketController
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.Random

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
            if (isBurning) {
                val clientLevel = level as ClientLevel
                val ship = level.getShipObjectManagingPos(worldPosition)
                val realWorldPosition = level.toWorldCoordinates(worldPosition).add(0.5, 0.5, 0.5)
                var realDirection = blockState.getValue(DirectionalBlock.FACING).opposite.normal.toJOMLD()
                if (ship != null) {
                    realDirection = ship.transform.rotation.transform(realDirection, Vector3d()).normalize()
                }
                val fireVelocity = realDirection.mul(burnPower.toDouble() * 0.5, Vector3d())
                val smokeVelocity = realDirection.mul(burnPower.toDouble(), Vector3d())
                val random = Random()
                for (i in 0..4) {
                    val fireRandomX = random.nextDouble(-0.15, 0.15)
                    val fireRandomY = random.nextDouble(-0.15, 0.15)
                    val fireRandomZ = random.nextDouble(-0.15, 0.15)
                    val thisFireVelocity = Vector3d(fireVelocity.x + fireRandomX, fireVelocity.y + fireRandomY, fireVelocity.z + fireRandomZ)
                    clientLevel.addParticle(ParticleTypes.FLAME, realWorldPosition.x, realWorldPosition.y, realWorldPosition.z, thisFireVelocity.x, thisFireVelocity.y, thisFireVelocity.z)
                }

                for (j in 0..12) {
                    val sparkRandomX = random.nextDouble(-0.5, 0.5)
                    val sparkRandomY = random.nextDouble(-0.5, 0.5)
                    val sparkRandomZ = random.nextDouble(-0.5, 0.5)
                    val thisSparkVelocity = Vector3d(fireVelocity.x + sparkRandomX, fireVelocity.y + sparkRandomY, fireVelocity.z + sparkRandomZ)
                    clientLevel.addParticle(ParticleTypes.SMALL_FLAME, realWorldPosition.x, realWorldPosition.y, realWorldPosition.z, thisSparkVelocity.x, thisSparkVelocity.y, thisSparkVelocity.z)
                }

                clientLevel.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, realWorldPosition.x, realWorldPosition.y, realWorldPosition.z, smokeVelocity.x, smokeVelocity.y, smokeVelocity.z)
            }
        }
    }

    fun induceIgnition() {
        if (this.level == null || this.level!!.isClientSide) return
        if (isBurning) return
        val slevel = this.level!! as ServerLevel
        val block = slevel.getBlockState(worldPosition).block
        if (block !is SugarRocketBlock) return
        block.triggerAdjacent(slevel, worldPosition, slevel.getBlockState(worldPosition))
    }

    fun ignite(time: Int = burnTime, power: Int = burnPower) {
        isBurning = true
        burnTime = time
        burnPower = power

        val serverLevel = level as ServerLevel

        val realWorldPosition = level.toWorldCoordinates(worldPosition).add(0.5, 0.5, 0.5)
        serverLevel.playSound(null, realWorldPosition.x, realWorldPosition.y, realWorldPosition.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.05f)

        val ship = serverLevel.getShipObjectManagingPos(worldPosition)
        if (ship != null) {
            SugarRocketController.getOrCreate(ship).addRocket(worldPosition, burnPower.toDouble() * ClockworkConfig.SERVER.sugarRocketBlockThrust, blockState.getValue(DirectionalBlock.FACING))
        }
        sendData()
    }

    fun burn() {
        burnProgress++
        val serverLevel = level as ServerLevel
        val ship = serverLevel.getShipObjectManagingPos(worldPosition)
        val realWorldPosition = level.toWorldCoordinates(worldPosition).add(0.5, 0.5, 0.5)

        if (burnProgress >= burnTime) {
            isBurning = false
            serverLevel.playSound(null, realWorldPosition.x, realWorldPosition.y, realWorldPosition.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 0.35f)
            val nextBlock = level!!.getBlockEntity(worldPosition.relative(blockState.getValue(DirectionalBlock.FACING)))
            if (nextBlock is SugarRocketBlockEntity && !nextBlock.isBurning) {
                nextBlock.ignite(burnTime, burnPower)
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
        tag.putInt("burnPower", burnPower)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        val wasBurning = isBurning
        isBurning = tag.getBoolean("burning")
        burnProgress = tag.getInt("burnProgress")
        burnTime = tag.getInt("burnTime")
        burnPower = tag.getInt("burnPower")

        if (clientPacket && isBurning) {
            clientBurnProgress.updateChaseTarget((burnProgress.toDouble() / burnTime.toDouble()).toFloat())
            if (!wasBurning && level != null) {
                val realWorldPos = level!!.toWorldCoordinates(worldPosition).add(0.5, 0.5, 0.5)
                level!!.addParticle(ParticleTypes.FLASH, realWorldPos.x, realWorldPos.y, realWorldPos.z, 0.0, 0.0, 0.0)
            }
        }
    }
}
