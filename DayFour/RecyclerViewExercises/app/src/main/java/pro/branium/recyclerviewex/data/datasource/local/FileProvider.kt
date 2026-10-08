package pro.branium.recyclerviewex.data.datasource.local

import java.io.InputStream
import java.io.OutputStream


interface FileProvider {
    fun openFileToRead(fileName: String): InputStream
    fun openFileToWrite(fileName: String): OutputStream
}