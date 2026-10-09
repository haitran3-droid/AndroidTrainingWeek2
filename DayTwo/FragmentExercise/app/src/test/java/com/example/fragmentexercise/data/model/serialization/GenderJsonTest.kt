package com.example.fragmentexercise.data.model.serialization

import com.example.fragmentexercise.data.model.Gender
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class GenderJsonTest {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Gender::class.java, GenderDeserializer())
        .registerTypeAdapter(Gender::class.java, GenderSerializer())
        .create()

    @Test
    fun readsVietnameseGenderLabels() {
        assertEquals(Gender.MALE, gson.fromJson("\"Nam\"", Gender::class.java))
        assertEquals(Gender.FEMALE, gson.fromJson("\"Nữ\"", Gender::class.java))
        assertEquals(Gender.OTHER, gson.fromJson("\"Khác\"", Gender::class.java))
    }

    @Test
    fun preservesGenderWhenSavingAndReadingAgain() {
        Gender.entries.forEach { gender ->
            val json = gson.toJson(gender, Gender::class.java)
            assertEquals("\"${gender.value}\"", json)
            assertEquals(gender, gson.fromJson(json, Gender::class.java))
        }
    }
}