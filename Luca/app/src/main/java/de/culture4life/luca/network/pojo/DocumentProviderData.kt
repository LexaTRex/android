package de.culture4life.luca.network.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DocumentProviderData(

    @Expose
    @SerializedName("name")
    val name: String,

    @Expose
    @SerializedName("publicKey")
    val publicKey: String,

    @Expose
    @SerializedName("fingerprint")
    val fingerprint: String

)
