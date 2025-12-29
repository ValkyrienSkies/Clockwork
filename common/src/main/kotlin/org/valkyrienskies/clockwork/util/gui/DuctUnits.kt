package org.valkyrienskies.clockwork.util.gui

abstract class DuctUnits {

    interface Convertible<T> {
        fun base(): T
        fun toBase(value: Double): Double
        fun fromBase(value: Double): Double
        fun convertTo(value: Double, to: Convertible<T>?) = to?.fromBase(this.toBase(value)) ?: this.toBase(value)
        fun display(value: Double, allowSimplification: Boolean = true): Pair<String, String>
    }

    enum class TemperatureUnit(val langKey: String) : Convertible<TemperatureUnit> {
        KELVIN("unit.temp.kelvin") {
            override fun toBase(value: Double) = value
            override fun fromBase(value: Double) = value
        },
        CELSIUS("unit.temp.celsius") {
            override fun toBase(value: Double) = value + 273.15
            override fun fromBase(value: Double) = value - 273.15
        },
        FAHRENHEIT("unit.temp.fahrenheit") {
            override fun toBase(value: Double) = (value - 32.0) * 5.0 / 9.0 + 273.15
            override fun fromBase(value: Double) = (value - 273.15) * 9.0 / 5.0 + 32.0
        };

        companion object { val BASE = KELVIN }
        override fun base() = BASE
        override fun display(value: Double, allowSimplification: Boolean) = Pair("%.0f".format(value), langKey)
    }

    enum class PressureUnit(val langKey: String) : Convertible<PressureUnit> {
        PASCAL("unit.pressure.pascal") {
            override fun toBase(value: Double) = value
            override fun fromBase(value: Double) = value
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification) return Pair("%.0f".format(value), langKey)
                return when {
                    value >= 10_000_000.0 -> Pair("%.1f".format(value / 1_000_000.0), "unit.pressure.megapascal")
                    value >= 10_000.0 -> Pair("%.1f".format(value / 1_000.0), "unit.pressure.kilopascal") // from 10 kPa
                    else -> Pair("%.0f".format(value), langKey)
                }
            }
        },
        BAR("unit.pressure.bar") {
            override fun toBase(value: Double) = value * 100_000.0
            override fun fromBase(value: Double) = value / 100_000.0
        },
        PSI("unit.pressure.psi") {
            override fun toBase(value: Double) = value * 6894.757
            override fun fromBase(value: Double) = value / 6894.757
        },
        MM_HG("unit.pressure.mm_hg") {
            override fun toBase(value: Double) = value * 133.322
            override fun fromBase(value: Double) = value / 133.322
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> = Pair("%.0f".format(value), langKey)
        };

        companion object { val BASE = PASCAL }
        override fun base() = BASE
        override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> = Pair("%.1f".format(value), langKey)
    }

    enum class EnergyUnit(val langKey: String) : Convertible<EnergyUnit> {
        JOULE("unit.energy.joule") {
            override fun toBase(value: Double) = value
            override fun fromBase(value: Double) = value
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification) return Pair("%.0f".format(value), langKey)
                return when {
                    value >= 10_000_000_000.0 -> Pair("%.2f".format(value / 1_000_000_000.0), "unit.energy.gigajoule")
                    value >= 10_000_000.0 -> Pair("%.2f".format(value / 1_000_000.0), "unit.energy.megajoule")
                    value >= 10_000.0 -> Pair("%.1f".format(value / 1_000.0), "unit.energy.kilojoule") // from 10 kJ
                    else -> Pair("%.0f".format(value), langKey)
                }
            }
        },
        KCAL("unit.energy.kcal") {
            override fun toBase(value: Double) = value * 4184.0
            override fun fromBase(value: Double) = value / 4184.0
            override fun display(value: Double, allowSimplification: Boolean) = Pair("%.0f".format(value), langKey)
        };

        companion object { val BASE = JOULE }
        override fun base() = BASE
        override fun display(value: Double, allowSimplification: Boolean) = Pair("%.1f".format(value), langKey)
    }

    enum class VolumeUnit(val langKey: String) : Convertible<VolumeUnit> {
        CUBIC_METER("unit.volume.cubic_meter") {
            override fun toBase(value: Double) = value
            override fun fromBase(value: Double) = value
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification) return Pair("%.1f".format(value), langKey)
                return when {
                    value >= 0.1 -> Pair("%.2f".format(value), "unit.volume.cubic_meter") // from 0.1 m3
                    value >= 0.001 -> Pair("%.0f".format(value * 1000.0), "unit.volume.liter")
                    else -> Pair("%.0f".format(value * 1_000_000.0), "unit.volume.milliliter")
                }
            }
        },
        GALLON("unit.volume.gallon") {
            override fun toBase(value: Double) = value * 0.00378541
            override fun fromBase(value: Double) = value / 0.00378541
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification || value >= 1.0) return Pair("%.1f".format(value), langKey)
                return Pair("%.0f".format(value * 128.0), "unit.volume.fluid_ounce")
            }
        };

        companion object { val BASE = CUBIC_METER }
        override fun base() = BASE
        override fun display(value: Double, allowSimplification: Boolean) = Pair("%.1f".format(value), langKey)
    }

    enum class MassUnit(val langKey: String) : Convertible<MassUnit> {
        KILOGRAM("unit.mass.kilogram") {
            override fun toBase(value: Double) = value
            override fun fromBase(value: Double) = value
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification) return Pair("%.1f".format(value), langKey)
                return when {
                    value >= 1000.0 -> Pair("%.1f".format(value / 1000.0), "unit.mass.ton")
                    value >= 0.1 -> Pair("%.1f".format(value), langKey) // from 0.1 kg
                    else -> Pair("%.0f".format(value * 1000.0), "unit.mass.gram")
                }
            }
        },
        POUND("unit.mass.pound") {
            override fun toBase(value: Double) = value * 0.453592
            override fun fromBase(value: Double) = value / 0.453592
            override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> {
                if (!allowSimplification) return Pair("%.1f".format(value), langKey)
                return when {
                    value >= 2000.0 -> Pair("%.1f".format(value / 2000.0), "unit.mass.imperial_ton")
                    value >= 1.0 -> Pair("%.1f".format(value), langKey)
                    else -> Pair("%.0f".format(value * 16.0), "unit.mass.ounce")
                }
            }
        };

        companion object { val BASE = KILOGRAM }
        override fun base() = BASE
        override fun display(value: Double, allowSimplification: Boolean): Pair<String, String> = Pair("%.1f".format(value), langKey)
    }
}