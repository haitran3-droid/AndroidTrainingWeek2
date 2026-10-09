package com.example.fragmentexercise.ui.home

import com.example.fragmentexercise.data.model.Student

interface OnStudentMenuClickListener {
    fun onEdit(student: Student)
    fun onDelete(student: Student)
    fun onViewDetail(student: Student)
}