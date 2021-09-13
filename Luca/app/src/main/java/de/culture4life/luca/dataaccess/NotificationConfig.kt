package de.culture4life.luca.dataaccess

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import de.culture4life.luca.network.pojo.NotifyingHealthDepartment
import java.util.*

private const val DEFAULT_LANGUAGE_CODE = "en"

class NotificationConfig(private val config: JsonObject) {

    private val gson = Gson()

    fun getTexts(level: Int, healthDepartmentId: String): NotificationTexts? {
        val defaultTexts = getDefaultTexts(level) ?: return null
        val department = getHealthDepartment(healthDepartmentId)
        val providedTexts = getProvidedTextsFromHealthDepartment(level, department)

        // The provided texts may only contain some properties,
        // unavailable properties are substituted by defaults.
        val mergedTexts = NotificationTexts(
            providedTexts?.title ?: defaultTexts.title,
            providedTexts?.banner ?: defaultTexts.banner,
            providedTexts?.shortMessage ?: defaultTexts.shortMessage,
            providedTexts?.message ?: defaultTexts.message
        )

        mergedTexts.message = replacePlaceHolder(
            mergedTexts.message,
            "((name))",
            department?.get("name")
        )
        mergedTexts.message = replacePlaceHolder(
            mergedTexts.message,
            "((email))",
            department?.get("email")
        )
        mergedTexts.message = replacePlaceHolder(
            mergedTexts.message,
            "((phone))",
            department?.get("phone")
        )

        return mergedTexts
    }

    fun getHealthDepartments(): List<NotifyingHealthDepartment> {
        return config["departments"].asJsonArray.map {
            gson.fromJson(it.toString(), NotifyingHealthDepartment::class.java)
        }
    }

    private fun getDefaultTexts(level: Int): NotificationTexts? {
        val default = config.getAsJsonObject("default")
        return getProvidedTextsFromHealthDepartment(level, default)
    }

    private fun getProvidedTextsFromHealthDepartment(level: Int, healthDepartment: JsonObject?): NotificationTexts? {
        if (healthDepartment == null) {
            return null
        }
        val config = healthDepartment.getAsJsonObject("config") ?: healthDepartment
        val levelKey = level.toString()
        if (!config.has(levelKey)) {
            return null
        }
        val messages = config.getAsJsonObject(levelKey).getAsJsonObject("messages")
        var languageCode = getLanguageCode()
        if (!messages.has(languageCode)) {
            if (!messages.has(DEFAULT_LANGUAGE_CODE)) {
                return null
            }
            languageCode = DEFAULT_LANGUAGE_CODE
        }
        val localizedMessages = messages.getAsJsonObject(languageCode)
        return NotificationTexts(localizedMessages)
    }

    private fun getHealthDepartment(healthDepartmentId: String): JsonObject? {
        return config.getAsJsonArray("departments")
            .firstOrNull { healthDepartmentId.equals(it.asJsonObject.get("uuid").asString, true) }
            ?.asJsonObject
    }

    private fun getLanguageCode(): String {
        return Locale.getDefault().language
    }

    private fun replacePlaceHolder(original: String?, placeholder: String, valueJson: JsonElement?): String? {
        if (original == null) {
            return null
        }
        val value = if (valueJson == null || valueJson is JsonNull || valueJson.asString.isEmpty()) {
            "?"
        } else {
            valueJson.asString
        }
        return original.replace(placeholder, value)
    }

}
