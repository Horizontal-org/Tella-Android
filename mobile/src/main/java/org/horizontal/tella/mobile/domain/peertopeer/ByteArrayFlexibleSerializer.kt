package org.horizontal.tella.mobile.domain.peertopeer

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import java.util.Base64 // On Android <26, switch to android.util.Base64

object ByteArrayFlexibleSerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ByteArrayFlexible", PrimitiveKind.STRING)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun serialize(encoder: Encoder, value: ByteArray) {
        // Always serialize as Base64 string to keep responses compact/consistent
        val b64 = Base64.getEncoder().encodeToString(value)
        encoder.encodeString(b64)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): ByteArray {
        if (decoder !is JsonDecoder)
            throw SerializationException("Expected JsonDecoder")

        val el = decoder.decodeJsonElement()
        return when (el) {
            is JsonPrimitive -> {
                // Base64 string
                val s = el.content
                try {
                    Base64.getDecoder().decode(s)
                } catch (e: Exception) {
                    throw SerializationException("Invalid Base64 in thumbnail", e)
                }
            }
            is JsonArray -> {
                // [0..255, 0..255, ...]
                val out = ByteArray(el.size)
                el.forEachIndexed { i, jp ->
                    val n = jp.jsonPrimitive.int
                    // clamp to 0..255 and convert to signed byte
                    val b = (n.coerceIn(0, 255) and 0xFF).toByte()
                    out[i] = b
                }
                out
            }
            is JsonNull -> {
                // shouldn't reach here for non-null type; handled by .nullable wrapper
                throw SerializationException("thumbnail was null")
            }
            else -> throw SerializationException("thumbnail must be Base64 string or int[]")
        }
    }
}