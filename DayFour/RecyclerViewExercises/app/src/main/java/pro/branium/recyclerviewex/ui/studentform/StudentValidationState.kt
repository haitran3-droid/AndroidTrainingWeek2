package pro.branium.recyclerviewex.ui.studentform

data class StudentValidationState(
    val idError: Int? = null,
    val fullNameError: Int? = null,
    val birthDateError: Int? = null,
    val emailError: Int? = null,
    val gpaError: Int? = null
) {
    val isSuccess: Boolean
        get() = idError == null &&
                fullNameError == null &&
                birthDateError == null &&
                emailError == null &&
                gpaError == null
}