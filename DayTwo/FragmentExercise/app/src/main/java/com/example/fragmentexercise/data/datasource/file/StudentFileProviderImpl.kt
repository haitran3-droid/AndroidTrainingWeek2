package com.example.fragmentexercise.data.datasource.file

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class StudentFileProviderImpl(
    private val context: Context
): StudentFileProvider {
    override fun openFileToRead(fileName: String): InputStream {
        val file = getInternalFile(fileName)
        return if(file.exists()){
            file.inputStream()
        }else{
            context.assets.open(fileName).use{ inputStream ->
                file.outputStream().use{outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.inputStream()
        }
    }

    override fun openFileToWrite(fileName: String): OutputStream {
        return getInternalFile(fileName).outputStream()
    }

    private fun getInternalFile(fileName: String): File {
        return File(context.filesDir, fileName)
    }
}