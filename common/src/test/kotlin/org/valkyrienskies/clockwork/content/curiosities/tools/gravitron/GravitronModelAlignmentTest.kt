package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.PI

class GravitronModelAlignmentTest {
    @Test
    fun `idle dial preserves the authored mesh and rests on the original hub`() {
        val authored = model("gravitroncompiled").getAsJsonArray("elements")
            .map { it.asJsonObject }.first { it.get("name")?.asString == "dialHand" }
        val needle = model("dialhand").getAsJsonArray("elements")[0].asJsonObject
        for (property in listOf("from", "to", "rotation", "faces")) {
            assertEquals(authored[property], needle[property], property)
        }
        val hub = model("gravitronbase").getAsJsonArray("elements")[3].asJsonObject
            .getAsJsonObject("rotation")["origin"].vector()
        assertTrue(hub.distance(Vector3f(GravitronVisuals.DIAL_X, GravitronVisuals.DIAL_Y, GravitronVisuals.DIAL_Z)) < 0.00001f)
        val rest = GravitronVisuals.dialRotation(GravitronAnimation().sample(0, 0f).dial)
        for (point in listOf(needle["from"].vector(), needle["to"].vector())) {
            assertTrue(point.distance(rest.transform(Vector3f(point))) < 0.00001f)
        }
    }

    @Test
    fun `dial sweep stays in the authored tilted plane`() {
        val authored = model("gravitroncompiled").getAsJsonArray("elements")
            .map { it.asJsonObject }.first { it.get("name")?.asString == "dialHand" }
        val tilt = authored.getAsJsonObject("rotation")["angle"].asFloat * (PI / 180).toFloat()
        val origin = authored.getAsJsonObject("rotation")["origin"].vector()
        val normal = Vector3f(0f, 0f, 1f).rotateX(tilt)
        val hub = Vector3f(GravitronVisuals.DIAL_X, GravitronVisuals.DIAL_Y, GravitronVisuals.DIAL_Z)
        for (angle in listOf(10f, 95f, 180f, 350f)) {
            for (point in listOf(authored["from"].vector(), authored["to"].vector())) {
                val relative = point.sub(origin).rotateX(tilt).add(origin).sub(hub)
                val turned = GravitronVisuals.dialRotation(angle).transform(Vector3f(relative))
                assertEquals(relative.dot(normal), turned.dot(normal), 0.00001f)
                assertEquals(relative.length(), turned.length(), 0.00001f)
            }
        }
    }

    @Test
    fun `supercharged energy retains the full authored size and body pivot`() {
        val authored = model("gravitroncompiled").getAsJsonArray("elements")[42].asJsonObject
        val energy = model("overload_fx").getAsJsonArray("elements")[0].asJsonObject
        for (property in listOf("from", "to", "rotation")) assertEquals(authored[property], energy[property], property)
        val center = energy["from"].vector().add(energy["to"].vector()).mul(0.5f)
        assertTrue(center.distance(Vector3f(GravitronVisuals.OVERCHARGE_X,
            GravitronVisuals.OVERCHARGE_Y, GravitronVisuals.OVERCHARGE_Z)) < 0.00001f)
    }

    private fun model(name: String): JsonObject = javaClass.getResourceAsStream(
        "/assets/vs_clockwork/models/item/gravitron/$name.json"
    )!!.reader().use { JsonParser.parseReader(it).asJsonObject }

    private fun JsonElement.vector(): Vector3f = asJsonArray.let { Vector3f(it[0].asFloat, it[1].asFloat, it[2].asFloat) }
}
