package pro.branium.recyclerviewex.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Student(
    @SerializedName("id")
    val id: String,
    @SerializedName("full_name")
    val fullName: FullName,
    @SerializedName("gender")
    val gender: Gender?,
    @SerializedName("birth_date")
    val birthDate: Date,
    @SerializedName("email")
    val email: String,
    @SerializedName("address")
    val address: String?,
    @SerializedName("major")
    val major: String?,
    @SerializedName("gpa")
    val gpa: Float?,
    @SerializedName("year")
    val year: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Student

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}