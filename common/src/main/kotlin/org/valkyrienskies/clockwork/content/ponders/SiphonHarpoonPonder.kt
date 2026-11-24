package org.valkyrienskies.clockwork.content.ponders

import com.mojang.math.Axis
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.level.PonderLevel
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.harpoon.HarpoonBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve

object SiphonHarpoonPonder {
    fun siphoning(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("siphon_harpoon", "Collecting gas with a Siphon Harpoon")
        scene.configureBasePlate(0, 0, 7)
        scene.showBasePlate()
        val launcher = util.grid().at(5, 1, 3)
        val rear = launcher.east()
        val gear = launcher.north()
        val drive = util.select().fromTo(gear, gear.east())
        val lever = launcher.south()
        val tank = launcher.west(4)
        val muzzle = util.vector().centerOf(launcher).add(-5.0 / 16, 0.0, 0.0)

        fun speed(rpm: Float) {
            scene.world().setKineticSpeed(drive, rpm)
            scene.world().setKineticSpeed(util.select().position(launcher), -rpm)
        }

        fun load() {
            scene.overlay().showControls(util.vector().topOf(launcher), Pointing.DOWN, 30)
                .rightClick().withItem(ClockworkItems.HARPOON.asStack())
            scene.idle(10)
            scene.world().modifyBlockEntity(launcher, HarpoonBlockEntity::class.java) { it.load(ClockworkItems.HARPOON.asStack()) }
        }

        fun charge() {
            for (step in 1..5) {
                scene.idle(8)
                scene.world().modifyBlockEntityNBT(util.select().position(launcher), HarpoonBlockEntity::class.java) {
                    it.putDouble("Charge", step / 25.0)
                }
            }
        }

        fun toggleLever() {
            scene.world().toggleRedstonePower(util.select().fromTo(launcher, lever))
            scene.effects().indicateRedstone(lever)
        }

        fun fire(impact: Vec3) {
            toggleLever()
            scene.world().modifyBlockEntity(launcher, HarpoonBlockEntity::class.java) { it.unload() }
            scene.addInstruction { it.addElement(HarpoonShot(muzzle, impact, it.currentTime)) }
        }

        scene.idle(10)
        scene.world().showSection(util.select().position(launcher), Direction.DOWN)
        scene.idle(10)
        scene.world().showSection(util.select().position(rear), Direction.WEST)
        scene.overlay().showText(100).text("Connect a Duct to the back of the Siphon Harpoon to collect gas")
            .pointAt(util.vector().topOf(rear)).placeNearTarget().attachKeyFrame()
        scene.idle(115)

        scene.overlay().showText(90).text("Right-click the launcher with a Harpoon to load it")
            .pointAt(muzzle).placeNearTarget().attachKeyFrame()
        load()
        scene.idle(95)

        scene.world().showSection(drive, Direction.DOWN)
        speed(64f)
        scene.effects().rotationDirectionIndicator(gear)
        scene.overlay().showText(95).text("Drive the launcher's cogwheel to wind it up")
            .pointAt(util.vector().topOf(gear)).placeNearTarget()
        charge()
        scene.idle(70)

        val creeper = scene.world().createEntity { level ->
            Creeper(EntityType.CREEPER, level).apply {
                moveTo(tank.x + 0.5, tank.y.toDouble(), tank.z + 0.5, -45f, 0f)
                isNoAi = true
                isNoGravity = true
                noPhysics = true
            }
        }
        scene.world().showSection(util.select().position(lever), Direction.DOWN)
        scene.idle(15)
        scene.overlay().showText(100).text("Apply a Redstone signal to fire the Harpoon")
            .pointAt(util.vector().topOf(lever)).placeNearTarget().attachKeyFrame()
        scene.idle(25)
        fire(util.vector().centerOf(tank).add(0.22, 0.0, 0.0))
        scene.idle(90)

        scene.overlay().showControls(util.vector().topOf(rear), Pointing.DOWN, 120).withGas(ClockworkGasses.METHANE)
        scene.overlay().showText(120).text("Certain creatures supply gas through the hose. Creepers supply Bog gas")
            .pointAt(util.vector().topOf(tank)).placeNearTarget().attachKeyFrame()
        scene.idle(135)

        speed(128f)
        scene.effects().rotationSpeedIndicator(gear)
        scene.overlay().showText(130).text("Faster rotation draws more gas, but exceeding the safe speed hurts the creature")
            .pointAt(util.vector().topOf(gear)).placeNearTarget().colored(PonderPalette.RED)
        scene.idle(25)
        scene.world().modifyEntity(creeper) { (it as Creeper).hurtTime = 10 }
        scene.idle(120)

        scene.overlay().showText(95).text("Switch off the signal to release the hose")
            .pointAt(util.vector().topOf(lever)).placeNearTarget().attachKeyFrame()
        scene.idle(20)
        toggleLever()
        scene.addInstruction { it.forEach(HarpoonShot::class.java) { shot -> shot.isVisible = false } }
        scene.idle(90)

        scene.world().modifyEntity(creeper) { it.discard() }
        scene.world().showSection(util.select().position(tank), Direction.DOWN)
        speed(64f)
        load()
        charge()
        scene.overlay().showText(120).text("Fire into a Gas Tank to pump its contents through the rear Duct")
            .pointAt(util.vector().topOf(tank)).placeNearTarget().attachKeyFrame()
        scene.idle(15)
        fire(util.vector().blockSurface(tank, Direction.EAST))
        scene.idle(120)
        speed(128f)
        scene.effects().rotationSpeedIndicator(gear)
        scene.overlay().showText(95).text("Increase the speed of the cogwheel for more pumping pressure")
            .pointAt(util.vector().topOf(gear)).placeNearTarget()
        scene.idle(110)
        scene.markAsFinished()
    }

    private class HarpoonShot(val muzzle: Vec3, val impact: Vec3, val firedAt: Int) : AnimatedSceneElementBase() {
        override fun renderLast(world: PonderLevel, buffer: MultiBufferSource, graphics: GuiGraphics, fade: Float, pt: Float) {
            val time = (world.scene ?: return).currentTime - firedAt + pt
            val tip = muzzle.lerp(impact, (time / 7.0).coerceIn(0.0, 1.0))
            val length = RopeKnotRenderer.harpoonRopeLength(muzzle.distanceTo(tip))
            if (length > 0.0) {
                val curve = WandRopeCurve(muzzle, muzzle.add(-length, 0.0, 0.0), length * 1.03)
                val points = (0..12).map { curve.point(it / 12.0) }
                RopeKnotRenderer.strand(world, points, Vec3.ZERO, graphics.pose(), buffer,
                    RopeKnotRenderer.Material.HOSE, packedLight = LightTexture.FULL_BRIGHT)
            }
            val pose = graphics.pose()
            pose.pushPose()
            pose.translate(tip.x, tip.y, tip.z)
            pose.mulPose(Axis.ZP.rotationDegrees(90f))
            RopeKnotRenderer.harpoonModel(pose, buffer, LightTexture.FULL_BRIGHT)
            pose.popPose()
        }
    }
}
