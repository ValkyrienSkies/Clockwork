package org.valkyrienskies.clockwork.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.joml.Vector3i
import org.joml.Vector3ic
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdKeySerializer
import java.io.IOException

// Custom KeySerializer for Vector3ic
class Vector3icKeySerializer : StdKeySerializer() {
    override fun serialize(key: Any, gen: JsonGenerator, serializers: SerializerProvider) {
        if (key is Vector3ic) {
            // Serialize as a string in the format "x,y,z"
            val serializedKey = "${key.x()},${key.y()},${key.z()}"
            gen.writeFieldName(serializedKey)
        } else {
            throw IllegalArgumentException("Key is not of type Vector3ic: $key")
        }
    }
}

// Custom KeyDeserializer for Vector3ic
class Vector3icKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String?, ctxt: DeserializationContext?): Any? {
        if (key.isNullOrEmpty()) {
            return null
        }

        return try {
            // Parse the "x,y,z" format into Vector3ic
            val parts = key.split(",")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid Vector3ic key format. Expected 'x,y,z'. Got: $key")
            }

            val x = parts[0].trim().toInt()
            val y = parts[1].trim().toInt()
            val z = parts[2].trim().toInt()

            Vector3i(x, y, z)
        } catch (e: NumberFormatException) {
            throw IOException("Failed to deserialize Vector3ic key: $key", e)
        }
    }
}