package com.example.fragmentexercise.ui.studentform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragmentexercise.R
import com.example.fragmentexercise.base.BaseViewModel
import com.example.fragmentexercise.common.DateUtils
import com.example.fragmentexercise.common.isValidEmail
import com.example.fragmentexercise.data.model.FullName
import com.example.fragmentexercise.data.model.Gender
import com.example.fragmentexercise.data.model.Student
import com.example.fragmentexercise.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentFormViewModel(
    repository: StudentRepository
) : BaseViewModel(repository) {

    private val _validationState = MutableLiveData<StudentValidationState>()
    val validationState: LiveData<StudentValidationState> = _validationState

    private val _isStudentAdded = MutableLiveData<Boolean>()
    val isStudentAdded: LiveData<Boolean> = _isStudentAdded

    private val _isStudentUpdated = MutableLiveData<Boolean>()
    val isStudentUpdated: LiveData<Boolean> = _isStudentUpdated

    /** Dùng cho UI validate “tức thời” – không ghi DB */
    fun validateNormalData(
        id: String,
        fullName: String,
        birthDate: String,
        email: String,
        gpa: String,
        formMode: FormMode
    ) {
        viewModelScope.launch {
            val state = validateInternal(id, fullName, birthDate, email, gpa, formMode)
            _validationState.postValue(state)
        }
    }

    /** Gọi khi nhấn “Thêm” */
    fun submitAdd(
        id: String,
        fullName: String,
        birthDate: String,
        email: String,
        gpa: String,
        address: String,
        major: String,
        year: String,
        gender: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = validateInternal(id, fullName, birthDate, email, gpa, FormMode.ADD)
            _validationState.postValue(state)
            if (!state.isSuccess) return@launch

            val student = buildStudent(
                id = id.trim(),
                fullNameString = fullName.trim(),
                birthDate = birthDate.trim(),
                email = email.trim(),
                gpa = gpa.trim().toFloat(),
                gender = gender,
                address = address.trim(),
                major = major.trim(),
                year = year.trim().toInt()
            )
            val ok = repository.addStudent(student)
            _isStudentAdded.postValue(ok)
        }
    }

    /** Gọi khi nhấn “Cập nhật” */
    fun submitUpdate(
        id: String,
        fullName: String,
        birthDate: String,
        email: String,
        gpa: String,
        address: String,
        major: String,
        year: String,
        gender: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = validateInternal(id, fullName, birthDate, email, gpa, FormMode.UPDATE)
            _validationState.postValue(state)
            if (!state.isSuccess) return@launch

            val student = buildStudent(
                id = id.trim(),
                fullNameString = fullName.trim(),
                birthDate = birthDate.trim(),
                email = email.trim(),
                gpa = gpa.trim().toFloat(),
                gender = gender,
                address = address.trim(),
                major = major.trim(),
                year = year.trim().toInt()
            )
            val ok = repository.updateStudent(student)
            _isStudentUpdated.postValue(ok)
        }
    }

    private suspend fun validateInternal(
        id: String,
        fullName: String,
        birthDate: String,
        email: String,
        gpa: String,
        formMode: FormMode
    ): StudentValidationState = withContext(Dispatchers.Default) {
        var state = StudentValidationState()

        val trimmedId = id.trim()
        val trimmedFullName = fullName.trim()
        val trimmedBirthDate = birthDate.trim()
        val trimmedEmail = email.trim()
        val trimmedGpa = gpa.trim()

        // ID
        if (trimmedId.isEmpty()) {
            state = state.copy(idError = R.string.error_id_empty)
        } else if (formMode == FormMode.ADD) {
            val existed = repository.isStudentExist(trimmedId)
            if (existed) state = state.copy(idError = R.string.error_student_exist)
        }
        // (Tuỳ yêu cầu UPDATE: nếu cho phép đổi ID, thêm check trùng ID mới ở đây)

        // Full name
        if (trimmedFullName.isEmpty()) {
            state = state.copy(fullNameError = R.string.error_full_name_empty)
        }

        // Birth date
        if (trimmedBirthDate.isEmpty()) {
            state = state.copy(birthDateError = R.string.error_birth_date_empty)
        } else {
            val correctFormat = trimmedBirthDate.matches("\\d{2}/\\d{2}/\\d{4}".toRegex())
            val errId = if (correctFormat) null else R.string.error_birth_date_format
            state = state.copy(birthDateError = errId)
        }

        // Email
        if (trimmedEmail.isEmpty()) {
            state = state.copy(emailError = R.string.error_email_empty)
        } else if (!trimmedEmail.isValidEmail()) {
            state = state.copy(emailError = R.string.error_invalid_email)
        }

        // GPA
        val gpaValue = trimmedGpa.toFloatOrNull()
        if (trimmedGpa.isEmpty()) {
            state = state.copy(gpaError = R.string.error_gpa_empty)
        } else if (gpaValue == null || gpaValue < 0f || gpaValue > 4.0f) {
            state = state.copy(gpaError = R.string.error_invalid_gpa)
        }

        state
    }

    private fun buildStudent(
        id: String,
        fullNameString: String,
        birthDate: String,
        email: String,
        gpa: Float,
        gender: String?,
        address: String,
        major: String,
        year: Int
    ): Student {
        val name = parseFullName(fullNameString)
        return Student(
            id = id,
            fullName = name,
            birthDate = DateUtils.stringToDate(birthDate),
            email = email,
            gpa = gpa,
            gender = Gender.fromValue(gender ?: "Khác"),
            address = address,
            major = major,
            year = year
        )
    }

    /** Tách Họ/Đệm/Tên theo quy tắc tiếng Việt thông dụng */
    private fun parseFullName(fullNameString: String): FullName {
        val parts = fullNameString.trim().split(Regex("\\s+"))
        return when {
            parts.size >= 3 -> {
                val lastName = parts.first()                  // Họ (ví dụ: Nguyễn)
                val firstName = parts.last()                 // Tên (ví dụ: An)
                val middle = parts.subList(1, parts.size - 1).joinToString(" ")
                FullName(firstName = firstName, lastName = lastName, middleName = middle)
            }

            parts.size == 2 -> {
                // Quy ước: parts[0] = Họ, parts[1] = Tên
                FullName(firstName = parts[1], lastName = parts[0], middleName = "")
            }

            else -> FullName(firstName = parts[0], lastName = "", middleName = "")
        }
    }
}