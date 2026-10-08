package pro.branium.recyclerviewex.data.model.adapter

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import pro.branium.recyclerviewex.data.model.Gender
import java.lang.reflect.Type

class GenderSerializer : JsonSerializer<Gender>{
    override fun serialize(p0: Gender?, p1: Type?, p2: JsonSerializationContext?): JsonElement? {
        return JsonPrimitive(p0?.value ?: "Khác")
    }
}