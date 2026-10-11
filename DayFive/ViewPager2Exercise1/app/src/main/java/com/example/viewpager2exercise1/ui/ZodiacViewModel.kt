package com.example.viewpager2exercise1.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.viewpager2exercise1.data.model.Zodiac
import com.example.viewpager2exercise1.data.repository.ZodiacRepository

class ZodiacViewModel(
    private val repository: ZodiacRepository
) : ViewModel(){
    private  val _zodiacs = MutableLiveData<List<Zodiac>>()
    val zodiacs: LiveData<List<Zodiac>> = _zodiacs

    init{
        getZodiacs()
    }

    private fun getZodiacs() {
        _zodiacs.value = repository.getZodiacs()
    }

    fun getZodiacAtPosition(position: Int): Zodiac?{
        val zodiacs = _zodiacs.value
        return if(zodiacs != null && position < zodiacs.size){
            zodiacs[position]
        }else{
            null
        }
    }

    class Factory(private val repository: ZodiacRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ZodiacViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ZodiacViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}