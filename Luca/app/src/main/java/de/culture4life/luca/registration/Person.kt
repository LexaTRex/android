package de.culture4life.luca.registration

import com.google.gson.annotations.Expose
import java.util.*

open class Person(
    @Expose open val firstName: String,
    @Expose open val lastName: String,
) {

    fun getFullName() = "$firstName $lastName".trim()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Person || other.javaClass != this.javaClass) {
            return false
        }
        return getFullName() == other.getFullName()
    }

    override fun toString() = getFullName()

    companion object {
        fun compare(s1: String, s2: String): Boolean {
            var noTitlesS1 = removeAcademicTitles(s1)
            var noTitlesS2 = removeAcademicTitles(s2)
            return simplify(noTitlesS1).equals(simplify(noTitlesS2), ignoreCase = true)
        }

        fun removeAcademicTitles(name: String): String {
            return name.replace("(?i)Prof\\. ".toRegex(), "")
                .replace("(?i)Dr\\. ".toRegex(), "")
        }

        fun simplify(name: String): String {
            return name.uppercase(Locale.getDefault())
                .replace("[^\\x41-\\x5A]".toRegex(), "")
        }
    }

}

