package org.valkyrienskies.clockwork.util.render

import org.slf4j.LoggerFactory
import java.lang.reflect.Method

/** Optional Iris API, shared by Iris and Oculus. Query the live pack state, not just mod presence. */
object ShaderPackCompat {
    private data class Api(val instance: Any, val enabled: Method, val shadow: Method)
    private val api: Api? = sequenceOf("net.irisshaders.iris", "net.coderbot.iris").mapNotNull { namespace ->
        try {
            val type = Class.forName("$namespace.api.v0.IrisApi")
            Api(type.getMethod("getInstance").invoke(null), type.getMethod("isShaderPackInUse"),
                type.getMethod("isRenderingShadowPass"))
        } catch (_: ClassNotFoundException) {
            null
        } catch (ex: ReflectiveOperationException) {
            LoggerFactory.getLogger("Clockwork/ShaderPackCompat").warn("Cannot access the Iris/Oculus API", ex)
            null
        }
    }.firstOrNull()
    private var warned = false

    fun enabled(): Boolean = query(api?.enabled)
    fun shadowPass(): Boolean = query(api?.shadow)

    private fun query(method: Method?): Boolean {
        if (method == null) return false
        return try {
            method.invoke(api!!.instance) as Boolean
        } catch (ex: ReflectiveOperationException) {
            if (!warned) {
                LoggerFactory.getLogger("Clockwork/ShaderPackCompat").warn("Cannot query the Iris/Oculus pipeline", ex)
                warned = true
            }
            false
        }
    }
}
