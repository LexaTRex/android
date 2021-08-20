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

}

