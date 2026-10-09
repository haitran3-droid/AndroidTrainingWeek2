package com.example.fragmentexercise.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmentexercise.data.model.Student
import com.example.fragmentexercise.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class StudentViewModel(
    private val repository: StudentRepository
) : ViewModel() {
    private val _students = MutableLiveData<List<Student>>()
    private val original = mutableListOf<Student>()
    private var sortState = SortState(SortField.NONE, false)
    private val viCollator = Collator.getInstance(Locale.forLanguageTag("vi-VN")).apply {
        strength = Collator.SECONDARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }

    private var query = ""
    private val _isStudentDeleted = MutableLiveData<Boolean>()
    private val _isStudentRestored = MutableLiveData<Boolean>()
    private val _pendingDelete = MutableLiveData<PendingDelete?>()

    val students: LiveData<List<Student>> = _students
    val isStudentDeleted: LiveData<Boolean> = _isStudentDeleted
    val isStudentRestored: LiveData<Boolean> = _isStudentRestored
    val pendingDelete: LiveData<PendingDelete?> = _pendingDelete

    data class PendingDelete(val student: Student, val position: Int)

    init {
        loadStudents()
    }

    fun loadStudents() {
        _students.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val students = repository.getStudents()
            original.clear()
            original.addAll(students)
            postTransformed()
        }
    }

    fun searchStudents(query: String) {
        this.query = query
        applyTransform()
    }

    fun sortBy(field: SortField) {
        sortState = if (sortState.field == field) {
            sortState.copy(descending = !sortState.descending)
        } else {
            SortState(field, false)
        }
        applyTransform()
    }

    private fun applyTransform() {
        val sorted = doTransform()
        _students.value = sorted
    }

    private fun postTransformed() {
        val sorted = doTransform()
        _students.postValue(sorted)
    }

    /**
     * Thực hiện logic sắp xếp dữ liệu dựa trên giá trị của query và sortState.
     * Nếu query rỗng thì trả về toàn bộ dữ liệu, ngược lại trả về dữ liệu sau khi lọc.
     * Sau đó sắp xếp dữ liệu dựa trên sortState.field và sortState.descending.
     * Nếu sortState.descending là true thì sắp xếp theo thứ tự giảm dần.
     */
    private fun doTransform(): List<Student> {
        val filtered = if (query.isBlank()) original
        else original.filter {
            it.fullName.toString().contains(query, true)
        }
        val sorted = when (sortState.field) {
            SortField.NAME -> filtered.sortedWith { s1, s2 ->
                val byName = viCollator.compare(s1.fullName.firstName, s2.fullName.firstName)
                val cmp = if (byName != 0) byName
                else viCollator.compare(s1.fullName.lastName, s2.fullName.lastName)
                if (sortState.descending) -cmp else cmp
            }

            SortField.ID -> if (sortState.descending) filtered.sortedByDescending { it.id }
            else filtered.sortedBy { it.id }

            SortField.GPA -> if (sortState.descending) filtered.sortedByDescending { it.gpa }
            else filtered.sortedBy { it.gpa }

            else -> filtered
        }
        return sorted
    }

    fun deleteStudentById(studentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // lấy thông tin sinh viên cần xóa
            val currentList = _students.value ?: emptyList()
            val index = currentList.indexOfFirst { it.id == studentId }
            val student = if (index != -1) currentList[index] else null

            // lưu vào biến tạm _pendingDelete
            _pendingDelete.postValue(student?.let { PendingDelete(it, index) })

            // tiến hành xóa
            val deleteResult = repository.deleteStudent(studentId)

            // update danh sách sinh viên sau khi xóa
            original.removeIf { it.id == studentId }
            postTransformed()
            _isStudentDeleted.postValue(deleteResult)
        }
    }

    fun undoDelete() {
        val pending = _pendingDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val reuslt = repository.addStudent(pending.student)
            val pos = pending.position.coerceIn(0, original.size - 1)
            original.add(pos, pending.student)
            postTransformed()
            _isStudentRestored.postValue(reuslt)
        }
    }

    fun resetStatus() {
        _isStudentDeleted.value = false
        _isStudentRestored.value = false
        _pendingDelete.value = null
    }
}