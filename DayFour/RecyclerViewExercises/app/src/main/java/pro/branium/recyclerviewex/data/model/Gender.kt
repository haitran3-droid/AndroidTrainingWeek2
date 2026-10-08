package pro.branium.recyclerviewex.data.model

enum class Gender(val value: String){
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    companion object{
        fun fromValue(value: String): Gender = entries.find{it.value == value} ?: OTHER
    }
}
