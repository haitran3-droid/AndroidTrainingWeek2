package com.example.fragmentexercise.common

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    const val EXTRA_STUDENT_ID = "extra_student_id"
    const val EXTRA_FORM_MODE = "extra_form_mode"

    @SuppressLint("ConstantLocale")
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun dateToString(date: Date?): String {
        return if (date == null) "" else formatter.format(date)
    }

    fun stringToDate(str: String): Date {
        return try {
            formatter.parse(str)
        } catch (_: ParseException) {
            Date()
        }
    }
}