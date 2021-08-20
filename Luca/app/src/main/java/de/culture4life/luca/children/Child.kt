package de.culture4life.luca.children

import de.culture4life.luca.registration.Person

class Child(firstName: String, lastName: String) : Person(firstName, lastName) {
    companion object {
        /**
         * The maximum age that is still considered to be a child. Above that age, children need to
         * use their own luca app.
         */
        const val MAXIMUM_AGE_IN_YEARS = 14

        fun from(name: String, adultLastName: String): Child {
            var childName = name
            val trimmedName = name.trim()
            val trimmedLastName = adultLastName.trim()
            if (trimmedName.lowercase().endsWith(trimmedLastName.lowercase())) {
                childName = trimmedName.substring(0, trimmedName.length - trimmedLastName.length).trim()
            }
            return Child(childName, adultLastName)
        }
    }
}

class Children : ArrayList<Child>()