package com.example.fragmentexercise.data.model.serialization

import com.example.fragmentexercise.data.model.Gender
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class GenderDeserializer : JsonDeserializer<Gender> {
    override fun deserialize(
        p0: JsonElement?,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): Gender? {
        return Gender.fromValue(p0?.asString ?: Gender.OTHER.value)
    }
}