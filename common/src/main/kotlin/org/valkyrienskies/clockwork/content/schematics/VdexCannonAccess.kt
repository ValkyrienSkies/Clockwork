package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.content.schematics.requirement.ItemRequirement
import net.minecraft.world.item.ItemStack

interface VdexCannonAccess {
    var vdexBuild: VdexCannonBuild?
}

interface VdexCannonInventory {
    fun blueprint(): ItemStack
    fun canFinishBlueprint(): Boolean
    fun finishBlueprint(): Boolean
    fun takeMaterials(requirement: ItemRequirement): ItemStack?
}
