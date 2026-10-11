package com.example.viewpager2exercise1.data.model

data class Zodiac(
    val name: String,
    val drawableId: Int?
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Zodiac

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
