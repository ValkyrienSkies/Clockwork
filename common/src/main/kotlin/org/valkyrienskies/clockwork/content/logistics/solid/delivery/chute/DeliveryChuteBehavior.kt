package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.network.chat.Component

class DeliveryChuteBehavior(label: Component, be: SmartBlockEntity, slot: ValueBoxTransform) : ScrollValueBehaviour(label, be, slot) {
}