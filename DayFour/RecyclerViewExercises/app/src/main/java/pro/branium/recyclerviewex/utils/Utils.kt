package pro.branium.recyclerviewex.utils

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    const val EXTRA_STUDENT_ID = "extra_student_id"
    const val EXTRA_FORM_MODE = "extra_form_mode"

    @SuppressLint("ConstantLocale")
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun dateToString(date: Date?): String{
        return if(date == null) "" else formatter.format(date)
    }

    fun stringToDate(str: String): Date{
        return try{
            formatter.parse(str)
        }catch (_: ParseException){
            Date()
        }
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        )
        return emailRegex.matches(email)
    }
}