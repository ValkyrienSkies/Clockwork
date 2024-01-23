package org.valkyrienskies.kelvin

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KProperty

fun logger(name: String): ClassLogger = ClassLogger(LogManager.getLogger(name))

@JvmInline
value class ClassLogger(val logger: Logger) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger = logger
}