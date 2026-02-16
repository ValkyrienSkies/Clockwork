package org.valkyrienskies.clockwork

import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

object ClockworkGasses {
    private val iconCodes: HashMap<ResourceLocation, String> = HashMap()

    private fun getIcon(name: String): ResourceLocation {
        return ClockworkMod.asResource("textures/icons/$name.png")
    }

    @JvmStatic
    val ICON_FONT_LOCATION = ClockworkMod.asResource("gas_icons")

    @JvmStatic
    fun getDisplayCharacterCode(gasType: GasType): String {
        return iconCodes.getOrPut(gasType.resourceLocation) {
            "\uE000"
        }
    }

    val EXHAUST = GasType("Exhaust", ClockworkMod.asResource("exhaust"),  density = 1.98, viscosity =1.10e-5, specificHeatCapacity = 2.846,
        thermalConductivity = 0.031, iconLocation = getIcon("exhaust"))
    val STEAM = GasType("Steam", ClockworkMod.asResource( "steam"),  density = 0.762 , viscosity = 1.223e-5, specificHeatCapacity = 2.2,
        thermalConductivity = 0.031, iconLocation = getIcon("steam"))

    val PHLOGISTON = GasType("Phlogiston",ClockworkMod.asResource( "phlogiston"),  density = 3.0, viscosity = 2.0e-5, specificHeatCapacity = 14.30,
        thermalConductivity = 0.240, sutherlandConstant = 150.0, adiabaticIndex = 1.008, iconLocation = getIcon("phlogiston"))
    val HELIUM = GasType("Aether",ClockworkMod.asResource( "aether"),  density = 0.166, viscosity = 1.96e-5, specificHeatCapacity = 5.1832,
        thermalConductivity = 0.151, sutherlandConstant = 79.4, adiabaticIndex = 1.66, iconLocation = getIcon("aether"))
    val HYDROGEN = GasType("Stellane",ClockworkMod.asResource( "stellane"),  density = 0.08988, viscosity = 0.88e-5, specificHeatCapacity = 14.30,
        thermalConductivity = 0.18, sutherlandConstant = 72.0, adiabaticIndex = 1.4, iconLocation = getIcon("stellane"))
    val METHANE = GasType("Bog",ClockworkMod.asResource( "bog"),  density = 0.657, viscosity = 1.10e-5, specificHeatCapacity = 2.2,
        thermalConductivity = 0.031, sutherlandConstant = 90.0, adiabaticIndex = 16.0, iconLocation = getIcon("bog"))

    val VITIROL = GasType("Vitriol", ClockworkMod.asResource( "vitriol"), density = 2.2, viscosity = 2.0e-5, specificHeatCapacity = 8.0,
        thermalConductivity = 0.16, sutherlandConstant = 68.4, adiabaticIndex = 1.008, getIcon("vitriol"))
    val OZONITE = GasType("Ozonite", ClockworkMod.asResource( "ozonite"),  density = 1.15, viscosity = 0.78e-5, specificHeatCapacity = 0.92,
        thermalConductivity = 0.024, sutherlandConstant = 127.0, adiabaticIndex = 1.4, getIcon("ozonite"))

    @JvmStatic
    fun init() {
        GasTypeRegistry.register(EXHAUST)
        GasTypeRegistry.register(STEAM)
        GasTypeRegistry.register(PHLOGISTON)
        GasTypeRegistry.register(HELIUM)
        GasTypeRegistry.register(HYDROGEN)
        GasTypeRegistry.register(METHANE)
        GasTypeRegistry.register(VITIROL)
        GasTypeRegistry.register(OZONITE)

        //ordering
        iconCodes.put(ResourceLocation(KelvinMod.MOD_ID, "air"), "\uE001")
        iconCodes.put(HELIUM.resourceLocation, "\uE002")
        iconCodes.put(METHANE.resourceLocation, "\uE003")
        iconCodes.put(EXHAUST.resourceLocation, "\uE004")
        iconCodes.put(OZONITE.resourceLocation, "\uE005")
        iconCodes.put(PHLOGISTON.resourceLocation, "\uE006")
        iconCodes.put(STEAM.resourceLocation, "\uE007")
        iconCodes.put(HYDROGEN.resourceLocation, "\uE008")
        iconCodes.put(VITIROL.resourceLocation, "\uE009")
    }

}
