package de.culture4life.luca.registration

import com.google.gson.annotations.Expose

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
            var s1 = removeAcademicTitles(s1)
            var s2 = removeAcademicTitles(s2)
            return simplify(s1).equals(simplify(s2), ignoreCase = true)
        }

        fun removeAcademicTitles(name: String): String {
            var name = name
            name = name.replace("(?i)Prof\\. ".toRegex(), "")
            name = name.replace("(?i)Dr\\. ".toRegex(), "")
            return name
        }

        fun simplify(name: String): String {
            var name = name
            name = name.toUpperCase()
            name = name.replace("[^\\x41-\\x5A]".toRegex(), "")
            return name
        }
    }
}

