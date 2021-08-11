package de.culture4life.luca.crypto

import com.nexenio.rxkeystore.RxKeyStore
import de.culture4life.luca.network.pojo.DailyKeyPair
import de.culture4life.luca.network.pojo.DailyKeyPairIssuer
import de.culture4life.luca.network.pojo.Issuer
import dgca.verifier.app.decoder.toBase64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey

object CryptoTestHelper {

    /**
     * will create and return a new valid SHA256withECDSA private and public key pair
     */
    fun createKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME).apply {
            initialize(256)
        }.generateKeyPair()
    }

    /**
     * will create a new DailyKeyPairIssuer object. It will contain valid newly created key pairs and
     * an Issuer and DailyKeyPair object. The data in DailyKeyPair will be signed and have a
     * valid signature
     */
    fun createDailyKeyPairAndIssuer(
        id: Int,
        createdAt: Long,
        issuerId: String,
        issuerName: String
    ): DailyKeyPairIssuer {
        val bouncyCastleKeyStore =
            RxKeyStore(RxKeyStore.TYPE_BKS, RxKeyStore.PROVIDER_BOUNCY_CASTLE)
        val signatureProvider = SignatureProvider(bouncyCastleKeyStore)

        // create SHA256withECDSA keypairs
        val hdDailyKeyPair = createKeyPair()
        val hdskpKeyPair = createKeyPair()
        val hdekpKeyPair = createKeyPair()

        val temp = DailyKeyPair(
            id,
            createdAt,
            issuerId,
            AsymmetricCipherProvider.encode(hdDailyKeyPair.public as ECPublicKey)
                .blockingGet().toBase64(),
            "NO_VALID_SIGNATURE".toByteArray().toBase64()
        )

        // sign current data in DailyKeyPair with hdskpKeyPair
        val signingData = CryptoManager.concatenate(
            temp.getEncodedKeyId(),
            temp.getEncodedCreatedAt(),
            CryptoManager.decodeFromString(temp.publicKey).blockingGet()
        ).blockingGet()

        val signature = signatureProvider.sign(signingData, hdskpKeyPair.private)
            .blockingGet()

        val dailyKeyPair = DailyKeyPair(
            temp.keyId,
            temp.createdAt,
            temp.issuerId,
            temp.publicKey,
            signature.toBase64()
        )

        // update issuer with generated keypairs
        val issuer = Issuer(
            issuerId = issuerId,
            name = issuerName,
            publicHDEKP = AsymmetricCipherProvider.encode(hdekpKeyPair.public as ECPublicKey)
                .blockingGet()
                .toBase64(),
            publicHDSKP = AsymmetricCipherProvider.encode(hdskpKeyPair.public as ECPublicKey)
                .blockingGet()
                .toBase64()
        )

        return DailyKeyPairIssuer(
            dailyKeyPair,
            issuer
        )
    }

}
