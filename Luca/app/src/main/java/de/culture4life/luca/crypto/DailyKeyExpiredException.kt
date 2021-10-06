package de.culture4life.luca.crypto

class DailyKeyExpiredException : DailyKeyUnavailableException {
    constructor() : super()
    constructor(message: String) : super(message)
}