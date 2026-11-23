package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkItems
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class TireCondition {
    var wear = 0.0
        private set
    var damage = 0.0
        private set
    val leaks get() = damage.toInt()
    val wearGrade get() = (wear * 10).toInt()
    data class Hole(val angle: Double, val across: Double)
    private val sites = ArrayList<Hole>()
    val holes: List<Hole> get() = sites

    private fun fallbackHole(index: Int) = Hole(index * 2.4 + .35, when (index % 3) { 0 -> .75; 1 -> -.8; else -> .08 })

    fun read(tag: CompoundTag?, type: TireType?) {
        wear = savedWear(tag, type)
        damage = tag?.getDouble("TirePunctures")?.takeIf { it.isFinite() && type?.maintenance?.canPuncture == true }
            ?.coerceIn(0.0, 12.0) ?: 0.0
        sites.clear()
        val saved = tag?.getList("TireHoles", Tag.TAG_COMPOUND.toInt())
        repeat(leaks) { i ->
            val hole = saved?.takeIf { i < it.size }?.getCompound(i)
            val angle = hole?.getDouble("Angle")
            val across = hole?.getDouble("Across")
            sites.add(if (angle != null && angle.isFinite() && across != null && across in -0.96..0.96)
                Hole(angle, across) else fallbackHole(i))
        }
    }

    fun write(tag: CompoundTag) {
        if (wear > 0) tag.putDouble("TireWear", wear) else tag.remove("TireWear")
        if (damage > 0) tag.putDouble("TirePunctures", damage) else tag.remove("TirePunctures")
        if (sites.isEmpty()) tag.remove("TireHoles")
        else tag.put("TireHoles", ListTag().apply {
            for (site in sites) add(CompoundTag().apply {
                putDouble("Angle", site.angle)
                putDouble("Across", site.across)
            })
        })
    }

    fun abrade(tire: WheelTire, scrubDistance: Double, abrasion: Double): Boolean {
        val previous = wear
        wear = (wear + scrubDistance * abrasion * tire.type.maintenance.wearRate /
            (16_000 * tire.width / tire.size.width)).coerceAtMost(1.0)
        return wear != previous
    }

    fun puncture(tire: WheelTire, severity: Double, hit: Vector3d?): Boolean {
        val profile = tire.type.maintenance
        if (!profile.canPuncture || !severity.isFinite() || severity <= 0) return false
        val previous = damage
        damage = (damage + severity * (1.0 + wear) / profile.punctureResistance).coerceAtMost(12.0)
        val added = leaks - sites.size
        if (added > 0) {
            val site = if (hit != null && hit.isFinite) {
                val axial = (2 * hit.x / tire.width).coerceIn(-1.0, 1.0)
                val depth = ((hypot(hit.y, hit.z) - tire.rimRadius) / tire.thickness).coerceIn(0.0, 1.0)
                val across = if (abs(axial) < .9) axial * .52 else Math.copySign(.68 + .23 * (1 - depth), axial)
                Hole(atan2(hit.z, hit.y), across)
            } else fallbackHole(sites.size)
            repeat(added) { i ->
                val spread = i - (added - 1) * .5
                sites.add(Hole(site.angle + spread * .15 / tire.carcassRadius, (site.across + spread * .11).coerceIn(-.96, .96)))
            }
        }
        return damage != previous
    }

    fun canRepair(stack: ItemStack, type: TireType): Boolean {
        val profile = type.maintenance
        val material = BuiltInRegistries.ITEM.getKey(stack.item) == profile.patchMaterial
        if (!material && !stack.`is`(ClockworkItems.TAR.get())) return false
        return damage > 0 || material && profile.repairsTread && wear > 0
    }

    fun repair(stack: ItemStack, type: TireType) {
        damage = (damage - 1.0).coerceAtLeast(0.0)
        while (sites.size > leaks) sites.removeAt(sites.lastIndex)
        if (type.maintenance.repairsTread && BuiltInRegistries.ITEM.getKey(stack.item) == type.maintenance.patchMaterial)
            wear = (wear - .25).coerceAtLeast(0.0)
    }

    fun status(): Component = Component.translatable("item.vs_clockwork.tire.condition",
        (wear * 100).roundToInt(), leaks)
}

private fun savedWear(tag: CompoundTag?, type: TireType?) =
    tag?.getDouble("TireWear")?.takeIf { it.isFinite() && (type?.maintenance?.wearRate ?: 0.0) > 0 }
        ?.coerceIn(0.0, 1.0) ?: 0.0

internal fun WheelTire.withSavedWear(tag: CompoundTag?): WheelTire {
    val grade = (savedWear(tag, type) * 10).toInt()
    return if (grade == wearGrade) this else copy(wearGrade = grade)
}
