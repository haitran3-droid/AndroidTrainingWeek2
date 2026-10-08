package pro.branium.recyclerviewex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pro.branium.recyclerviewex.data.repository.StudentRepository
import pro.branium.recyclerviewex.ui.BaseViewModel

class StudentDetailViewModel(repository: StudentRepository) : BaseViewModel(repository) {
    class Factory(private val repository: StudentRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudentDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StudentDetailViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}