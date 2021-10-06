package de.culture4life.luca.crypto

open class DailyKeyUnavailableException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}