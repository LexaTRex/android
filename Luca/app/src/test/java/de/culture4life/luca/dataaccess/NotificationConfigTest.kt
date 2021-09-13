package de.culture4life.luca.dataaccess

import androidx.test.runner.AndroidJUnit4
import com.google.gson.Gson
import com.google.gson.JsonObject
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class NotificationConfigTest {

    val response = """{
        "default": {
            "1": {
                "messages": {
                    "en": {
                        "message": "Default english level 1 message",
                        "shortMessage": "Default english level 1 short message",
                        "banner": "Default english level 1 banner",
                        "title": "Default english level 1 title"
                    },
                    "de": {
                        "message": "Default german level 1 message",
                        "shortMessage": "Default german level 1 short message",
                        "banner": "Default german level 1 banner",
                        "title": "Default german level 1 title"
                    }
                }
            },
            "2": {
                "messages": {
                    "en": {
                        "message": "Default english level 2 message with ((name)), Email: ((email)), Phone: ((phone))",
                        "shortMessage": "Default english level 2 short message",
                        "banner": "Default english level 2 banner",
                        "title": "Default english level 2 title"
                    },
                    "de": {
                        "message": "Default german level 2 message with ((name)), Email: ((email)), Phone: ((phone))",
                        "shortMessage": "Default german level 2 short message",
                        "banner": "Default german level 2 banner",
                        "title": "Default german level 2 title"
                    }
                }
            },
            "3": {
                "messages": {
                    "en": {
                        "message": "Default english level 3 message",
                        "shortMessage": "Default english level 3 short message",
                        "banner": "Default english level 3 banner",
                        "title": "Default english level 3 title"
                    },
                    "de": {
                        "message": "Default german level 3 message",
                        "shortMessage": "Default german level 3 short message",
                        "banner": "Default german level 3 banner",
                        "title": "Default german level 3 title"
                    }
                }
            },
            "4": {
                "messages": {
                    "en": {
                        "message": "Default english level 4 message",
                        "shortMessage": "Default english level 4 short message",
                        "banner": "Default english level 4 banner",
                        "title": "Default english level 4 title"
                    },
                    "de": {
                        "message": "Default german level 4 message",
                        "shortMessage": "Default german level 4 short message",
                        "banner": "Default german level 4 banner",
                        "title": "Default german level 4 title"
                    }
                }
            }
        },
        "departments": [
            {
                "uuid": "1cbad379-2417-446e-8be4-3bc21d1905f6",
                "name": "Special",
                "phone": null,
                "email": "special@health-departments.de",
                "config": {
                    "2": {
                        "messages": {
                            "de": {
                                "shortMessage": "Custom german level 2 short message"
                            }
                        }
                    }
                }
            },
            {
                "uuid": "73b99f6e-d905-4c20-b966-454ab9c939fd",
                "name": "Default",
                "phone": null,
                "email": "default@health-departments.de",
                "config": {}
            }
        ]
    }"""

    val responseJson: JsonObject = Gson().fromJson(response, JsonObject::class.java)
    var notificationConfig = NotificationConfig(responseJson)

    @Before
    fun setUp() {
        Locale.setDefault(Locale.GERMANY)
    }

    @Test
    fun getTexts_customHealthDepartment_customizedTexts() {
        notificationConfig.run {
            val expectedTests = NotificationTexts(
                "Default german level 2 title",
                "Default german level 2 banner",
                "Custom german level 2 short message",
                "Default german level 2 message with Special, Email: special@health-departments.de, Phone: ?"
            )
            val actualTexts = getTexts(2, "1cbad379-2417-446e-8be4-3bc21d1905f6")
            assertEquals(expectedTests, actualTexts)
        }
    }

    @Test
    fun getTexts_defaultHealthDepartment_defaultTexts() {
        notificationConfig.run {
            val expectedTests = NotificationTexts(
                "Default german level 2 title",
                "Default german level 2 banner",
                "Default german level 2 short message",
                "Default german level 2 message with Default, Email: default@health-departments.de, Phone: ?"
            )
            val actualTexts = getTexts(2, "73b99f6e-d905-4c20-b966-454ab9c939fd")
            assertEquals(expectedTests, actualTexts)
        }
    }

    @Test
    fun getHealthDepartments_fromDefaultConfig_returnsThem() {
        notificationConfig.run {
            assertEquals("1cbad379-2417-446e-8be4-3bc21d1905f6", getHealthDepartments()[0].id)
            assertEquals("Special", getHealthDepartments()[0].name)
            assertEquals("special@health-departments.de", getHealthDepartments()[0].mail)
            assertEquals(null, getHealthDepartments()[0].phoneNumber)
        }
    }

}