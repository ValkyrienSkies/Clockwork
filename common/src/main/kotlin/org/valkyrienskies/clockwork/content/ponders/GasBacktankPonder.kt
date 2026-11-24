package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllItems
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

object GasBacktankPonder {
    fun backtank(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("gas_backtank", "Carrying gas in a Gas Backtank")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()

        val inlet = util.grid().at(2, 1, 1)
        val filling = inlet.above()
        val outlet = util.grid().at(2, 1, 3)
        val emptying = outlet.above()
        scene.idle(5)
        scene.world().showSection(util.select().fromTo(inlet, inlet.east()), Direction.DOWN)
        scene.idle(10)
        scene.world().showSection(util.select().position(filling), Direction.DOWN)
        scene.idle(10)
        scene.overlay().showText(110)
            .text("Place a Gas Backtank on a pressurized Duct to fill it")
            .pointAt(util.vector().blockSurface(inlet, Direction.UP)).placeNearTarget().attachKeyFrame()
        scene.idle(125)

        val wearer = scene.world().createEntity { level ->
            ArmorStand(level, 1.25, 1.0, 1.5).apply {
                setShowArms(true)
                setNoBasePlate(true)
                yRot = -110f
            }
        }
        scene.overlay().showControls(util.vector().topOf(filling), Pointing.DOWN, 30).rightClick()
        scene.overlay().showText(125)
            .text("Right-click with an empty hand and a free chest slot to put it on")
            .pointAt(util.vector().centerOf(filling)).placeNearTarget().attachKeyFrame()
        scene.idle(35)
        scene.world().modifyEntity(wearer) { entity ->
            val level = entity.level()
            val tank = ClockworkBlocks.GAS_BACKTANK.get().getCloneItemStack(level, filling, level.getBlockState(filling))
            (entity as ArmorStand).setItemSlot(EquipmentSlot.CHEST, tank)
        }
        scene.world().setBlock(filling, Blocks.AIR.defaultBlockState(), false)
        scene.idle(105)
        scene.world().modifyEntity(wearer) { entity ->
            (entity as ArmorStand).setItemSlot(EquipmentSlot.MAINHAND, ClockworkItems.HANDHELD_DRILL.asStack())
        }
        scene.overlay().showText(125)
            .text("The air in a worn Backtank can power tools such as the Power Drill")
            .pointAt(util.vector().of(1.25, 2.4, 1.5)).placeNearTarget()
        scene.idle(140)

        scene.world().modifyEntity(wearer) { entity ->
            val stand = entity as ArmorStand
            stand.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
            stand.setItemSlot(EquipmentSlot.HEAD, AllItems.COPPER_DIVING_HELMET.asStack())
        }
        val air = GasTypeRegistry.GAS_TYPES.getValue(ResourceLocation("kelvin", "air"))
        scene.overlay().showControls(util.vector().topOf(inlet.east()), Pointing.DOWN, 115).withGas(air)
        scene.overlay().showText(115)
            .text("Fill the Backtank with Air to breathe underwater with a Diving Helmet")
            .pointAt(util.vector().of(1.25, 2.75, 1.5)).placeNearTarget().attachKeyFrame()
        scene.idle(125)

        scene.world().showSection(util.select().fromTo(outlet.west(), outlet), Direction.DOWN)
        scene.idle(10)
        scene.overlay().showControls(util.vector().topOf(outlet), Pointing.DOWN, 25)
            .rightClick().withItem(ClockworkItems.GAS_BANKTANK.asStack())
        scene.idle(25)
        scene.world().showSection(util.select().position(emptying), Direction.DOWN)
        scene.world().modifyEntity(wearer) { entity ->
            val level = entity.level()
            val contents = (entity as ArmorStand).getItemBySlot(EquipmentSlot.CHEST).tag
            if (contents != null) ClockworkBlocks.GAS_BACKTANK.get().deserializeNode(
                emptying.toDuctNodePos(level.dimension().location()), contents, level
            )
            entity.discard()
        }
        scene.overlay().showText(120)
            .text("Place the Backtank on another Duct to supply it with the remaining gas")
            .pointAt(util.vector().topOf(outlet.west())).placeNearTarget().attachKeyFrame()
        scene.idle(135)
        scene.markAsFinished()
    }
}
