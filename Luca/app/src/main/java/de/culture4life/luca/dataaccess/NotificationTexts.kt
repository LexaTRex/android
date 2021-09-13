package de.culture4life.luca.dataaccess

import com.google.gson.JsonObject

data class NotificationTexts(
    var title: String?,
    var banner: String?,
    var shortMessage: String?,
    var message: String?
) {

    constructor(json: JsonObject) : this(
        json.get("title")?.asString,
        json.get("banner")?.asString,
        json.get("shortMessage")?.asString,
        json.get("message")?.asString
    )

}
