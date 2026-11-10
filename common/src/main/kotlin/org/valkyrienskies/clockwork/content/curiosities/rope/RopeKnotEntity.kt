package org.valkyrienskies.clockwork.content.curiosities.rope

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringClipboard
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringControls
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import java.util.UUID
import java.util.Optional

class RopeKnotEntity(type: EntityType<*>, level: Level) : Entity(type, level) {
    var linkId: UUID?
        get() = entityData.get(LINK).orElse(null)
        set(value) { entityData.set(LINK, Optional.ofNullable(value)) }
    var spring: Boolean
        get() = entityData.get(SPRING)
        set(value) { entityData.set(SPRING, value) }
    var end: Int
        get() = entityData.get(END)
        set(value) { entityData.set(END, value) }
    init { noPhysics = true; isNoGravity = true }
    override fun defineSynchedData() {
        entityData.define(LINK, Optional.empty())
        entityData.define(END, 0)
        entityData.define(SPRING, false)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) = Unit
    override fun addAdditionalSaveData(tag: CompoundTag) = Unit
    override fun getAddEntityPacket() = ClientboundAddEntityPacket(this)
    override fun isPickable() = !isRemoved
    override fun isPushable() = false

    override fun skipAttackInteraction(attacker: Entity): Boolean {
        if (spring && attacker is Player && SpringClipboard.isClipboard(attacker.mainHandItem)) {
            if (attacker is ServerPlayer) SpringControls.pasteFromClipboard(attacker, this)
            return true
        }
        return super.skipAttackInteraction(attacker)
    }

    override fun tick() {
        super.tick()
        val world = level() as? ServerLevel ?: return
        val link = linkId?.let { WandLinks.get(world).links[it] }
        if (link == null || !link.physical) { discard(); return }
        spring = link.spring
        val anchor = if (end == 0) link.a else link.b
        val point = anchor.world(world) ?: return
        setPos(point.x, point.y - bbHeight / 2, point.z)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val player = source.entity as? ServerPlayer ?: return false
        if (source.directEntity !== player) return false
        return RopeKnotServer.hit(player, this)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (spring && hand == InteractionHand.MAIN_HAND && SpringClipboard.isClipboard(stack)) {
            if (player is ServerPlayer) SpringControls.copyToClipboard(player, this, stack)
            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        if (spring && SpringControls.isTool(stack)) {
            if (player is ServerPlayer) SpringControls.use(player, this, stack)
            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        if (stack.item !is RopeKnotItem || !player.mayBuild()) return InteractionResult.PASS
        if (player is ServerPlayer) {
            val link = linkId?.let { WandLinks.get(player.serverLevel()).links[it] } ?: return InteractionResult.PASS
            RopeKnotServer.tie(player, stack, if (end == 0) link.a else link.b)
        }
        return InteractionResult.sidedSuccess(level().isClientSide)
    }

    companion object {
        private val LINK = SynchedEntityData.defineId(RopeKnotEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)
        private val SPRING = SynchedEntityData.defineId(RopeKnotEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val END = SynchedEntityData.defineId(RopeKnotEntity::class.java, EntityDataSerializers.INT)
    }
}
