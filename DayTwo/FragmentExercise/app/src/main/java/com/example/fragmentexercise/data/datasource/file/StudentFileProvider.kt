package com.example.fragmentexercise.data.datasource.file

import java.io.InputStream
import java.io.OutputStream

interface StudentFileProvider {
    fun openFileToRead(fileName: String): InputStream
    fun openFileToWrite(fileName: String): OutputStream
}