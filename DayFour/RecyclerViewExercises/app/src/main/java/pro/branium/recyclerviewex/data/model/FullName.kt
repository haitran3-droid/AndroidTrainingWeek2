package pro.branium.recyclerviewex.data.model

import com.google.gson.annotations.SerializedName

data class FullName(
    @SerializedName("first")
    val firstName: String,
    @SerializedName("last")
    val lastName: String,
    @SerializedName("midd")
    val middleName: String? = null
){
    override fun toString(): String {
        return if(middleName != null){
            "$lastName $middleName $firstName"
        }else{
            "$lastName $firstName"
        }
    }
}
