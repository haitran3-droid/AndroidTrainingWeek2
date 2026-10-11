package com.example.viewpager2exercise1.data.datasource

import com.example.viewpager2exercise1.R
import com.example.viewpager2exercise1.data.model.Zodiac

class ZodiacLocalDataSourceImpl: ZodiacLocalDataSource {
    private val zodiacs = listOf(
        Zodiac("Tý", R.drawable.mouse),
        Zodiac("Sửu", R.drawable.buffalo),
        Zodiac("Dần", R.drawable.tiger),
        Zodiac("Mão", R.drawable.rabbit),
        Zodiac("Thìn", R.drawable.dragon),
        Zodiac("Tỵ", R.drawable.snake),
        Zodiac("Ngọ", R.drawable.horse),
        Zodiac("Mùi", R.drawable.sheep),
        Zodiac("Thân", R.drawable.monkey),
        Zodiac("Dậu", R.drawable.cock),
        Zodiac("Tuất", R.drawable.dog),
        Zodiac("Hợi", R.drawable.pig)
    )

    override fun getZodiacs(): List<Zodiac> {
        return zodiacs
    }
}