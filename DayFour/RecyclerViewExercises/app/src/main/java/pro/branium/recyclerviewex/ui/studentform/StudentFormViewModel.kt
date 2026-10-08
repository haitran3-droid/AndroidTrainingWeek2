package pro.branium.recyclerviewex.ui.studentform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.branium.recyclerviewex.R
import pro.branium.recyclerviewex.data.model.FullName
import pro.branium.recyclerviewex.data.model.Gender
import pro.branium.recyclerviewex.data.model.Student
import pro.branium.recyclerviewex.data.repository.StudentRepository
import pro.branium.recyclerviewex.ui.BaseViewModel
import pro.branium.recyclerviewex.utils.Utils

class StudentFormViewModel(repository: StudentRepository) : BaseViewModel(repository) {
    private val _validationState = MutableLiveData<StudentValidationState>()
    private val _isStudentUpdated = MutableLiveData<Boolean>()

    val validationState: LiveData<StudentValidationState> = _validationState
    val isStudentUpdated: MutableLiveData<Boolean> = _isStudentUpdated

    fun prepareToUpdate(
        id: String,
        fullNameString: String,
        birthDate: String,
        email: String,
        gpa: Float,
        gender: String? = null,
        address: String,
        major: String,
        year: Int,
        formMode: FormMode
    ) {
        val nameParts = fullNameString.split(" ")
        val fullName = if (nameParts.size >= 3) {
            val middleName = nameParts.subList(1, nameParts.size - 1).joinToString(" ")
            FullName(
                firstName = nameParts[nameParts.size - 1],
                lastName = nameParts[0],
                middleName = middleName
            )
        } else if (nameParts.size == 2) {
            FullName(firstName = nameParts[0], lastName = nameParts[1])
        } else {
            FullName(firstName = nameParts[0], lastName = "")
        }
        val student = Student(
            id = id,
            fullName = fullName,
            birthDate = Utils.stringToDate(birthDate),
            email = email,
            gpa = gpa,
            gender = Gender.fromValue(gender ?: "Khác"),
            address = address,
            major = major,
            year = year
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (formMode == FormMode.ADD) {
                val addResult = repository.addStudent(student)
                _isStudentUpdated.postValue(addResult)
            } else {
                val updateResult = repository.updateStudent(student)
                _isStudentUpdated.postValue(updateResult)
            }
        }
    }

    fun validateNormalData(
        id: String,
        fullName: String,
        birthDate: String,
        email: String,
        gpa: String,
        address: String,
        major: String,
        year: String,
        gender: String? = null,
        formMode: FormMode
    ) {
        viewModelScope.launch {
            var state = StudentValidationState()
            val trimmedId = id.trim()
            val trimmedFullName = fullName.trim()
            val trimmedBirthDate = birthDate.trim()
            val trimmedEmail = email.trim()
            val trimmedGpa = gpa.trim()

            if (trimmedId.isEmpty()) {
                state = state.copy(idError = R.string.error_id_empty)
            } else if (formMode == FormMode.ADD) {
                val isExisted = repository.isStudentExist(trimmedId)
                if (isExisted) {
                    state = state.copy(idError = R.string.error_student_exist)
                }
            }

            if (trimmedFullName.isEmpty()) {
                state = state.copy(fullNameError = R.string.error_full_name_empty)
            }

            if (trimmedBirthDate.isEmpty()) {
                state = state.copy(birthDateError = R.string.error_birth_date_empty)
            }

            if (trimmedEmail.isEmpty()) {
                state = state.copy(emailError = R.string.error_email_empty)
            } else if (!Utils.isValidEmail(trimmedEmail)) {
                state = state.copy(emailError = R.string.error_invalid_email)
            }

            if (trimmedGpa.isEmpty()) {
                state = state.copy(gpaError = R.string.error_gpa_empty)
            } else {
                val gpaValue = trimmedGpa.toFloatOrNull()
                if (gpaValue == null || gpaValue < 0 || gpaValue > 4.0) {
                    state = state.copy(gpaError = R.string.error_invalid_gpa)
                }
            }
            _validationState.postValue(state)
            if(state.isSuccess) {
                prepareToUpdate(
                    id,
                    fullName,
                    birthDate,
                    email,
                    gpa.toFloat(),
                    gender,
                    address,
                    major,
                    year.toInt(),
                    formMode
                )
            }
        }
    }

    // todo: tạo lớp factory cho lớp viewmodel này
    class Factory(private val repository: StudentRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudentFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StudentFormViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

