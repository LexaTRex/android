package de.culture4life.luca.network.pojo

import de.culture4life.luca.crypto.AsymmetricCipherProvider
import de.culture4life.luca.crypto.CryptoManager
import java.security.PublicKey

data class Issuer(
    val issuerId: String,
    val name: String,
    val publicHDEKP: String,
    val publicHDSKP: String
) {

    fun publicHDSKPToPublicKey(): PublicKey {
        return CryptoManager.decodeFromString(publicHDSKP)
            .flatMap { AsymmetricCipherProvider.decodePublicKey(it) }
            .blockingGet()
    }

}