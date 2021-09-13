package de.culture4life.luca.network.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NotifyingHealthDepartment(

    @Expose
    @SerializedName("uuid")
    val id: String,

    @Expose
    @SerializedName("name")
    val name: String,

    @Expose
    @SerializedName("email")
    val mail: String?,

    @Expose
    @SerializedName("phone")
    val phoneNumber: String?

)